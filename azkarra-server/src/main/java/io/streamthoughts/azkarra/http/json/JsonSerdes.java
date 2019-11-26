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
package io.streamthoughts.azkarra.http.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.http.error.SerializationException;
import io.streamthoughts.azkarra.http.json.serializers.ConfSerializer;
import io.streamthoughts.azkarra.http.json.serializers.GenericRecordSerializer;
import io.streamthoughts.azkarra.http.json.serializers.TaskMetadataSerializer;
import io.streamthoughts.azkarra.http.json.serializers.ThreadMetadataSerializer;
import io.streamthoughts.azkarra.http.json.serializers.TopicPartitionSerializer;
import io.streamthoughts.azkarra.http.json.serializers.TopologyDescriptorSerializer;
import io.streamthoughts.azkarra.http.json.serializers.WindowedSerializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.TaskMetadata;
import org.apache.kafka.streams.processor.ThreadMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Helper class for wrapping {@link ObjectMapper}.
 */
public class JsonSerdes {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Conf.class, new ConfSerializer())
              .addSerializer(TopicPartition.class, new TopicPartitionSerializer())
              .addSerializer(Windowed.class, new WindowedSerializer())
              .addSerializer(TopologyDescriptor.class, new TopologyDescriptorSerializer())
              .addSerializer(TaskMetadata.class, new TaskMetadataSerializer())
              .addSerializer(ThreadMetadata.class, new ThreadMetadataSerializer())
              .addSerializer(GenericRecord.class, new GenericRecordSerializer());
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.registerModule(module);
    }

    /**
     * Static helper that can be used to deserialize a given {@link InputStream}.
     *
     * @param is  the {@link InputStream} to read.
     * @return    the {@link JsonNode}.
     */
    public static JsonNode deserialize(final InputStream is) {
        try {
            return deserialize(is.readAllBytes());
        } catch (IOException e) {
            throw new SerializationException("Error while reading all bytes from input stream", e);
        }
    }

    /**
     * Static helper that can be used to deserialize a given {@link InputStream}.
     *
     * @param is   the {@link InputStream} to read.
     * @param type the expected type.
     *
     * @return    the deserialized object.
     */
    public static <T> T deserialize(final InputStream is, final Class<T> type) {
        try {
            return deserialize(is.readAllBytes(), type);
        } catch (IOException e) {
            throw new SerializationException("Error while reading all bytes from input stream", e);
        }
    }

    /**
     * Static helper that can be used to deserialize a given {@link InputStream}.
     *
     * @param data the json string.
     * @param type the expected type.
     *
     * @return    the deserialized object.
     */
    public static <T> T deserialize(final String data, final Class<T> type) {
        return deserialize(data.getBytes(StandardCharsets.UTF_8), type);
    }

    /**
     * Static helper that can be used to deserialize a given bytes array.
     *
     * @param data the data to deserialize.
     * @param type the expected type.
     *
     * @return    the deserialized object.
     */
    public static <T> T deserialize(final byte[] data, final Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(data, type);
        } catch (IOException e) {
            throw new SerializationException("Error happens while de-serializing '" + data + "'", e);
        }
    }

    /**
     * Static helper that can be used to deserialize a given bytes array.
     *
     * @param node the data to deserialize.
     * @param type the expected type.
     *
     * @return    the deserialized object.
     */
    public static <T> T deserialize(final JsonNode node, final Class<T> type) {
        try {
            return OBJECT_MAPPER.treeToValue(node, type);
        } catch (IOException e) {
            throw new SerializationException("Error happens while de-serializing '" + node + "'", e);
        }
    }


    /**
     * Static helper that can be used to deserialize a given bytes array.
     *
     * @param data the data to deserialize.
     *
     * @return    the {@link JsonNode}.
     */
    public static JsonNode deserialize(final byte[] data) {
        try {
            return OBJECT_MAPPER.readTree(data);
        } catch (IOException e) {
            throw new SerializationException("Error happens while de-serializing '" + data + "'", e);
        }
    }

    /**
     * Static helper that can be used to serialize a given object.
     *
     * @param data the object to serialize
     *
     * @return    the JSON string representation.
     */
    public static String serialize(final Object data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error happens while serializing object '" + data + "'", e);
        }
    }
}
