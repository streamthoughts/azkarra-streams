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
package io.streamthoughts.azkarra.runtime.streams;

import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainerAware;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsFactory;
import io.streamthoughts.azkarra.api.streams.consumer.MonitorOffsetsConsumerInterceptor;
import io.streamthoughts.azkarra.api.streams.errors.DelegatingUncaughtExceptionHandler;
import io.streamthoughts.azkarra.api.streams.errors.StreamThreadExceptionHandler;
import io.streamthoughts.azkarra.api.streams.listener.CompositeStateListener;
import io.streamthoughts.azkarra.api.streams.listener.CompositeStateRestoreListener;
import io.streamthoughts.azkarra.api.streams.listener.CompositeUncaughtExceptionHandler;
import io.streamthoughts.azkarra.api.streams.topology.TopologyDefinition;
import io.streamthoughts.azkarra.commons.streams.LoggingStateRestoreListener;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.StateRestoreListener;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.apache.kafka.clients.consumer.ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.MAIN_CONSUMER_PREFIX;

/**
 * Default builder class for creating and configuring a new wrapped {@link KafkaStreams} instance.
 */
public class LocalKafkaStreamsContainerBuilder {

    private final static List<StreamsConfigDecorator> CONFIG_DECORATORS = List.of(
        new MonitorConsumerInterceptorConfigDecorator()
    );

    private String containerId;
    private TopologyDefinition topologyDefinition;
    private KafkaStreamsFactory kafkaStreamsFactory;
    private Conf streamsConfig;
    private List<StateRestoreListener> restoreListeners = Collections.emptyList();
    private List<KafkaStreams.StateListener> stateListeners = Collections.emptyList();
    private List<StreamThreadExceptionHandler> exceptionHandlers = Collections.emptyList();
    private List<StreamsLifecycleInterceptor> interceptors = Collections.emptyList();

    public LocalKafkaStreamsContainerBuilder withContainerId(final String containerId) {
        this.containerId = containerId;
        return this;
    }

    public LocalKafkaStreamsContainerBuilder withStreamsConfig(final Conf streamsConfig) {
        this.streamsConfig = streamsConfig;
        return this;
    }

    public LocalKafkaStreamsContainerBuilder withInterceptors(final List<StreamsLifecycleInterceptor> interceptors) {
        this.interceptors = interceptors;
        return this;
    }

    public LocalKafkaStreamsContainerBuilder withKafkaStreamsFactory(final KafkaStreamsFactory kafkaStreamsFactory) {
        this.kafkaStreamsFactory = kafkaStreamsFactory;
        return this;
    }

    public LocalKafkaStreamsContainerBuilder withTopologyDefinition(final TopologyDefinition topologyDefinition) {
        this.topologyDefinition = topologyDefinition;
        return this;
    }

    public LocalKafkaStreamsContainerBuilder withRestoreListeners(final List<StateRestoreListener> listeners) {
        this.restoreListeners = listeners;
        return this;
    }

    public LocalKafkaStreamsContainerBuilder withStreamThreadExceptionHandlers(
            final List<StreamThreadExceptionHandler> handlers) {
        this.exceptionHandlers = handlers;
        return this;
    }

    public LocalKafkaStreamsContainerBuilder withStateListeners(final List<KafkaStreams.StateListener> listeners) {
        this.stateListeners = listeners;
        return this;
    }

    /**
     * Builds a {@link KafkaStreams} instance.
     *
     * @return a new {@link KafkaStreamsContainer} instance.
     */
    public LocalKafkaStreamsContainer build() {
        Conf enrichedStreamsConfig = streamsConfig;
        for (StreamsConfigDecorator decorator : CONFIG_DECORATORS) {
            enrichedStreamsConfig = decorator.apply(enrichedStreamsConfig);
        }

        final var delegatingKafkaStreamsFactory = new DelegatingKafkaStreamsFactory(kafkaStreamsFactory);
        final var container = new LocalKafkaStreamsContainer(
            containerId,
            enrichedStreamsConfig,
            topologyDefinition,
            delegatingKafkaStreamsFactory,
            interceptors
        );
        delegatingKafkaStreamsFactory.setKafkaStreamsContainer(container);
        return container;
    }

    /**
     * The {@code StreamsConfigDecorator} can be used to provide additional properties to the streams configuration.
     */
    @FunctionalInterface
    private interface StreamsConfigDecorator {

        /**
         * Decorates the given {@link Conf}.
         *
         * @param streamsConfig the streams config.
         * @return              the decorated config.
         */
        Conf apply(final Conf streamsConfig);

    }

    private static class MonitorConsumerInterceptorConfigDecorator implements StreamsConfigDecorator {

        private static final String INTERCEPTORS_CONFIG_KEY = MAIN_CONSUMER_PREFIX + INTERCEPTOR_CLASSES_CONFIG;

        /**
         * {@inheritDoc}
         */
        @Override
        public Conf apply(final Conf streamsConfig) {
            var interceptorClasses = MonitorOffsetsConsumerInterceptor.class.getName();

            if (streamsConfig.hasPath(INTERCEPTORS_CONFIG_KEY)) {
                interceptorClasses = "," + streamsConfig.getString(INTERCEPTORS_CONFIG_KEY);
            }

            var interceptorClassesConfig = Conf.of(INTERCEPTORS_CONFIG_KEY, interceptorClasses);
            return Conf.of(interceptorClassesConfig, streamsConfig);
        }
    }

    private class DelegatingKafkaStreamsFactory implements KafkaStreamsFactory, KafkaStreamsContainerAware {

        private final KafkaStreamsFactory factory;
        private LocalKafkaStreamsContainer container;

        /**
         * Creates a new {@link DelegatingKafkaStreamsFactory} instance.
         *
         * @param factory  the {@link KafkaStreamsFactory} instance to delegate creation.
         */
        DelegatingKafkaStreamsFactory(final KafkaStreamsFactory factory) {
            this.factory = Objects.requireNonNull(factory, "factory cannot be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public KafkaStreams make(final Topology topology, final Conf streamsConfig) {

            // Delegate KafkaStreams instantiation to user-factory.
            if (KafkaStreamsContainerAware.class.isAssignableFrom(factory.getClass())) {
                ((KafkaStreamsContainerAware)factory).setKafkaStreamsContainer(container);
            }

            final KafkaStreams kafkaStreams = factory.make(topology, streamsConfig);

            initStateListener(kafkaStreams);
            initUncaughtExceptionHandler(kafkaStreams);
            initGlobalStateRestoreListener(kafkaStreams);

            return kafkaStreams;
        }

        public void initGlobalStateRestoreListener(final KafkaStreams kafkaStreams) {

            final LoggingStateRestoreListener loggingListener = new LoggingStateRestoreListener();
            container.setStateRestoreService(loggingListener);

            final CompositeStateRestoreListener listener = new CompositeStateRestoreListener(restoreListeners);
            listener.addListener(loggingListener);
            listener.addListener(new UpdateContainerStateRestoreListener());
            listener.setKafkaStreamsContainer(container);

            kafkaStreams.setGlobalStateRestoreListener(listener);
        }

        private void initStateListener(final KafkaStreams kafkaStreams) {
            final CompositeStateListener listener = new CompositeStateListener(stateListeners);
            listener.addListener(new UpdateContainerStateListener());
            listener.setKafkaStreamsContainer(container);
            kafkaStreams.setStateListener(listener);
        }

        private void initUncaughtExceptionHandler(final KafkaStreams kafkaStreams) {
            final var compositeUncaughtExceptionHandler = new CompositeUncaughtExceptionHandler();
            compositeUncaughtExceptionHandler.addHandler((t, e) -> {
                container.logger().error("Handling uncaught streams thread exception: {}", e.getMessage());
                container.setException(e);
            });

            if (exceptionHandlers != null) {
                exceptionHandlers
                .stream()
                .map(handler -> new DelegatingUncaughtExceptionHandler(container, handler))
                .forEach(compositeUncaughtExceptionHandler::addHandler);
            }
            kafkaStreams.setUncaughtExceptionHandler(compositeUncaughtExceptionHandler);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setKafkaStreamsContainer(final KafkaStreamsContainer container) {
            this.container = (LocalKafkaStreamsContainer) container;
        }
    }
}
