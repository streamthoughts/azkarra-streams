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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.time.Time;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.common.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

public class KafkaBrokerReadyInterceptor implements StreamsLifecycleInterceptor, Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaBrokerReadyInterceptor.class);

    enum InterceptorState implements State {WAITING_FOR_KAFKA_BROKERS_READY}

    private KafkaBrokerReadyInterceptorConfig config;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configs) {
        config = new KafkaBrokerReadyInterceptorConfig(configs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifecycleContext context, final StreamsLifecycleChain chain) {
        if (config == null) throw new IllegalStateException("interceptor not configured");

        final State previousState = context.streamsState();

        boolean isReady = false;
        int numBrokerAvailable = 0;
        try {
            LOG.info("Checking for Kafka to be ready. Expected broker(s): {}", config.getMinAvailableBrokers());
            final var container = context.container();
            final var adminClient = container.getAdminClient();

            final var start = Time.SYSTEM.milliseconds();
            var remaining = config.getTimeoutMs();

            while (remaining > 0) {
                context.setState(InterceptorState.WAITING_FOR_KAFKA_BROKERS_READY);
                try {
                    Collection<Node> nodes = getClusterNodes(adminClient, remaining);
                    numBrokerAvailable = nodes.size();
                    if (isReady = !nodes.isEmpty() && numBrokerAvailable >= config.getMinAvailableBrokers()) {
                        break;
                    }
                } catch (Exception e) {
                    LOG.error("Unexpected error while listing Kafka nodes", e);
                }
                Time.SYSTEM.sleep(Duration.ofMillis(Math.min(config.getRetryBackoffMs(), remaining(start))));

                LOG.info("Waiting for Kafka cluster to be ready. Expected {} brokers, but only {} was found.",
                    config.getMinAvailableBrokers(),
                    numBrokerAvailable
                );
                remaining = remaining(start);
            }

        } finally {
            // always reset the previous state.
            context.setState(previousState);
            if (!isReady) {
                LOG.warn(
                    "Kafka Cluster is not ready yet. Expected {} brokers, but only {} was found. TimeoutMs expires",
                    config.getMinAvailableBrokers(),
                    numBrokerAvailable
                );
            }
            chain.execute();
        }
    }

    private long remaining(long start) {
        return Math.max(0, config.getTimeoutMs() - (Time.SYSTEM.milliseconds() - start));
    }

    private Collection<Node> getClusterNodes(final AdminClient adminClient,
                                             final long remaining) throws InterruptedException, ExecutionException {
        return adminClient
            .describeCluster(timeoutMsOptions(remaining))
            .nodes()
            .get();
    }

    private static DescribeClusterOptions timeoutMsOptions(final long remaining) {
        return new DescribeClusterOptions().timeoutMs((int) Math.min(Integer.MAX_VALUE, remaining));
    }
}
