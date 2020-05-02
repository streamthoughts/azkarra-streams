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
package io.streamthoughts.azkarra.serialization;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;

import java.io.Closeable;

/**
 * Interface for serializing and de-serializing object.
 *
 * @param <T>   the data-type to serialize/deserialize.
 */
public interface Serdes<T> extends Configurable, Closeable, AutoCloseable {

    /**
     * Configure this {@link Serdes}.
     */
    @Override
    default void configure(final Conf configuration) {

    }

    /**
     * Get the content-type attached to this {@link Serdes}.
     *
     * @return  a string representing the content-type.
     */
    String contentType();

    /**
     * Serialize a data object into a byte array.
     *
     * @param object    the data object to convert; may be null.
     *
     * @return          the serialized data byte array; may be null
     */
    byte[] serialize(final T object) throws SerializationException;

    /**
     * Deserialize data from a byte array into a value or object.
     *
     * @param data      the data byte array to deserialize; may be null.
     * @return          the deserialized typed data; may be null
     */
    T deserialize(final byte[] data) throws SerializationException;


    /**
     * Close this {@link Serdes}.
     */
    @Override
    default void close() {

    }
}
