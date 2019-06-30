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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ZonedDateTimeAdapterTest {
	@Test
	void unmarshals() throws IOException {
		JsonReader reader = new JsonReader(new StringReader("\"2019-06-20T12:34:56.123456789Z\""));
		ZonedDateTimeAdapter adapter = new ZonedDateTimeAdapter();

		ZonedDateTime zonedDateTime = adapter.read(reader);

		assertThat(zonedDateTime).isEqualTo(ZonedDateTime.parse("2019-06-20T12:34:56.123456789Z"));
	}

	@Test
	void marshals() throws IOException {
		StringWriter stringWriter = new StringWriter(32);
		JsonWriter writer = new JsonWriter(stringWriter);
		ZonedDateTimeAdapter adapter = new ZonedDateTimeAdapter();

		adapter.write(writer, ZonedDateTime.parse("2019-06-20T12:34:56.123456789Z"));

		assertThat(stringWriter.toString()).isEqualTo("\"2019-06-20T12:34:56.123456789Z\"");
	}
}
