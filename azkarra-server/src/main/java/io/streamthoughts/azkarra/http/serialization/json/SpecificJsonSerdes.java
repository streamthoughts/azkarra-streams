/*
 * Copyright 2019-2020 StreamThoughts.
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
package io.streamthoughts.azkarra.http.serialization.json;

import io.streamthoughts.azkarra.serialization.Serdes;
import io.streamthoughts.azkarra.serialization.SerializationException;
import io.streamthoughts.azkarra.serialization.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class SpecificJsonSerdes<T> implements Serdes<T> {

    private final Json json;

    private final Class<T> type;

    /**
     * Creates a new {@link SpecificJsonSerdes} instance.
     *
     * @param json  the {@link Json} object.
     */
    public SpecificJsonSerdes(final Json json,
                              final Class<T> type) {
        this.json = Objects.requireNonNull(json, "the Json object cannot be null");
        this.type = Objects.requireNonNull(type, "the type cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String contentType() {
        return "application/json; charset=utf-8";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] serialize(final Object object) throws SerializationException {
        return json.serialize(object).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T deserialize(final byte[] data) throws SerializationException {
        return json.deserialize(data, type);
    }
}

