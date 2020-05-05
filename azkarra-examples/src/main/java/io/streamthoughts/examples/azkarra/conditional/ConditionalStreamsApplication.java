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
package io.streamthoughts.examples.azkarra.conditional;

import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.annotations.ConditionalOn;
import io.streamthoughts.azkarra.api.annotations.Factory;
import io.streamthoughts.azkarra.api.components.BaseComponentModule;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.AzkarraStreamsApplication;
import io.streamthoughts.examples.azkarra.Version;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueMapper;

/**
 * This example demonstrates how conditional components can be used to dynamically
 * change the behavior of a stream topology based on its configuration.
 */
@AzkarraStreamsApplication
public class ConditionalStreamsApplication {

    public static void main(final String[] args) {
        AzkarraApplication.run(ConditionalStreamsApplication.class, args);
    }

    @Component
    @ConditionalOn(components = Normalize.class)
    public static class NormalizeStreamsTopology extends BaseComponentModule implements TopologyProvider {
        @Override
        public String version() {
            return Version.getVersion();
        }
        @Override
        public Topology get() {
            Normalize normalize = getComponent(Normalize.class);
            final StreamsBuilder builder = new StreamsBuilder();
            builder.stream("streams-plaintext-input", Consumed.with(Serdes.String(), Serdes.String()))
                    .mapValues(normalize, Named.as("map" + normalize.name()))
                    .to("streams-plaintext-upper", Produced.with(Serdes.String(), Serdes.String()));
            return builder.build();
        }
    }

    public interface Normalize extends ValueMapper<String, String> {

        String name();

        String normalize(final String s);

        @Override
        default String apply(String s) {
            return normalize(s);
        }
    }

    @Factory
    public static class Normalizes {
        @Component
        @ConditionalOn(property = "topology.lower.enable", havingValue = "true")
        public Normalize toLower() {
            return new Normalize() {
                @Override
                public String name() {
                    return "ToLower";
                }
                @Override
                public String normalize(final String s) {
                    return s.toLowerCase();
                }
            };
        }

        @Component
        @ConditionalOn(property = "topology.upper.enable", havingValue = "true")
        public Normalize toUpper() {
            return new Normalize() {
                @Override
                public String name() {
                    return "ToUpper";
                }
                @Override
                public String normalize(final String s) {
                    return s.toUpperCase();
                }
            };
        }
    }

}
