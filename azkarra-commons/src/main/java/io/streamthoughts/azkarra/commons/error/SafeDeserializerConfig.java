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

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.utils.Bytes;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public class SafeDeserializerConfig extends AbstractConfig {

    public static final String SAFE_DESERIALIZER_DEFAULT_VALUE_CONFIG = "safe.deserializer.default.object";
    public static final String SAFE_DESERIALIZER_DEFAULT_VALUE_DOC    = "The default object value to return when an " +
            "input record cannot be de-serialized (default is null)";

    private final Class<?> objectValueType;
    /**
     * Creates a new {@link SafeDeserializerConfig} instance.
     * @param originals the originals configuration.
     */
    public SafeDeserializerConfig(final Class<?> objectValueType, final Map<?, ?> originals) {
        super(configDef(typeFrom(objectValueType)), originals);
        this.objectValueType = objectValueType;
    }

    public Object defaultObject() {
        return mayConvert(get(SAFE_DESERIALIZER_DEFAULT_VALUE_CONFIG), objectValueType);
    }

    public static ConfigDef configDef(final ConfigDef.Type type) {

        return new ConfigDef()
            .define(SAFE_DESERIALIZER_DEFAULT_VALUE_CONFIG, type, null, ConfigDef.Importance.HIGH,
                    SAFE_DESERIALIZER_DEFAULT_VALUE_DOC);
    }

    static private <T> Object mayConvert(final Object o, final Class<T> type) {
        if (o == null) return null;

        Object result = o;
        if (byte[].class.isAssignableFrom(type)) {
            result = ((String)o).getBytes();
        } else if (ByteBuffer.class.isAssignableFrom(type)) {
            result = ByteBuffer.wrap(((String)o).getBytes());
        } else if (Bytes.class.isAssignableFrom(type)) {
            result = Bytes.wrap(((String)o).getBytes());
        } else if (UUID.class.isAssignableFrom(type)) {
            result = UUID.fromString((String)o);
        }
        return result;
    }

    static private <T> ConfigDef.Type typeFrom(final Class<T> type) {
        if (String.class.isAssignableFrom(type)) {
            return ConfigDef.Type.STRING;
        }

        if (Short.class.isAssignableFrom(type)) {
            return ConfigDef.Type.SHORT;
        }

        if (Integer.class.isAssignableFrom(type)) {
            return ConfigDef.Type.INT;
        }

        if (Long.class.isAssignableFrom(type)) {
            return ConfigDef.Type.LONG;
        }

        if (Float.class.isAssignableFrom(type)) {
            return ConfigDef.Type.DOUBLE;
        }

        if (Double.class.isAssignableFrom(type)) {
            return ConfigDef.Type.DOUBLE;
        }

        if (byte[].class.isAssignableFrom(type)) {
            return ConfigDef.Type.STRING;
        }

        if (ByteBuffer.class.isAssignableFrom(type)) {
            return ConfigDef.Type.STRING;
        }

        if (Bytes.class.isAssignableFrom(type)) {
            return ConfigDef.Type.STRING;
        }

        if (UUID.class.isAssignableFrom(type)) {
            return ConfigDef.Type.STRING;
        }

        throw new IllegalArgumentException("Unknown class for built-in serializer. Supported types are: " +
                "String, Short, Integer, Long, Float, Double, ByteArray, ByteBuffer, Bytes, UUID");
    }
}
