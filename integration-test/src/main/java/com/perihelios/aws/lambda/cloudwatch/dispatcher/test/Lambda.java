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

import com.amazonaws.services.lambda.runtime.Context;
import com.perihelios.aws.lambda.cloudwatch.dispatcher.CloudWatchEventDispatcher;

import java.io.InputStream;

public class Lambda {
	@SuppressWarnings("unused")
	public void handle(InputStream message, Context context) {
		new CloudWatchEventDispatcher(message, context)
				.logMessage()
				.withEventHandler(CustomEvent.class, (event, ctx) ->
						ctx.getLogger().log("Custom Event: " + event.value())
				)
				.dispatch();
	}
}
