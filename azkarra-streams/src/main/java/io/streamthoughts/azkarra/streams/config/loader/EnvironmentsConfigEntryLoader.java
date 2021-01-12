/*
 * Copyright 2019-2021 StreamThoughts.
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

package io.streamthoughts.azkarra.streams.config.loader;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextListener;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.config.ConfEntry;
import io.streamthoughts.azkarra.runtime.StreamsExecutionEnvironmentAbstractFactory;
import io.streamthoughts.azkarra.streams.AbstractConfigEntryLoader;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.config.loader.internal.EnvironmentConfig;
import io.streamthoughts.azkarra.streams.config.loader.internal.TopologyConfig;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EnvironmentsConfigEntryLoader extends AbstractConfigEntryLoader {

    private static final String CONFIG_ENTRY_KEY = "environments";

    /**
     * Creates a new {@link EnvironmentsConfigEntryLoader} instance.
     */
    public EnvironmentsConfigEntryLoader() {
        super(CONFIG_ENTRY_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(final ConfEntry configEntryObject, final AzkarraApplication application) {
        final List<EnvironmentConfig> environmentConfigEntries = loadConfiguredEnvironments(configEntryObject);
        application.getContext().addListener(new EnvironmentContextLoader(environmentConfigEntries));
    }

    private List<EnvironmentConfig> loadConfiguredEnvironments(final ConfEntry configEntryObject) {
        return configEntryObject.asSubConfList()
            .stream()
            .map(EnvironmentConfig::read)
            .collect(Collectors.toList());
    }

    private static class EnvironmentContextLoader implements AzkarraContextListener {

        final List<EnvironmentConfig> environmentConfigEntries;

        public EnvironmentContextLoader(final List<EnvironmentConfig> environmentConfigEntries) {
            this.environmentConfigEntries = environmentConfigEntries;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onContextStart(final AzkarraContext context) {
            var factory = context.getComponent(StreamsExecutionEnvironmentAbstractFactory.class);
            for (EnvironmentConfig environmentConfigEntry : environmentConfigEntries) {
                final StreamsExecutionEnvironment<?> environment = factory.create(
                    environmentConfigEntry.type(),
                    environmentConfigEntry.name(),
                    environmentConfigEntry.config()
                );
                context.addExecutionEnvironment(environment);
                List<TopologyConfig> topologyConfigs = environmentConfigEntry.topologyStreamConfigs();
                for (TopologyConfig topology : topologyConfigs) {
                    Executed executed = Executed.as(topology.name().orElse(null));
                    executed = topology.config().map(executed::withConfig).orElse(executed);
                    executed = topology.description().map(executed::withDescription).orElse(executed);

                    Optional<String> version = topology.version();
                    context.addTopology(topology.type(), version.orElse(null), environmentConfigEntry.name(), executed);
                }
            }
        }
    }
}
