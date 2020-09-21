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
package io.streamthoughts.examples.azkarra.topology;

import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.annotations.TopologyInfo;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.examples.azkarra.Version;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.Locale;

/**
 * A basic WordCount topology.
 */
@Component
@TopologyInfo( description = "A configurable WordCount topology example")
public class ConfigurableWordCountTopology implements TopologyProvider, Configurable {

    private static final String TOPOLOGY_TOPIC_SOURCE_CONFIG = "topology.topic.source";
    private static final String TOPOLOGY_TOPIC_SINK_CONFIG = "topology.topic.sink";
    private static final String TOPOLOGY_STORE_NAME_CONFIG = "topology.store.name";

    private String topicSource;
    private String topicSink;
    private String storeName;

    @Override
    public String version() {
        return Version.getVersion();
    }

    @Override
    public void configure(final Conf configuration) {
        topicSource = configuration.getString(TOPOLOGY_TOPIC_SOURCE_CONFIG);
        topicSink = configuration.getString(TOPOLOGY_TOPIC_SINK_CONFIG);
        storeName = configuration.getString(TOPOLOGY_STORE_NAME_CONFIG);
    }

    @Override
    public Topology topology() {

        final StreamsBuilder builder = new StreamsBuilder();

        final KStream<String, String> source = builder.stream(topicSource);

        final KTable<String, Long> counts = source
                .flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\s")))
                .groupBy((key, value) -> value)
                .count(Materialized.as(storeName));

        counts.toStream().to(topicSink, Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }
}