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

package io.streamthoughts.azkarra.metrics.binders;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.annotations.ConditionalOn;
import io.streamthoughts.azkarra.api.annotations.Factory;
import io.streamthoughts.azkarra.metrics.AzkarraMetricsConfig;
import io.streamthoughts.azkarra.metrics.annotations.ConditionalOnMetricsEnable;

import javax.inject.Singleton;

/**
 * Factory to register JVM metrics binders.
 */
@Factory
public class JvmMeterRegistryBinderFactory {

    /**
     * Return the JVM Memory metrics component.
     *
     * @return the {@link JvmGcMetrics}
     */
    @Component
    @Singleton
    @ConditionalOnMetricsEnable
    @ConditionalOn(property = AzkarraMetricsConfig.METRICS_BINDERS_JVM_ENABLE_CONFIG, havingValue = "true")
    @ConditionalOn(missingComponents = JvmGcMetrics.class)
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    /**
     * Return the JVM Memory metrics component.
     *
     * @return the {@link JvmMemoryMetrics}
     */
    @Component
    @Singleton
    @ConditionalOnMetricsEnable
    @ConditionalOn(property = AzkarraMetricsConfig.METRICS_BINDERS_JVM_ENABLE_CONFIG, havingValue = "true")
    @ConditionalOn(missingComponents = JvmMemoryMetrics.class)
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    /**
     * Return the JVM Thread metrics component.
     *
     * @return the {@link JvmThreadMetrics}.
     */
    @Component
    @Singleton
    @ConditionalOnMetricsEnable
    @ConditionalOn(property = AzkarraMetricsConfig.METRICS_BINDERS_JVM_ENABLE_CONFIG, havingValue = "true")
    @ConditionalOn(missingComponents = JvmThreadMetrics.class)
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    /**
     * Return the JVM Class loader metrics component.
     *
     * @return the {@link ClassLoaderMetrics}.
     */
    @Component
    @Singleton
    @ConditionalOnMetricsEnable
    @ConditionalOn(property = AzkarraMetricsConfig.METRICS_BINDERS_JVM_ENABLE_CONFIG, havingValue = "true")
    @ConditionalOn(missingComponents = ClassLoaderMetrics.class)
    public ClassLoaderMetrics classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }
}
