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
package io.streamthoughts.azkarra.streams.context.internal;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class which is used to initially configure a {@link AzkarraContext} instance.
 */
public class ApplicationConfig {

    public final static String CONTEXT_CONFIG              = "azkarra.context";
    public final static String CONTEXT_COMPONENT_CONFIG    = "azkarra.components";
    public final static String CONTEXT_ENVIRONMENTS_CONFIG = "azkarra.environments";

    /**
     * Static helper that can be used to creates a new {@link ApplicationConfig} instance
     * from the provided {@link Conf}.
     *
     * @return a new {@link ApplicationConfig} instance.
     */
    public static ApplicationConfig read(final Conf conf) {
        Objects.requireNonNull(conf, "conf cannot be null");
        return new Reader().read(conf);
    }

    private final Conf context;
    private final Set<String> components;
    private final List<EnvironmentConfig> environments;

    /**
     * Creates a new {@link ApplicationConfig} instance.
     *
     * @param context         the context configuration.
     * @param components      the components class.
     * @param environments    the environments configuration.
     */
    private ApplicationConfig(final Conf context,
                              final Set<String> components,
                              final List<EnvironmentConfig> environments) {
        this.context = context;
        this.components = components;
        this.environments = environments;
    }

    /**
     * Gets the context configuration.
     *
     * @return  the {@link Conf} instance.
     */
    public Conf context() {
        return Optional.ofNullable(context).orElse(Conf.empty());
    }

    /**
     * Gets all registered components class name.
     *
     * @return a set of string class name.
     */
    public Set<String> components() {
        return components;
    }

    /**
     * Gets all registered environments.
     *
     * @return  a list of {@link EnvironmentConfig} instances.
     */
    public List<EnvironmentConfig> environments() {
        return environments;
    }

    public static final class Reader {

        private static final String AZKARRA_METRICS = "azkarra.metrics";

        public ApplicationConfig read(final Conf conf) {

            Objects.requireNonNull(conf, "conf cannot be null");

            Conf context = conf.hasPath(CONTEXT_CONFIG) ? conf.getSubConf(CONTEXT_CONFIG) : Conf.empty();
            if (conf.hasPath(AZKARRA_METRICS)) {
                // Lookup for configuration prefixed with 'metrics' to simplify the user-defined configuration.
                // Note : this will be refactored in later version because this create a direct reference
                // to the Azkarra Metrics module.
                context = context.withFallback(Conf.of("metrics", conf.getSubConf(AZKARRA_METRICS)));
            }
            return new ApplicationConfig(
                context,
                mayGetConfiguredComponents(conf),
                mayGetConfiguredEnvironments(conf)
            );
        }

        private List<EnvironmentConfig> mayGetConfiguredEnvironments(final Conf originals) {
            if (!originals.hasPath(CONTEXT_ENVIRONMENTS_CONFIG)) {
                return Collections.emptyList();
            }

            return originals.getSubConfList(CONTEXT_ENVIRONMENTS_CONFIG)
                    .stream()
                    .map(EnvironmentConfig::read)
                    .collect(Collectors.toList());
        }

        private Set<String> mayGetConfiguredComponents(final Conf originals) {
            return originals.hasPath(CONTEXT_COMPONENT_CONFIG) ?
                    new HashSet<>(originals.getStringList(CONTEXT_COMPONENT_CONFIG)) :
                    Collections.emptySet();
        }
    }
}
