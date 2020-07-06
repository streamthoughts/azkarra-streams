/*
 * Copyright 2020 StreamThoughts.
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
package io.streamthoughts.examples.azkarra.sse;

import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.events.EventStreamSupport;
import io.streamthoughts.azkarra.api.events.LimitHandlers;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.AzkarraStreamsApplication;
import io.streamthoughts.examples.azkarra.Version;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Arrays;
import java.util.Locale;

@AzkarraStreamsApplication
public class ServerSentEventExample {

    public static void main(String[] args) {
        AzkarraApplication.run(ServerSentEventExample.class, args);
    }

    @Component
    public static class WordCountPublisherTopology extends EventStreamSupport implements TopologyProvider {

        @Override
        public String version() {
            return Version.getVersion();
        }

        @Override
        public Topology get() {
            setDefaultEventQueueSize(10_000);
            setDefaultEventQueueLimitHandler(LimitHandlers.dropHeadOnLimitReached());
            addEventStreamsWithDefaults("word"); // register a default event-streams for type named 'word'

            final StreamsBuilder builder = new StreamsBuilder();

            final KStream<String, String> source = builder.stream("streams-plaintext-input");

            final KTable<String, Long> counts = source
                .flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\s")))

                .groupBy((key, value) -> value)
                .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("count").withCachingDisabled());

            // Use the EventStreamSupport#send method to publish all key-value records for type 'word'
            counts
                .toStream()
                .foreach( (k, v) -> send("word", k, v));

            return builder.build();
        }
    }
}
