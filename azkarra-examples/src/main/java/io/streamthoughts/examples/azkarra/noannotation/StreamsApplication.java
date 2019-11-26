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
package io.streamthoughts.examples.azkarra.noannotation;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.ConfBuilder;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.config.HttpServerConf;
import io.streamthoughts.examples.azkarra.topology.BasicWordCountTopology;
import io.streamthoughts.examples.azkarra.topology.ConfigurableWordCountTopology;

import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;

/**
 * Example to manually register a topology.
 */
public class StreamsApplication {

    public static void main(final String[] args) {

        Conf streamsConf = ConfBuilder.newConf()
        .with(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        .with(DEFAULT_KEY_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde")
        .with(DEFAULT_VALUE_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde")
        .build();

        Conf config = Conf.with("streams", streamsConf);

        final AzkarraContext context = DefaultAzkarraContext.create(config);

        // register and add the topology to the default environment.
        context.addTopology(BasicWordCountTopology.class, Executed.as("BasicWordCount"));

        // register the topology to the context (the topology will not be started).
        context.addComponent(ConfigurableWordCountTopology.class);

        new AzkarraApplication()
            .setContext(context)
            .enableHttpServer(true, HttpServerConf.with("localhost", 8082))
            .run(args);
    }
}
