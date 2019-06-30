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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NameValueListAdapterTest {
	@Test
	void unmarshals_from_array() throws IOException {
		JsonReader reader = new JsonReader(new StringReader(
				"[{\"name\":\"blah\",\"value\":\"something\"},{\"value\":1,\"name\":\"stuff\"}]"
		));

		Map<String, String> map = new NameValueListAdapter().read(reader);

		assertThat(map.get("blah")).isEqualTo("something");
		assertThat(map.get("stuff")).isEqualTo("1");
	}

	@Test
	void unmarshals_as_well_as_possible_with_junk_in_array() throws IOException {
		JsonReader reader = new JsonReader(new StringReader(
				"[{\"name\":\"blah\",\"value\":\"something\"},{\"stuff\":1},{\"name\":\"other\",\"value\":\"val\"}]"
		));

		Map<String, String> map = new NameValueListAdapter().read(reader);

		assertThat(map.get("blah")).isEqualTo("something");
		assertThat(map.get("other")).isEqualTo("val");
	}

	@Test
	void unmarshals_null_or_missing_values_from_array() throws IOException {
		JsonReader reader = new JsonReader(new StringReader(
				"[{\"name\":\"blah\",\"value\":null},{\"name\":\"other\"}]"
		));

		Map<String, String> map = new NameValueListAdapter().read(reader);

		assertThat(map).containsKeys("blah", "other");
		assertThat(map.get("blah")).isNull();
		assertThat(map.get("other")).isNull();
	}

	@Test
	void marshals_to_array() throws IOException {
		StringWriter stringWriter = new StringWriter(1024);
		JsonWriter writer = new JsonWriter(stringWriter);
		Map<String, String> map = new LinkedHashMap<>();
		map.put("blah", "val");
		map.put("stuff", "nonsense");

		new NameValueListAdapter().write(writer, map);

		assertThat(stringWriter.toString())
				.isEqualTo("[{\"name\":\"blah\",\"value\":\"val\"},{\"name\":\"stuff\",\"value\":\"nonsense\"}]");
	}

	@Test
	void marshals_null_values_to_array() throws IOException {
		StringWriter stringWriter = new StringWriter(1024);
		JsonWriter writer = new JsonWriter(stringWriter);
		Map<String, String> map = new LinkedHashMap<>();
		map.put("blah1", null);
		map.put("blah2", "val2");

		new NameValueListAdapter().write(writer, map);

		assertThat(stringWriter.toString())
				.isEqualTo("[{\"name\":\"blah1\",\"value\":null},{\"name\":\"blah2\",\"value\":\"val2\"}]");
	}

	@Test
	void marshals_null() throws IOException {
		StringWriter stringWriter = new StringWriter(1024);
		JsonWriter writer = new JsonWriter(stringWriter);

		new NameValueListAdapter().write(writer, null);

		assertThat(stringWriter.toString()).isEqualTo("null");
	}
}
