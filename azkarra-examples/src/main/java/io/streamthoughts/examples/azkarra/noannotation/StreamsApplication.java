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
package io.streamthoughts.examples.azkarra.noannotation;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.banner.Banner;
import io.streamthoughts.azkarra.http.ServerConfig;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.banner.AzkarraBanner;
import io.streamthoughts.azkarra.streams.banner.BannerPrinterBuilder;
import io.streamthoughts.azkarra.streams.config.AzkarraConf;
import io.streamthoughts.examples.azkarra.topology.BasicWordCountTopology;
import io.streamthoughts.examples.azkarra.topology.ConfigurableWordCountTopology;

/**
 * Example to manually register a topology.
 */
public class StreamsApplication {

    public static void main(final String[] args) {

        BannerPrinterBuilder.newBuilder()
                .setMode(Banner.Mode.CONSOLE)
                .build()
                .print(new AzkarraBanner());

        final AzkarraContext context = DefaultAzkarraContext.create();

        // register and add the topology to the default environment.
        context.addTopology(BasicWordCountTopology.class, Executed.as("BasicWordCount"));

        // register the topology to the context (the topology will not be started).
        context.registerComponent(ConfigurableWordCountTopology.class);

        final ServerConfig serverConfig = ServerConfig
                .newBuilder()
                .setListener("localhost")
                .setPort(8082)
                .build();

        new AzkarraApplication()
            .setConfiguration(AzkarraConf.create())
            .setBannerMode(Banner.Mode.OFF)
            .setContext(context)
            .setHttpServerEnable(true)
            .setHttpServerConf(serverConfig)
            .setAutoStart(true) // mandatory for auto-starting ConfigurableWordCountTopology.
            .setEnableComponentScan(false)
            .run(args);
    }
}
