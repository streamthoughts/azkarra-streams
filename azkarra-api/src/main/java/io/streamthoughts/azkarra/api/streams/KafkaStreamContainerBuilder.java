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
package io.streamthoughts.azkarra.api.streams;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.consumer.MonitorOffsetsConsumerInterceptor;
import io.streamthoughts.azkarra.api.streams.listener.CompositeStateListener;
import io.streamthoughts.azkarra.api.streams.listener.CompositeStateRestoreListener;
import io.streamthoughts.azkarra.api.streams.listener.CompositeUncaughtExceptionHandler;
import io.streamthoughts.azkarra.api.streams.rocksdb.DefaultRocksDBConfigSetter;
import io.streamthoughts.azkarra.api.streams.topology.TopologyContainer;
import io.streamthoughts.azkarra.api.time.Time;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.StateRestoreListener;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.kafka.clients.consumer.ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.MAIN_CONSUMER_PREFIX;
import static org.apache.kafka.streams.StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG;

/**
 * Default builder class for creating and configuring a new wrapped {@link KafkaStreams} instance.
 */
public class KafkaStreamContainerBuilder {

    private TopologyContainer topologyContainer;
    private KafkaStreamsFactory kafkaStreamsFactory;
    private List<StateRestoreListener> restoreListeners = Collections.emptyList();
    private List<KafkaStreams.StateListener> stateListeners = Collections.emptyList();
    private List<Thread.UncaughtExceptionHandler> exceptionHandlers = Collections.emptyList();

    /**
     * Creates a new {@link KafkaStreamContainerBuilder} instance.
     *
     * @return a new {@link KafkaStreamContainerBuilder} instance.
     */
    public static KafkaStreamContainerBuilder newBuilder() {
        return new KafkaStreamContainerBuilder();
    }

    private KafkaStreamContainerBuilder() {

    }

    public KafkaStreamContainerBuilder withKafkaStreamsFactory(final KafkaStreamsFactory kafkaStreamsFactory) {
        this.kafkaStreamsFactory = kafkaStreamsFactory;
        return this;
    }

    public KafkaStreamContainerBuilder withTopologyContainer(final TopologyContainer topologyContainer) {
        this.topologyContainer = topologyContainer;
        return this;
    }

    public KafkaStreamContainerBuilder withRestoreListeners(final List<StateRestoreListener> listeners) {
        this.restoreListeners = listeners;
        return this;
    }

    public KafkaStreamContainerBuilder withUncaughtExceptionHandler(
            final List<Thread.UncaughtExceptionHandler> handlers) {
        this.exceptionHandlers = handlers;
        return this;
    }

    public KafkaStreamContainerBuilder withStateListeners(final List<KafkaStreams.StateListener> listeners) {
        this.stateListeners = listeners;
        return this;
    }

    /**
     * Builds a {@link KafkaStreams} instance.
     *
     * @return a new {@link KafkaStreamsContainer} instance.
     */
    public KafkaStreamsContainer build() {

        Conf rocksDBConf = Conf.empty();

        // Configure default RocksDB setter class if no one is already defined.
        Conf streamsConfig = topologyContainer.streamsConfig();
        if (!streamsConfig.hasPath(ROCKSDB_CONFIG_SETTER_CLASS_CONFIG)) {
            rocksDBConf = rocksDBConf.withFallback(
                Conf.with(ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, DefaultRocksDBConfigSetter.class.getName())
            );
        }

        streamsConfig = streamsConfig.withFallback(rocksDBConf);

        topologyContainer.streamsConfig(streamsConfig);

        InternalKafkaStreamsFactory delegate = new InternalKafkaStreamsFactory(kafkaStreamsFactory);
        final KafkaStreamsContainer container = new KafkaStreamsContainer(topologyContainer, delegate);
        delegate.setContainer(container);
        return container;
    }

    private class InternalKafkaStreamsFactory implements KafkaStreamsFactory {

        private final KafkaStreamsFactory factory;

        private KafkaStreamsContainer container;

        /**
         * Creates a new {@link InternalKafkaStreamsFactory} instance.
         *
         * @param factory  the {@link KafkaStreamsFactory} instance to delegate creation.
         */
        InternalKafkaStreamsFactory(final KafkaStreamsFactory factory) {
            this.factory = Objects.requireNonNull(factory, "factory cannot be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public KafkaStreams make(final Topology topology, final Conf streamsConfig) {

            // add built-in interceptor for monitoring consumer offsets.
            final String interceptorConfig = MAIN_CONSUMER_PREFIX + INTERCEPTOR_CLASSES_CONFIG;

            final String interceptors = Stream.of(
                streamsConfig.getOptionalString(interceptorConfig).orElse(null),
                MonitorOffsetsConsumerInterceptor.class.getName())
            .filter(s -> s != null && !s.isEmpty())
            .collect(Collectors.joining(","));

            final Conf finalStreamsConfig = Conf.with(interceptorConfig, interceptors).withFallback(streamsConfig);

            // delegate KafkaStreams instantiation to user-factory.
            final KafkaStreams kafkaStreams = factory.make(topology, finalStreamsConfig);

            final CompositeStateListener compositeStateListener = new CompositeStateListener(stateListeners);
            compositeStateListener.addListener((newState, oldState) -> {
                final StateChangeEvent event = new StateChangeEvent(
                    Time.SYSTEM.milliseconds(),
                    State.valueOf(newState.name()),
                    State.valueOf(oldState.name())
                );
                container.stateChanges(event);
            });

            final CompositeUncaughtExceptionHandler handler = new CompositeUncaughtExceptionHandler();
            handler.addHandler((t, e) -> {
                container.logger().error("Handling uncaught streams thread exception: {}", e.getMessage());
                container.setException(e);
            });

            if (exceptionHandlers != null) {
                exceptionHandlers.forEach(handler::addHandler);
            }

            kafkaStreams.setStateListener(compositeStateListener);
            kafkaStreams.setUncaughtExceptionHandler(handler);
            kafkaStreams.setGlobalStateRestoreListener(new CompositeStateRestoreListener(restoreListeners));

            return kafkaStreams;
        }

        private void setContainer(final KafkaStreamsContainer container) {
            this.container = container;
        }
    }
}
