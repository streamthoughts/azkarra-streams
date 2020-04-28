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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.util.NameTransformer;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;

/**
 * The {@link JsonSerializer} to serialize {@link TopicPartition} instance.
 */
public class TopicPartitionSerializer extends JsonSerializer<TopicPartition> {

    private final JsonSerializer<TopicPartition> delegate
            = new UnwrappingTopicPartitionSerializer(NameTransformer.NOP);

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final TopicPartition value,
                          final JsonGenerator gen,
                          final SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        delegate.serialize(value, gen, serializers);
        gen.writeEndObject();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonSerializer<TopicPartition> unwrappingSerializer(final NameTransformer nameTransformer) {
        return new UnwrappingTopicPartitionSerializer(nameTransformer);
    }

    static class UnwrappingTopicPartitionSerializer extends JsonSerializer<TopicPartition> {

        private final NameTransformer nameTransformer;

        UnwrappingTopicPartitionSerializer(final NameTransformer nameTransformer) {
            this.nameTransformer = nameTransformer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(
                final TopicPartition value,
                final JsonGenerator gen,
                final SerializerProvider serializers
        ) throws IOException {
            gen.writeStringField(nameTransformer.transform("topic"), value.topic());
            gen.writeNumberField(nameTransformer.transform("partition"), value.partition());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isUnwrappingSerializer() {
            return true;
        }
    }
}
