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
package io.streamthoughts.azkarra.metrics.endpoints;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.server.AzkarraRestExtension;
import io.streamthoughts.azkarra.api.server.AzkarraRestExtensionContext;
import io.streamthoughts.azkarra.metrics.AzkarraMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Objects;

/**
 * Endpoint to scrap metrics in prometheus format.
 */
@Path("/")
public class PrometheusEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusEndpoint.class);

    private PrometheusMeterRegistry registry;

    /**
     * Creates a new {@link PrometheusEndpoint} instance.
     * @param registry  the {@link PrometheusMeterRegistry} instance; ust bot be {@code null}.
     */
    public PrometheusEndpoint(final PrometheusMeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    @GET
    @Path("/prometheus")
    @Produces(MediaType.TEXT_PLAIN)
    public String prometheus() {
        return registry.scrape();
    }

    public static class PrometheusEndpointExtension implements AzkarraRestExtension {

        /**
         * {@inheritDoc}
         */
        @Override
        public void register(final AzkarraRestExtensionContext restContext) {
            final AzkarraContext azkarraContext = restContext.context();
            if (!isEndpointEnable(azkarraContext)) {
                LOG.info("PrometheusEndpoint will not be registered to JAX-RS context. Endpoint not enabled.");
                return;
            }
            PrometheusMeterRegistry registry = azkarraContext.getComponent(PrometheusMeterRegistry.class);
            restContext.configurable().register(new PrometheusEndpoint(registry));
        }

        private boolean isEndpointEnable(final AzkarraContext context) {
            AzkarraMetricsConfig metricsConfig = new AzkarraMetricsConfig(context.getConfiguration());
            return metricsConfig.isEnable(AzkarraMetricsConfig.METRICS_ENABLE_CONFIG) &&
                   metricsConfig.isEnable(AzkarraMetricsConfig.METRICS_ENDPOINT_PROMETHEUS_ENABLE_CONFIG);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {

        }
    }
}
