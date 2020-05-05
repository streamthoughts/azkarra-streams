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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.util.Version;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.kstream.Materialized;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoCreateTopicsInterceptorTest {

    @Mock
    private AdminClient mkClient;

    @Mock
    private AutoCreateTopicsInterceptor interceptor;

    @Mock
    private StreamsLifecycleChain chain;

    @Captor
    private ArgumentCaptor<Collection<NewTopic>> captorNewTopics;

    @Captor
    private ArgumentCaptor<Collection<String>> captorDeleteTopics;

    @BeforeEach
    public void setUp() throws ExecutionException, InterruptedException {
        MockitoAnnotations.initMocks(this);

        // mock Create topics
        CreateTopicsResult mkCreateResult = mock(CreateTopicsResult.class);
        KafkaFuture mockCreateFuture = mock(KafkaFuture.class);
        when(mkCreateResult.all()).thenReturn(mockCreateFuture);
        when(mkClient.createTopics(Mockito.anyCollection())).thenReturn(mkCreateResult);

        // mock Listing topics
        ListTopicsResult mkListResult = mock(ListTopicsResult.class);
        KafkaFuture<Collection<TopicListing>> mockListingFuture = mock(KafkaFuture.class);

        when(mockListingFuture.get()).thenReturn(Arrays.asList(
            new TopicListing("test-count-repartition", false),
            new TopicListing("test-count-changelog", false)
        ));
        when(mkListResult.listings()).thenReturn(mockListingFuture);
        when(mkClient.listTopics()).thenReturn(mkListResult);

        // mock Delete topics
        DeleteTopicsResult mkDeleteResult = mock(DeleteTopicsResult.class);
        KafkaFuture mkDeleteFuture = mock(KafkaFuture.class);
        when(mkDeleteResult.all()).thenReturn(mkDeleteFuture);
        when(mkClient.deleteTopics(anyCollection())).thenReturn(mkDeleteResult);

        interceptor = new AutoCreateTopicsInterceptor(mkClient);
        interceptor.configure(Conf.empty());
    }

    @Test
    public void shouldAutoCreateTopicsWhenStreamsStart() {
        StreamsLifecycleContext context = newContext();
        interceptor.onStart(context, chain);

        Mockito.verify(mkClient).createTopics(captorNewTopics.capture());
        Collection<String> createdTopics = captorNewTopics.getValue()
                .stream()
                .map(NewTopic::name)
                .collect(Collectors.toList());
        Assertions.assertEquals(2, createdTopics.size());
        Assertions.assertTrue(createdTopics.contains("input-topic"));
        Assertions.assertTrue(createdTopics.contains("output-topic"));
    }

    @Test
    public void shouldAutoCreateTopicsWhenStreamsStartGivenTopics() {
        StreamsLifecycleContext context = newContext();

        NewTopic inputTopic = new NewTopic("input-topic", 3, (short) 1);
        NewTopic outputTopic = new NewTopic("output-topic", 6, (short) 2);

        interceptor.setTopics(Arrays.asList(inputTopic, outputTopic));
        interceptor.onStart(context, chain);

        Mockito.verify(mkClient).createTopics(captorNewTopics.capture());
        List<NewTopic> createdTopics = new ArrayList<>(captorNewTopics.getValue());
        Assertions.assertEquals(2, createdTopics.size());
        createdTopics.sort(Comparator.comparingInt(NewTopic::numPartitions));

        NewTopic actualInputTopic = createdTopics.get(0);
        Assertions.assertEquals(inputTopic.name(), actualInputTopic.name());
        Assertions.assertEquals(inputTopic.numPartitions(), actualInputTopic.numPartitions());
        Assertions.assertEquals(inputTopic.replicationFactor(), actualInputTopic.replicationFactor());

        NewTopic actualOutputTopic = createdTopics.get(1);
        Assertions.assertEquals(outputTopic.name(), actualOutputTopic.name());
        Assertions.assertEquals(outputTopic.numPartitions(), actualOutputTopic.numPartitions());
        Assertions.assertEquals(outputTopic.replicationFactor(), actualOutputTopic.replicationFactor());
    }

    @Test
    public void shouldAutoDeleteTopicsWhenStreamsStop() {
        StreamsLifecycleContext context = newContext();

        interceptor.setDeleteTopicsOnStreamsClosed(true);
        interceptor.onStop(context, chain);

        Mockito.verify(mkClient).deleteTopics(captorDeleteTopics.capture());
        Collection<String> deletedTopics = captorDeleteTopics.getValue();
        Assertions.assertEquals(2, deletedTopics.size());
        Assertions.assertTrue(deletedTopics.contains("test-count-repartition"));
        Assertions.assertTrue(deletedTopics.contains("test-count-changelog"));
    }

    private StreamsLifecycleContext newContext() {
        return new StreamsLifecycleContext() {
            State state = State.CREATED;

            @Override
            public String applicationId() {
                return "test";
            }

            @Override
            public TopologyDescription topologyDescription() {
                return topology();
            }

            @Override
            public String topologyName() {
                return "test";
            }

            @Override
            public Version topologyVersion() {
                return Version.parse("1.0");
            }

            @Override
            public Conf streamsConfig() {
                return Conf.empty();
            }

            @Override
            public State streamsState() {
                return state;
            }

            @Override
            public void setState(State state) {
                this.state = state;
            }

            @Override
            public void addStateChangeWatcher(KafkaStreamsContainer.StateChangeWatcher watcher) {

            }
        };
    }

    private static TopologyDescription topology() {
        StreamsBuilder builder = new StreamsBuilder();
        builder.stream("input-topic")
            .groupBy((k, v) -> v)
            .count(Materialized.as("count"))
            .toStream()
            .to("output-topic");
        return builder.build().describe();
    }
}