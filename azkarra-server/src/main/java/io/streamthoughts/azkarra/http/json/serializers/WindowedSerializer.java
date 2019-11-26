/*
 * Copyright 2019 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.azkarra.http.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.kafka.streams.kstream.Windowed;

import java.io.IOException;

/**
 * The {@link JsonSerializer} to serialize {@link org.apache.kafka.streams.kstream.Windowed} instance.
 */
public class WindowedSerializer extends JsonSerializer<Windowed> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final Windowed windowed,
                          final JsonGenerator gen,
                          final SerializerProvider serializers) throws IOException {

        gen.writeStartObject();
        gen.writeFieldName("key");
        gen.writeObject(windowed.key());
        gen.writeFieldName("windowStart");
        gen.writeNumber(windowed.window().start());
        gen.writeFieldName("windowEnd");
        gen.writeNumber(windowed.window().end());
        gen.writeEndObject();
    }
}
