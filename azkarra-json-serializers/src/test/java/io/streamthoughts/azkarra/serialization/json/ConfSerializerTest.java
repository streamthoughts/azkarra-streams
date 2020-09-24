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
package io.streamthoughts.azkarra.serialization.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.streamthoughts.azkarra.api.config.Conf;
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

        var conf = Conf.of(SaslConfigs.SASL_JAAS_CONFIG, "dummy");
        assertWithConf(conf, "{\"sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenPasswordStreamsKeyPrefixedWithProducer() throws IOException {

        var conf = Conf.of(StreamsConfig.producerPrefix(SaslConfigs.SASL_JAAS_CONFIG), "dummy");
        assertWithConf(conf, "{\"producer.sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenPasswordStreamsKeyPrefixedWithConsumer() throws IOException {
        var conf = Conf.of(StreamsConfig.consumerPrefix(SaslConfigs.SASL_JAAS_CONFIG), "dummy");
        assertWithConf(conf, "{\"consumer.sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenPasswordStreamsKeyPrefixedWithAdmin() throws IOException {
        var conf = Conf.of(StreamsConfig.adminClientPrefix(SaslConfigs.SASL_JAAS_CONFIG), "dummy");
        assertWithConf(conf, "{\"admin.sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenPasswordStreamsKeyPrefixedWithStreams() throws IOException {
        var conf = Conf.of("streams." + SaslConfigs.SASL_JAAS_CONFIG, "dummy");
        assertWithConf(conf, "{\"streams.sasl.jaas.config\":\"[hidden]\"}");
    }

    @Test
    public void shouldObfuscateConfigGivenKeyEndingWithPassword() throws IOException {
        var conf = Conf.of("my.secret.password", "dummy");
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