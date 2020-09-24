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
package io.streamthoughts.azkarra.commons.error;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class SafeDeserializer<T> implements Deserializer<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SafeDeserializer.class);

    private Deserializer<T> deserializer;

    private final Class<T> type;

    private T defaultObject;

    /**
     * Creates a new {@link SafeDeserializer} instance.
     *
     * @param deserializer  the {@link Deserializer} to delegate.
     */
    public SafeDeserializer(final Deserializer<T> deserializer,
                            final T defaultValue) {
        this(deserializer, defaultValue, null);
    }

    /**
     * Creates a new {@link SafeDeserializer} instance.
     *
     * @param deserializer  the {@link Deserializer} to delegate.
     */
    public SafeDeserializer(final Deserializer<T> deserializer,
                            final Class<T> type) {
        this(deserializer, null, type);
    }

    /**
     * Creates a new {@link SafeDeserializer} instance.
     *
     * @param deserializer  the {@link Deserializer} to delegate.
     */
    private SafeDeserializer(final Deserializer<T> deserializer,
                             final T defaultValue,
                             final Class<T> type) {
        this.deserializer = Objects.requireNonNull(deserializer, "deserializer cannot be null");
        this.defaultObject = defaultValue;
        this.type = type;
    }

    /**
     * {@inheritDoc
     */
    @Override
    @SuppressWarnings("unchecked")
    public void configure(final Map<String, ?> configs, final boolean isKey) {
        this.deserializer.configure(configs, isKey);
        if (type != null) {
            this.defaultObject = (T) new SafeDeserializerConfig(type, configs).defaultObject();
        }
    }

    /**
     * {@inheritDoc
     */
    @Override
    public T deserialize(final String topic, final byte[] data) {
        try {
            return this.deserializer.deserialize(topic, data);
        } catch (Throwable t) {
            LOG.error(
                "Unexpected exception occurred during deserialization: {}. Returned default object.",
                t.getMessage()
            );
            return defaultObject;
        }
    }

    /**
     * {@inheritDoc
     */
    @Override
    public T deserialize(final String topic, final Headers headers, final byte[] data) {
        try {
            return this.deserializer.deserialize(topic, headers, data);
        } catch (Throwable t) {
            LOG.error(
                "Unexpected exception occurred during deserialization: {}. Returned default object.",
                t.getMessage()
            );
            return defaultObject;
        }
    }

    /**
     * {@inheritDoc
     */
    @Override
    public void close() {
        this.deserializer.close();
    }
}
