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
package io.streamthoughts.azkarra.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;

import java.util.Collection;

/**
 * The default {@link MeterRegistryConfigurer} implementation to register a collection of
 * {@link MeterBinder} and {@link MeterFilter} on any {@link MeterRegistry}.
 */
public class MicrometerMeterRegistryConfigurer implements MeterRegistryConfigurer<MeterRegistry> {

    private final Collection<MeterBinder> binders;
    private final Collection<MeterFilter> filters;

    /**
     * Creates a new {@link MicrometerMeterRegistryConfigurer} instance.
     *
     * @param binders the list of {@link MeterBinder}.
     * @param filters the list of {@link MeterFilter}.
     */
    public MicrometerMeterRegistryConfigurer(
            final Collection<MeterBinder> binders,
            final Collection<MeterFilter> filters) {
        this.binders = binders;
        this.filters = filters;
    }

    /**
     * {@inheritDoc}
     *
     * @param meterRegistry the {@link MeterRegistry} to bind metrics to.
     */
    @Override
    public void apply(final MeterRegistry meterRegistry) {
        if (filters != null && !filters.isEmpty())
            filters.forEach(meterRegistry.config()::meterFilter);

        if (binders != null && !binders.isEmpty())
            binders.forEach(binder -> binder.bindTo(meterRegistry));
    }
}
