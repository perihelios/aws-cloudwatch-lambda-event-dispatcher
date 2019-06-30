/*
 * Copyright 2019 Perihelios LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.perihelios.aws.lambda.cloudwatch.dispatcher.test;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvalidParameterValueException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static software.amazon.awssdk.core.SdkBytes.fromByteArray;
import static software.amazon.awssdk.regions.Region.AWS_GLOBAL;
import static software.amazon.awssdk.services.lambda.model.Runtime.JAVA8;

class IntegrationTest {
	private String lambdaPolicyArn;
	private LambdaClient lambda;
	private CloudWatchEventsClient cloudWatchEvents;
	private IamClient iam;
	private CloudWatchLogsClient cloudWatchLogs;

	private String name;
	private String logGroupName;

	private String lambdaRoleArn;
	private String lambdaArn;

	@BeforeEach
	void setUp() {
		iam = IamClient.builder().region(AWS_GLOBAL).build();
		lambda = LambdaClient.create();
		cloudWatchEvents = CloudWatchEventsClient.create();
		cloudWatchLogs = CloudWatchLogsClient.create();

		name = String.format("cloudwatch-event-dispatcher-test-%08x", new Random().nextInt());
		logGroupName = "/aws/lambda/" + name;
		lambdaPolicyArn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole";

		createLambdaRole();

		await()
				.atMost(20, SECONDS)
				.pollDelay(5, SECONDS)
				.pollInterval(1, SECONDS)
				.ignoreException(InvalidParameterValueException.class)
				.untilAsserted(this::createLambda);

		createCloudWatchRule();
	}

	@AfterEach
	void tearDown() {
		iam.detachRolePolicy(with -> with
				.roleName(name)
				.policyArn(lambdaPolicyArn)
		);
		iam.deleteRole(with -> with.roleName(name));
		lambda.deleteFunction(with -> with.functionName(name));
		cloudWatchEvents.removeTargets(with -> with.rule(name).ids(name));
		cloudWatchEvents.deleteRule(with -> with.force(true).name(name));
		cloudWatchLogs.deleteLogGroup(with -> with.logGroupName(logGroupName));
	}

	@Test
	void lambdaDispatchesEvent() {
		cloudWatchEvents.putEvents(with -> with.entries(
				PutEventsRequestEntry.builder()
						.source(name)
						.detailType("Custom Event")
						.detail(new Gson().toJson(Map.of("value", "test succeeded")))
						.build()
		));

		await()
				.atMost(20, SECONDS)
				.pollDelay(5, SECONDS)
				.pollInterval(1, SECONDS)
				.untilAsserted(() ->
						assertThat(cloudWatchLogs()).contains(
								"Custom Event: test succeeded"
						)
				);
	}

	@Test
	void lambdaLogsMessage() {
		await()
				.atMost(10, SECONDS)
				.pollInterval(1, SECONDS)
				.untilAsserted(() ->
						lambda.invoke(with -> with.functionName(name).payload(SdkBytes.fromString("\"abc\"", UTF_8)))
				);

		await()
				.pollDelay(3, SECONDS)
				.pollInterval(1, SECONDS)
				.untilAsserted(() -> assertThat(cloudWatchLogs()).contains("Raw message: \"abc\""));
	}

	private void createLambdaRole() {
		lambdaRoleArn = iam.createRole(builder -> builder.roleName(name).assumeRolePolicyDocument(
				new Gson().toJson(Map.of(
						"Version", "2012-10-17",
						"Statement", List.of(
								Map.of(
										"Sid", "",
										"Effect", "Allow",
										"Principal", Map.of("Service", "lambda.amazonaws.com"
										),
										"Action", "sts:AssumeRole"
								)
						)
				))
		)).role().arn();

		iam.attachRolePolicy(with -> with
				.roleName(name)
				.policyArn(lambdaPolicyArn)
		);
	}

	private void createLambda() {
		try (InputStream stream = getClass().getResourceAsStream("/lambda.jar")) {
			byte[] lambdaJarBytes = stream.readAllBytes();

			lambdaArn = lambda.createFunction(builder -> builder
					.functionName(name)
					.code(codeBuilder -> codeBuilder.zipFile(fromByteArray(lambdaJarBytes)))
					.runtime(JAVA8)
					.role(lambdaRoleArn)
					.handler(Lambda.class.getName() + "::handle")
			).functionArn();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void createCloudWatchRule() {
		String ruleArn = cloudWatchEvents.putRule(with -> with
				.eventPattern(
						new Gson().toJson(Map.of("source", List.of(name)))
				)
				.name(name)
		).ruleArn();

		cloudWatchEvents.putTargets(with -> with
				.rule(name)
				.targets(
						Target.builder().id(name).arn(lambdaArn).build()
				)
		);

		lambda.addPermission(with -> with.
				functionName(name)
				.action("lambda:InvokeFunction")
				.principal("events.amazonaws.com")
				.sourceArn(ruleArn)
				.statementId("1")
		);
	}

	private List<String> cloudWatchLogs() {
		String logStreamName = cloudWatchLogs.describeLogStreams(with -> with
				.logGroupName(logGroupName)
		).logStreams().get(0).logStreamName();

		return cloudWatchLogs.getLogEvents(with -> with
				.logGroupName(logGroupName)
				.logStreamName(logStreamName)
				.startFromHead(true)
		).events().stream()
				.map(OutputLogEvent::message)
				.collect(Collectors.toList());
	}
}
