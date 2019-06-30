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
package com.perihelios.aws.lambda.cloudwatch.dispatcher.event;

import com.perihelios.aws.lambda.cloudwatch.dispatcher.CloudWatchEventDispatcher;

import java.util.function.BiConsumer;

/**
 * Base class of all CloudWatch events.
 * <p>
 * CloudWatch events are delivered to AWS Lambda functions as JSON documents. There are a small number of top-level
 * properties in the JSON; these are mostly metadata, and are unmarshalled into the {@link Header}, obtainable via
 * {@link #header()}. The event proper is contained in the {@code detail} property, with the {@code detail-type}
 * property providing a way to identify what properties may be expected in {@code detail}.
 * </p>
 * <p>
 * Subclasses should <em>always</em> be annotated with {@link DetailType}, as this is the only mechanism to indicate
 * to which subclass an event should be unmarshalled. Assume an event such as this is received by an AWS Lambda:
 * </p>
 * <pre>
 *     {
 *         ...
 *         "detail-type": "Some Service Event",
 *         "detail": {
 *             "blah": "some value",
 *             "foo": 17
 *         }
 *         ...
 *     }</pre>
 * <p>
 * A proper class to which to unmarshal this event might look like this:
 * </p>
 * <pre>
 *        &#64;DetailType("Some Service Event")
 *     	class MyCustomEvent extends CloudWatchEvent {
 *     	    private String blah;
 *     	    private int foo;
 *
 *     	    String blah() {
 *     	        return blah;
 *     	    }
 *
 *     	    int foo() {
 *     	        return foo;
 *     	    }
 *     	}
 * </pre>
 * <p>
 * Note that {@code MyCustomEvent} would also need to be registered with
 * {@link CloudWatchEventDispatcher#withEventHandler(Class, BiConsumer) CloudWatchEventDispatcher.withEventHandler()}
 * in order to be used during event dispatch.
 * </p>
 */
public class CloudWatchEvent {
	private Header header;

	/**
	 * Returns the header (metadata) from the event.
	 *
	 * @return the event header
	 */
	public Header header() {
		return header;
	}

	/**
	 * Sets the header (metadata) on the event.
	 *
	 * @param header the event header to set
	 */
	public void setHeader(Header header) {
		this.header = header;
	}
}
