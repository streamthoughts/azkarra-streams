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

/**
 * Interface that is used to configure a specific type of {@link MeterRegistry}.
 *
 * Any component that implements this interface will be applied on the supported {@link MeterRegistry} during
 * the {@link io.streamthoughts.azkarra.api.components.ComponentFactory} initialization.
 *
 * @see MeterRegistryFactory
 * @see MicrometerMeterRegistryConfigurer
 *
 * @param <T>   the registry type to configure.
 */
@FunctionalInterface
public interface MeterRegistryConfigurer<T extends MeterRegistry> {

    /**
     * Applies this configurer to the given meter registry.
     *
     * @param meterRegistry the {@link MeterRegistry} to configure.
     */
    void apply(final T meterRegistry);

    /**
     * Checks whether this configurer supports the given meter registry type.
     *
     * @param meterRegistry a {@link MeterRegistry}.
     * @return boolean      {@code true} if the meter registry is supported.
     */
    default boolean supports(final T meterRegistry) {
        return true;
    }
}