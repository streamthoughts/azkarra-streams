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

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

public class SafeSerdesTest {

    private static final String TOPIC = "t";
    private static final byte[] CORRUPTED_RECORD = "bad".getBytes();

    @Test
    public void shouldSafelyDeserializeDoubleValueGivenDefaultObject() {
        final Serde<Double> serde = SafeSerdes.Double(0.0);
        final Deserializer<Double> deserializer = serde.deserializer();
        final Serializer<Double> serializer = serde.serializer();

        Assertions.assertEquals(0.0,  deserializer.deserialize(TOPIC, CORRUPTED_RECORD));
        Assertions.assertEquals(2.0,  deserializer.deserialize(TOPIC, serializer.serialize(TOPIC, 2.0)));
    }

    @Test
    public void shouldSafelyDeserializeLongValueGivenDefaultObject() {
        final Serde<Long> serde = SafeSerdes.Long(0L);
        final Deserializer<Long> deserializer = serde.deserializer();
        final Serializer<Long> serializer = serde.serializer();

        Assertions.assertEquals(0L,  deserializer.deserialize(TOPIC, CORRUPTED_RECORD));
        Assertions.assertEquals(2L,  deserializer.deserialize(TOPIC, serializer.serialize(TOPIC, 2L)));
    }

    @Test
    public void shouldSafelyDeserializeUUIDValueGivenDefaultObject() {
        final UUID defaultUUID = UUID.randomUUID();
        final Serde<UUID> serde = SafeSerdes.UUID(defaultUUID);
        final Deserializer<UUID> deserializer = serde.deserializer();
        final Serializer<UUID> serializer = serde.serializer();

        Assertions.assertEquals(defaultUUID,  deserializer.deserialize(TOPIC, CORRUPTED_RECORD));

        UUID uuid = UUID.randomUUID();
        Assertions.assertEquals(uuid,  deserializer.deserialize(TOPIC, serializer.serialize(TOPIC, uuid)));
    }

    @Test
    public void shouldSafelyDeserializeUUIDValueGivenConfiguredObject() {
        final UUID defaultUUID = UUID.randomUUID();
        final Serde<UUID> serde = SafeSerdes.UUID();

        serde.configure(new HashMap<>(){{
            put(SafeDeserializerConfig.SAFE_DESERIALIZER_DEFAULT_VALUE_CONFIG, defaultUUID.toString());
        }}, false);

        final Deserializer<UUID> deserializer = serde.deserializer();
        final Serializer<UUID> serializer = serde.serializer();

        Assertions.assertEquals(defaultUUID,  deserializer.deserialize(TOPIC, CORRUPTED_RECORD));

        UUID uuid = UUID.randomUUID();
        Assertions.assertEquals(uuid,  deserializer.deserialize(TOPIC, serializer.serialize(TOPIC, uuid)));
    }
}