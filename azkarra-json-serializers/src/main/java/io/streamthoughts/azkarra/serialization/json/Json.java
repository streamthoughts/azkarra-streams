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

package io.streamthoughts.azkarra.serialization.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.streamthoughts.azkarra.serialization.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Helper class for wrapping {@link ObjectMapper}.
 */
public final class Json {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ObjectMapper objectMapper;

    public static Json getDefault() {
        return new Json(OBJECT_MAPPER);
    }

    /**
     * Creates a new {@link Json} instance.
     *
     * @param objectMapper  the {@link ObjectMapper}.
     */
    public Json(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public void configure(final Consumer<ObjectMapper> configure) {
        configure.accept(objectMapper);
    }

    public void registerModules(final Iterable<? extends Module> modules) {
        objectMapper.registerModules(modules);
    }

    public void registerModule(final Module module) {
        objectMapper.registerModule(module);
    }

    /**
     * An helper method that can be used to deserialize a given {@link InputStream}.
     *
     * @param is  the {@link InputStream} to read.
     * @return    the {@link JsonNode}.
     */
    public JsonNode deserialize(final InputStream is) {
        try {
            return deserialize(is.readAllBytes());
        } catch (IOException e) {
            throw new SerializationException("Error while reading all bytes from input stream", e);
        }
    }

    /**
     * An helper method that can be used to deserialize a given {@link InputStream}.
     *
     * @param is   the {@link InputStream} to read.
     * @param type the expected type.
     *
     * @return    the deserialized object.
     */
    public <T> T deserialize(final InputStream is, final Class<T> type) {
        try {
            return deserialize(is.readAllBytes(), type);
        } catch (IOException e) {
            throw new SerializationException("Error while reading all bytes from input stream", e);
        }
    }

    /**
     * An helper method that can be used to deserialize a given {@link InputStream}.
     *
     * @param data the json string.
     * @param type the expected type.
     *
     * @return    the deserialized object.
     */
    public <T> T deserialize(final String data, final Class<T> type) {
        return deserialize(data.getBytes(StandardCharsets.UTF_8), type);
    }

    /**
     * An helper method that can be used to deserialize a given bytes array.
     *
     * @param data the data to deserialize.
     * @param type the expected type.
     *
     * @return    the deserialized object.
     */
    public <T> T deserialize(final byte[] data, final Class<T> type) {
        try {
            return objectMapper.readValue(data, type);
        } catch (IOException e) {
            throw new SerializationException(e.getMessage(), e);
        }
    }

    /**
     * An helper method that can be used to deserialize a given bytes array.
     *
     * @param node the data to deserialize.
     * @param type the expected type.
     *
     * @return    the deserialized object.
     */
    public <T> T deserialize(final JsonNode node, final Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (IOException e) {
            throw new SerializationException("Error happens while de-serializing '" + node + "'", e);
        }
    }


    /**
     * An helper method that can be used to deserialize a given bytes array.
     *
     * @param data the data to deserialize.
     *
     * @return    the {@link JsonNode}.
     */
    public JsonNode deserialize(final byte[] data) {
        try {
            return objectMapper.readTree(data);
        } catch (IOException e) {
            throw new SerializationException("Error happens while de-serializing '" + data + "'", e);
        }
    }

    /**
     * An helper method that can be used to serialize a given object.
     *
     * @param data the object to serialize
     *
     * @return    the JSON string representation.
     */
    public String serialize(final Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error happens while serializing object '" + data + "'", e);
        }
    }

    public ObjectMapper unwrap() {
        return objectMapper;
    }
}