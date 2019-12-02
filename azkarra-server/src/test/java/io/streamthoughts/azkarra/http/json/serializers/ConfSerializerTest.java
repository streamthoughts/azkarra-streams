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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.ConfBuilder;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class ConfSerializerTest {

    @Test
    public void shouldObfuscateConfigGivenPasswordStreamsKey() throws IOException {

        Conf conf = ConfBuilder.newConf()
                .with(SaslConfigs.SASL_JAAS_CONFIG, "dummy")
                .build();
        assertWithConf(conf, "{\"sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenPasswordStreamsKeyPrefixedWithProducer() throws IOException {

        Conf conf = ConfBuilder.newConf()
                .with(StreamsConfig.producerPrefix(SaslConfigs.SASL_JAAS_CONFIG), "dummy")
                .build();
        assertWithConf(conf, "{\"producer.sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenPasswordStreamsKeyPrefixedWithConsumer() throws IOException {

        Conf conf = ConfBuilder.newConf()
                .with(StreamsConfig.consumerPrefix(SaslConfigs.SASL_JAAS_CONFIG), "dummy")
                .build();
        assertWithConf(conf, "{\"consumer.sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenPasswordStreamsKeyPrefixedWithAdmin() throws IOException {

        Conf conf = ConfBuilder.newConf()
                .with(StreamsConfig.adminClientPrefix(SaslConfigs.SASL_JAAS_CONFIG), "dummy")
                .build();
        assertWithConf(conf, "{\"admin.sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenPasswordStreamsKeyPrefixedWithStreams() throws IOException {

        Conf conf = ConfBuilder.newConf()
                .with("streams." + SaslConfigs.SASL_JAAS_CONFIG, "dummy")
                .build();
        assertWithConf(conf, "{\"streams.sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenKeyEndingWithPassword() throws IOException {

        Conf conf = ConfBuilder.newConf()
                .with("my.secret.password", "dummy")
                .build();
        assertWithConf(conf, "{\"my.secret.password\":\"[hidden]\"}");
    }


    private void assertWithConf(final Conf conf, final String expected) throws IOException {
        Writer jsonWriter = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(jsonWriter);
        jsonGenerator = jsonGenerator.setCodec(new ObjectMapper());
        SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
        new ConfSerializer().serialize(conf, jsonGenerator, serializerProvider);
        jsonGenerator.flush();
        Assertions.assertEquals(expected, jsonWriter.toString());
    }

}