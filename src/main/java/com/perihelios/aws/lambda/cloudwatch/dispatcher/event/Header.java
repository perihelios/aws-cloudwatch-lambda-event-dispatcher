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

import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Encapsulates the metadata from a CloudWatch event.
 * <p>
 * Every CloudWatch event includes a set of common fields; this class encapsulates those.
 * </p>
 * <p>
 * Note that the {@code detail-type} property is not included in this class, as it is used exclusively by the
 * unmarshaller to identify to which subtype of {@link CloudWatchEvent} the event should be unmarshalled. The
 * {@code detail} property is unmarshalled as the body of the identified subtype, and so is also not included in the
 * header.
 * </p>
 */
@SuppressWarnings("unused")
public class Header {
	private String version;
	private String id;
	private String source;
	private String account;
	private ZonedDateTime time;
	private String region;
	private List<String> resources = emptyList();

	/**
	 * Returns the CloudWatch event version.
	 * <p>
	 * At this time, the version is always {@code "0"}.
	 * </p>
	 *
	 * @return the event version
	 */
	public String version() {
		return version;
	}

	/**
	 * Returns the unique CloudWatch event ID.
	 *
	 * @return the event ID
	 */
	public String id() {
		return id;
	}

	/**
	 * Returns the AWS service name that is the source of the event.
	 *
	 * @return the event source
	 */
	public String source() {
		return source;
	}

	/**
	 * Returns the AWS account ID for which the event was generated.
	 *
	 * @return the AWS account ID
	 */
	public String account() {
		return account;
	}

	/**
	 * Returns the timestamp when the event occurred.
	 * <p>
	 * The resolution of this field is typically only to the second. Specific events may provide higher-resolution
	 * timestamps as part of their bodies.
	 * </p>
	 *
	 * @return the event timestamp
	 */
	public ZonedDateTime time() {
		return time;
	}

	/**
	 * Returns the AWS region where the event occurred.
	 *
	 * @return the event region
	 */
	public String region() {
		return region;
	}

	/**
	 * Returns the AWS resources (typically, ARNs) affected by the event.
	 *
	 * @return the resources affected by the event
	 */
	public List<String> resources() {
		return resources;
	}
}
