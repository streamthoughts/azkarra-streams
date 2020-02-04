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
package io.streamthoughts.azkarra.commons.error;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.util.UUID;

/**
 * Factory for creating serializers / safe-deserializers.
 */
public class SafeSerdes {

    public static <T> Serde<T> serdeFrom(final Serde<T> serde, final T defaultValue) {
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), defaultValue)
        );
    }

    public static <T> Serde<T> serdeFrom(final Serializer<T> serializer,
                                         final Deserializer<T> deserializer,
                                         final T defaultValue) {
        return new Serdes.WrapperSerde<>(
            serializer,
            new SafeDeserializer<>(deserializer, defaultValue)
        );
    }

    public static <T> Serde<T> serdeFrom(final Class<T> type, final T defaultValue) {
        Serde<T> serde = Serdes.serdeFrom(type);
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), defaultValue)
        );
    }

    public static Serde<String> String() {
        final Serde<String> serde = Serdes.String();
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), String.class)
        );
    }

    public static Serde<String> String(final String defaultValue) {
        final Serde<String> serde = Serdes.String();
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), defaultValue)
        );
    }

    public static  Serde<Long> Long() {
        final Serde<Long> serde = Serdes.Long();
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), Long.class)
        );
    }

    public static  Serde<Long> Long(final Long defaultValue) {
        final Serde<Long> serde = Serdes.Long();
        return new Serdes.WrapperSerde<>(
                serde.serializer(),
                new SafeDeserializer<>(serde.deserializer(), defaultValue)
        );
    }

    public static  Serde<Double> Double() {
        final Serde<Double> serde = Serdes.Double();
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), Double.class)
        );
    }

    public static  Serde<Double> Double(final Double defaultValue) {
        final Serde<Double> serde = Serdes.Double();
        return new Serdes.WrapperSerde<>(
                serde.serializer(),
                new SafeDeserializer<>(serde.deserializer(), defaultValue)
        );
    }

    public static  Serde<Integer> Integer() {
        final Serde<Integer> serde = Serdes.Integer();
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), Integer.class)
        );
    }

    public static  Serde<Float> Float() {
        final Serde<Float> serde = Serdes.Float();
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), Float.class)
        );
    }

    public static  Serde<Float> Float(final Float defaultValue) {
        final Serde<Float> serde = Serdes.Float();
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), defaultValue)
        );
    }

    public static  Serde<UUID> UUID() {
        final Serde<UUID> serde = Serdes.UUID();
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), UUID.class)
        );
    }

    public static  Serde<UUID> UUID(final UUID uuid) {
        final Serde<UUID> serde = Serdes.UUID();
        return new Serdes.WrapperSerde<>(
            serde.serializer(),
            new SafeDeserializer<>(serde.deserializer(), uuid)
        );
    }
}
