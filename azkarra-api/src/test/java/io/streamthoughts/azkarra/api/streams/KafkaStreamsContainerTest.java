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
package io.streamthoughts.azkarra.api.streams;

import io.streamthoughts.azkarra.api.config.Conf;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class KafkaStreamsContainerTest {

    @Test
    public void testSourceTopicsLookup() {
        Conf streamsConfig = Conf.with(StreamsConfig.APPLICATION_ID_CONFIG, "test-app");
        KafkaStreamsContainer container = new KafkaStreamsContainer(null, streamsConfig, container1 -> null);

        StreamsBuilder builder = new StreamsBuilder();

        builder.stream("input-topic")
               .groupBy((k, v) -> v)
               .count()
               .toStream()
               .to("output-topic");

        Topology topology = builder.build();
        Set<String> userTopics = container.getSourceTopics(topology.describe());
        System.out.println(userTopics);
        Assertions.assertNotNull(userTopics);
        Assertions.assertEquals(1, userTopics.size());
        Assertions.assertTrue(userTopics.contains("input-topic"));
    }
}