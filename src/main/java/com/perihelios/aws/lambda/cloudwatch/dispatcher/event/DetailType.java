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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates subtypes of {@link CloudWatchEvent}, identifying the text in the {@code detail-type} property of the
 * JSON-marshalled event that should indicate the use of the subtype.
 * <p>
 * See the general description of {@link CloudWatchEvent} for an example of using this annotation.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DetailType {
	/**
	 * Event detail type description, as matched against the {@code detail-type} property of the event.
	 *
	 * @return the detail type description
	 */
	String value();
}
