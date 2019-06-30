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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.gson.stream.JsonToken.NULL;

/**
 * Gson type adapter to convert {@link Map}s from and to a common CloudWatch event map structure.
 * <p>
 * CloudWatch events tend to represent map data structures as lists of name/value pairs:
 * </p>
 * <pre>
 *     [
 *         {
 *             "name": "...",
 *             "value": "...
 *         },
 *         {
 *             "name": "...",
 *             "value": "..."
 *         },
 *         ...
 *     ]
 * </pre>
 * <p>
 * Dealing with such a structure is more awkward than using a standard Java {@code Map}. This can be adapted to a
 * {@code Map} field with this class, using the {@link com.google.gson.annotations.JsonAdapter JsonAdapter} annotation:
 * </p>
 * <pre>
 *     &#64;JsonAdapter(NameValueListAdapter.class)
 *     private Map&lt;String, String&gt; stuff;</pre>
 */
public class NameValueListAdapter extends TypeAdapter<Map<String, String>> {
	/**
	 * Writes one JSON array for {@code value}.
	 * <p>
	 * See the general description for this class for details.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void write(JsonWriter out, Map<String, String> value) throws IOException {
		if (value == null) {
			out.nullValue();

			return;
		}

		out.beginArray();

		for (Entry<String, String> entry : value.entrySet()) {
			out.beginObject();
			out.name("name").value(entry.getKey());
			out.name("value").value(entry.getValue());
			out.endObject();
		}

		out.endArray();
	}

	/**
	 * Reads one JSON array and returns it as a {@code Map}.
	 * <p>
	 * See the general description for this class for details.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> read(JsonReader in) throws IOException {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();

		in.beginArray();

		while (in.hasNext()) {
			in.beginObject();

			String key = null;
			String value = null;

			boolean needKey = true;
			boolean needValue = true;

			while ((needKey || needValue) && in.hasNext()) {
				String jsonName = in.nextName();

				if (jsonName.equals("name")) {
					key = in.nextString();
					needKey = false;
				} else if (jsonName.equals("value")) {
					if (in.peek() == NULL) {
						in.skipValue();
					} else {
						value = in.nextString();
					}

					needValue = false;
				} else {
					in.skipValue();
				}
			}

			in.endObject();

			if (!needKey) {
				map.put(key, value);
			}
		}

		in.endArray();

		return map;
	}
}
