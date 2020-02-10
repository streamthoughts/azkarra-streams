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
package io.streamthoughts.azkarra.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.annotations.ConditionalOn;
import io.streamthoughts.azkarra.api.annotations.Eager;
import io.streamthoughts.azkarra.api.annotations.Factory;
import io.streamthoughts.azkarra.api.annotations.Primary;
import io.streamthoughts.azkarra.api.components.BaseComponentModule;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.metrics.annotations.ConditionalOnMetricsEnable;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;

/**
 * The default factory to build the primary {@link MeterRegistry}.
 */
@Factory
public class MeterRegistryFactory extends BaseComponentModule {

    private static final String COMPONENT_NAME = "AzkarraCompositeMeterRegistry";

    @Eager
    @Primary
    @Singleton
    @Component
    @ConditionalOnMetricsEnable
    @Named(MeterRegistryFactory.COMPONENT_NAME)
    public CompositeMeterRegistry compositeMeterRegistry() {
        final var compositeMeterRegistry = new CompositeMeterRegistry();
        getAllComponents(MeterRegistryConfigurer.class)
          .stream()
          .filter(c -> c.supports(compositeMeterRegistry))
          .forEach(c -> c.apply(compositeMeterRegistry));

        Collection<MeterRegistry> registries = registries();
        if (registries.isEmpty()) {
            compositeMeterRegistry.add(new SimpleMeterRegistry());
        } else {
            registries.forEach(compositeMeterRegistry::add);
        }
        return compositeMeterRegistry;
    }

    @Primary
    @Singleton
    @Component
    @ConditionalOnMetricsEnable
    public MeterRegistryConfigurer meterRegistryConfigurer() {
        Collection<MeterBinder> binders = getAllComponents(MeterBinder.class);
        Collection<MeterFilter> filters = getAllComponents(MeterFilter.class);
        return new MicrometerMeterRegistryConfigurer(binders, filters);
    }

    @Singleton
    @Component
    @ConditionalOnMetricsEnable
    @ConditionalOn(missingComponents = PrometheusMeterRegistry.class)
    public PrometheusMeterRegistry prometheusRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    private Collection<MeterRegistry> registries() {
        return getAllComponents(
            MeterRegistry.class,
            Qualifiers.excludeByName(COMPONENT_NAME) // exclude this component from being returned
        );
    }
}
