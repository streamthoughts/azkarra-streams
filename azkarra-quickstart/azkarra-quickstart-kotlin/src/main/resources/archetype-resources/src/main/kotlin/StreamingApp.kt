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

#set ( $package = $package.replaceAll("-", ".") )
package ${package}

import io.streamthoughts.azkarra.api.annotations.Component
import io.streamthoughts.azkarra.api.config.Conf
import io.streamthoughts.azkarra.api.config.Configurable
import io.streamthoughts.azkarra.api.streams.TopologyProvider
import io.streamthoughts.azkarra.streams.AzkarraApplication
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.AzkarraStreamsApplication
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.Produced


/**
 * Skeleton for a Azkarra Streams Application
 *
 * <p>For a tutorial how to write a Azkarra Streams application, check the
 * tutorials and examples on the <a href="https://www.azkarrastreams.io/docs/">Azkarra Website</a>.
 * </p>
 */
@AzkarraStreamsApplication
object StreamingApp {

    @JvmStatic
    fun main(args: Array<String>) {
        AzkarraApplication.run(StreamingApp::class.java, args)
    }

    @Component
    class WordCountTopologyProvider: TopologyProvider, Configurable {

        private lateinit var topicSource: String
        private lateinit var topicSink: String
        private lateinit var stateStoreName: String

        override fun configure(conf: Conf) {
            topicSource = conf.getOptionalString("topic.source")
                    .orElse("streams-plaintext-input");
            topicSink = conf.getOptionalString("topic.sink")
                    .orElse("streams-wordcount-output");
            stateStoreName = conf.getOptionalString("state.store.name")
                    .orElse("count");
        }

        override fun version(): String {
            return Version.getVersion()
        }

        override fun topology(): Topology {
            val builder = StreamsBuilder()
            val source: KStream<String, String> = builder.stream(topicSource)

            val counts: KTable<String, Long> = source
                .flatMapValues { value -> value.toLowerCase().split("\\W+") }
                .groupBy { _, value -> value }
                .count(Materialized.`as`(stateStoreName))

            counts.toStream().to(topicSink, Produced.with(Serdes.String(), Serdes.Long()))

            return builder.build()
        }
    }
}