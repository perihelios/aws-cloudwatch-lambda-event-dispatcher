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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.perihelios.aws.lambda.cloudwatch.dispatcher.event.CloudWatchEvent;
import com.perihelios.aws.lambda.cloudwatch.dispatcher.event.DetailType;
import com.perihelios.aws.lambda.cloudwatch.dispatcher.event.Header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Main entry point of the API&mdash;all users of this library will create and configure an instance of this class.
 * <p>
 * A typical usage of this class in a Lambda function is:
 * </p>
 * <pre>
 *     ...
 *     void myLambdaFunc(InputStream message, Context context) {
 *         new CloudWatchEventDispatcher(message, context)
 *             .withEventHandler(MyEvent1.class, new MyEvent1Handler())
 *             .withEventHandler(MyEvent2.class, new MyEvent2Handler())
 *             .logMessage()
 *             .dispatch();
 *     }
 *     ...
 * </pre>
 */
public class CloudWatchEventDispatcher {
	private final String message;
	private final Context context;
	private final Map<String, BiConsumer<?, Context>> handlers;
	private final Map<String, Class<? extends CloudWatchEvent>> eventTypes;

	private boolean logRawMessage;

	/**
	 * Creates a dispatcher for a CloudWatch event, ready for further configuration.
	 *
	 * @param message raw message stream, presumed to contain CloudWatch event JSON
	 * @param context AWS Lambda context, to be passed to handlers as they are invoked
	 */
	public CloudWatchEventDispatcher(InputStream message, Context context) {
		this.message = readQuickly(message).trim();
		this.context = context;
		this.handlers = new HashMap<>();
		this.eventTypes = new HashMap<>();
	}

	/**
	 * Registers an event handler for a particular CloudWatch event type.
	 * <p>
	 * All classes passed in {@code eventType} must be annotated with {@link DetailType}. See the general description
	 * of that annotation, and of {@link CloudWatchEvent}, for details.
	 *
	 * @param eventType class to which events will be unmarshalled
	 * @param handler   consumer of events of {@code eventType} type
	 * @param <T>       type of event, with type bounds ensuring compatibility between {@code eventType} and
	 *                  {@code handler}
	 * @return a reference to this object
	 */
	public <T extends CloudWatchEvent> CloudWatchEventDispatcher withEventHandler(
			Class<T> eventType, BiConsumer<? super T, Context> handler) {

		DetailType detailType = eventType.getAnnotation(DetailType.class);

		if (detailType == null) {
			throw new IllegalArgumentException(
					"Cannot register event handler for event type " + eventType.getName() +
							"; event type not annotated with " + DetailType.class.getName()
			);
		}

		String typeDescription = detailType.value();

		eventTypes.put(typeDescription, eventType);
		handlers.put(typeDescription, handler);

		return this;
	}

	/**
	 * Instructs the dispatcher to log the incoming message, before it is parsed as JSON.
	 * <p>
	 * Activating logging via this method is primarily useful for troubleshooting errors where an AWS Lambda may be
	 * receiving messages <em>other than</em> CloudWatch events. CloudWatch will only send valid JSON documents,
	 * but it is possible to (erroneously) connect a Lambda function expecting CloudWatch events to another event
	 * source.
	 * <p>
	 * Logging is done via the {@link com.amazonaws.services.lambda.runtime.LambdaLogger LambdaLogger} obtained from
	 * {@link Context#getLogger()}. CloudWatch Logs must be enabled for the Lambda, and the Lambda's role must have
	 * the necessary permissions to write to CloudWatch Logs.
	 *
	 * @return a reference to this object
	 */
	public CloudWatchEventDispatcher logMessage() {
		logRawMessage = true;

		return this;
	}

	/**
	 * Dispatches the event to registered handlers.
	 * <p>
	 * This is the terminal operation of the dispatcher. All settings specified via the other methods of this class are
	 * applied at this time.
	 *
	 * @throws IllegalArgumentException if the message is not valid JSON, or if the message is missing
	 *                                  {@code detail-type} or {@code detail} properties, or if the message's
	 *                                  {@code detail-type} does not correspond to any event types registered via
	 *                                  {@link #withEventHandler(Class, BiConsumer) withEventHandler()}
	 */
	public void dispatch() {
		if (logRawMessage) {
			context.getLogger().log("Raw message: " + message);
		}

		JsonObject jsonObject;
		try {
			jsonObject = new JsonParser().parse(message).getAsJsonObject();
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse message as JSON", e);
		}

		Gson gson = new GsonBuilder()
				.registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
				.create();

		JsonElement detailType = jsonObject.get("detail-type");
		if (detailType == null) {
			throw new IllegalArgumentException(
					"Received message is not CloudWatch event (missing \"detail-type\" property)"
			);
		}

		JsonElement detail = jsonObject.get("detail");
		if (detail == null) {
			throw new IllegalArgumentException(
					"Received message is not CloudWatch event (missing \"detail\" property)"
			);
		}

		String typeDescription = detailType.getAsString();

		Class<? extends CloudWatchEvent> eventType = eventTypes.get(typeDescription);
		if (eventType == null) {
			throw new IllegalArgumentException(
					"Received event of unknown type; detail-type field in message: " + typeDescription
			);
		}

		Header header = gson.fromJson(jsonObject, Header.class);
		CloudWatchEvent event = gson.fromJson(detail, eventType);
		event.setHeader(header);

		// The generic type bounds used on the method that stores key/value pairs in the map make this type-safe
		@SuppressWarnings("unchecked")
		BiConsumer<Object, Context> handler =
				(BiConsumer<Object, Context>) handlers.get(typeDescription);

		handler.accept(event, context);
	}

	private static String readQuickly(InputStream stream) {
		try {
			byte[] bytes = new byte[stream.available()];

			//noinspection ResultOfMethodCallIgnored
			stream.read(bytes);
			int next = stream.read();

			if (next < 0) {
				return new String(bytes, UTF_8);
			}

			return readSlowly(bytes, next, stream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				stream.close();
			} catch (IOException ignore) {
			}
		}
	}

	private static String readSlowly(byte[] initial, int next, InputStream stream) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(initial.length + 1 + 65_536);
		byte[] buffer = new byte[65_536];

		outputStream.write(initial);
		outputStream.write(next);

		int read;
		while ((read = stream.read(buffer)) >= 0) {
			outputStream.write(buffer, 0, read);
		}

		return new String(outputStream.toByteArray(), UTF_8);
	}
}
