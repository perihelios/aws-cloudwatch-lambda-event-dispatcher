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
package com.perihelios.aws.lambda.cloudwatch.dispatcher;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.perihelios.aws.lambda.cloudwatch.dispatcher.event.DetailType;
import com.perihelios.aws.lambda.cloudwatch.dispatcher.event.Header;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.fill;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudWatchEventDispatcherTest {
	@Test
	void throws_on_unhandled_event_type() {
		assertThatThrownBy(() -> new CloudWatchEventDispatcher(classpathFile("unknown-event.json"), null).dispatch())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Received event of unknown type; detail-type field in message: Unknown Event");
	}

	@Test
	void throws_when_handler_event_type_missing_detail_type_annotation() {
		assertThatThrownBy(
				() -> new CloudWatchEventDispatcher(new ByteArrayInputStream(new byte[0]), null)
						.withEventHandler(UnannotatedEvent.class, (a, b) -> {}))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Cannot register event handler for event type " + UnannotatedEvent.class.getName() +
						"; event type not annotated with " + DetailType.class.getName()
				);
	}

	@Test
	void throws_when_message_not_json() {
		assertThatThrownBy(
				() -> new CloudWatchEventDispatcher(new ByteArrayInputStream("xyz".getBytes(UTF_8)), null)
						.withEventHandler(FictitiousEvent.class, (a, b) -> {})
						.dispatch()
		)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Failed to parse message as JSON")
				.hasCauseInstanceOf(RuntimeException.class);
	}

	@Test
	void throws_when_message_missing_detail_type_property() {
		assertThatThrownBy(
				() -> new CloudWatchEventDispatcher(classpathFile("missing-detail-type.json"), null)
						.withEventHandler(FictitiousEvent.class, (a, b) -> {})
						.dispatch()
		)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Received message is not CloudWatch event (missing \"detail-type\" property)");
	}

	@Test
	void throws_when_message_missing_detail_property() {
		assertThatThrownBy(
				() -> new CloudWatchEventDispatcher(classpathFile("missing-detail.json"), null)
						.withEventHandler(FictitiousEvent.class, (a, b) -> {})
						.dispatch()
		)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Received message is not CloudWatch event (missing \"detail\" property)");
	}

	@Test
	void dispatches_event() {
		Context lambdaContext = mock(Context.class);

		new CloudWatchEventDispatcher(classpathFile("fictitious-event.json"), lambdaContext)
				.withEventHandler(FictitiousEvent.class, (event, context) -> {
					assertThat(context).isSameAs(lambdaContext);

					Header header = event.header();

					assertThat(header.version()).isEqualTo("0");
					assertThat(header.id()).isEqualTo("85085726-4d64-918b-c9bb-62b172316c7c");
					assertThat(header.source()).isEqualTo("aws.fiction");
					assertThat(header.account()).isEqualTo("261421242815");
					assertThat(header.time()).isEqualTo(ZonedDateTime.parse("2019-06-16T22:20:01Z"));
					assertThat(header.region()).isEqualTo("us-west-7");
					assertThat(header.resources()).containsExactly("arn:aws:fiction:us-west-7:261421242815:item/path");

					assertThat(event.biscuit()).isEqualTo("flaky");
				})
				.dispatch();
	}

	@Test
	void logs_message() {
		ByteArrayInputStream stream = classpathFile("single-line-event.json.txt");
		String json = new String(stream.readAllBytes(), UTF_8).trim();
		stream.reset();

		LambdaLogger logger = mock(LambdaLogger.class);
		Context lambdaContext = mock(Context.class);
		when(lambdaContext.getLogger()).thenReturn(logger);

		new CloudWatchEventDispatcher(stream, lambdaContext)
				.withEventHandler(FictitiousEvent.class, (event, context) -> {})
				.logMessage()
				.dispatch();

		verify(logger).log("Raw message: " + json);
	}

	@Test
	void reads_long_message() {
		char[] spaces = new char[100_000];
		fill(spaces, ' ');

		StringBuilder message =
				new StringBuilder(new String(classpathFile("fictitious-event.json").readAllBytes(), UTF_8));
		message.insert(1, spaces);

		ByteArrayInputStream stream = new ByteArrayInputStream(message.toString().getBytes(UTF_8)) {
			@Override
			public synchronized int available() {
				return 10;
			}
		};

		new CloudWatchEventDispatcher(stream, null)
				.withEventHandler(FictitiousEvent.class, (event, context) ->
						assertThat(event.biscuit()).isEqualTo("flaky")
				)
				.dispatch();
	}

	private static ByteArrayInputStream classpathFile(String filename) {
		if (!filename.startsWith("/")) {
			filename = "/" + filename;
		}

		try (InputStream stream = CloudWatchEventDispatcher.class.getResourceAsStream(filename)) {
			if (stream == null) {
				throw new FileNotFoundException("File not found on classpath: " + filename);
			}

			return new ByteArrayInputStream(stream.readAllBytes());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
