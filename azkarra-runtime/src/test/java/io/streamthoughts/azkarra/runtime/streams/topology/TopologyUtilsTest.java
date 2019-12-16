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
package io.streamthoughts.azkarra.runtime.streams.topology;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static io.streamthoughts.azkarra.runtime.streams.topology.TopologyUtils.getSinkTopics;
import static io.streamthoughts.azkarra.runtime.streams.topology.TopologyUtils.getSourceTopics;
import static io.streamthoughts.azkarra.runtime.streams.topology.TopologyUtils.isInternalTopic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TopologyUtilsTest {

    private static final String REPARTITION_TOPIC = "KSTREAM-AGGREGATE-STATE-STORE-0000000002-repartition";

    private static Topology topology;

    @BeforeAll
    public static void setUp() {
        StreamsBuilder builder = new StreamsBuilder();

        builder.stream("input-topic")
                .groupBy((k, v) -> v)
                .count()
                .toStream()
                .to("output-topic");

        topology = builder.build();
    }

    @Test
    public void shouldReturnOnlySourceTopics() {
        Set<String> userTopics = getSourceTopics(topology.describe());
        assertEquals(2, userTopics.size());
        assertTrue(Arrays.asList("input-topic", REPARTITION_TOPIC).containsAll(userTopics));
    }

    @Test
    public void shouldReturnOnlySinkTopics() {
        Set<String> userTopics = getSinkTopics(topology.describe());
        assertEquals(2, userTopics.size());
        assertTrue(Arrays.asList("output-topic", REPARTITION_TOPIC).containsAll(userTopics));
    }


    @Test
    public void shouldReturnTrueGivenInternalTopicNotPrefixed() {
        assertTrue(isInternalTopic(REPARTITION_TOPIC));
    }

    @Test
    public void shouldReturnTrueGivenInternalTopicPrefixedWithAppId() {
        assertTrue(TopologyUtils.isInternalTopic("test", "test-" + REPARTITION_TOPIC));
    }

    @Test
    public void shouldReturnTrueGivenNamedRepartitionTopic() {
        assertTrue(TopologyUtils.isInternalTopic("test", "test-count-repartition"));
    }

    @Test
    public void shouldReturnTrueGivenNamedChangelogTopic() {
        assertTrue(TopologyUtils.isInternalTopic("test", "test-count-changelog"));
    }

}