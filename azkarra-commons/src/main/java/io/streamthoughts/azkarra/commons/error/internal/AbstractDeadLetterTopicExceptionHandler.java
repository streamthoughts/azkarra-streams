/*
 * Copyright 2021 StreamThoughts.
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
package io.streamthoughts.azkarra.commons.error.internal;

import io.streamthoughts.azkarra.commons.error.DeadLetterTopicExceptionHandlerConfig;
import io.streamthoughts.azkarra.commons.error.ExceptionType;
import io.streamthoughts.azkarra.commons.error.GlobalDeadLetterTopicManager;
import io.streamthoughts.azkarra.commons.error.GlobalDeadLetterTopicManagerConfig;
import io.streamthoughts.azkarra.commons.error.HandlerResponse;
import org.apache.kafka.common.Configurable;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.streams.StreamsConfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AbstractDeadLetterTopicExceptionHandler implements Configurable {

  private DeadLetterTopicExceptionHandlerConfig config;

  private List<Header> customHeaders;

  private String applicationId;

  private final ExceptionType exceptionTypes;

  /**
   * Creates a new {@link AbstractDeadLetterTopicExceptionHandler} instance.
   *
   * @param exceptionTypes  the exception type.
   */
  public AbstractDeadLetterTopicExceptionHandler(final ExceptionType exceptionTypes) {
    this.exceptionTypes = Objects.requireNonNull(exceptionTypes, "'exceptionType' should not be null");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void configure(final Map<String, ?> configs) {
    config = new DeadLetterTopicExceptionHandlerConfig(configs, exceptionTypes);
    applicationId = (String) configs.get(StreamsConfig.APPLICATION_ID_CONFIG);
    customHeaders = config.customHeaders();

    mayInitializeGlobalDeadLetterTopicManager(config.globalProducerConfigs());
  }

  private void mayInitializeGlobalDeadLetterTopicManager(final Map<String, Object> internalProducerConfig) {
    if (!internalProducerConfig.isEmpty() && !GlobalDeadLetterTopicManager.initialized()) {
      GlobalDeadLetterTopicManager.initialize(
          GlobalDeadLetterTopicManagerConfig.create()
          .withProducerConfig(internalProducerConfig)
          .withAdminClientConfig(config.globalAdminConfigs())
          .withTopicPartitions(config.topicPartitions())
          .withAutoCreateTopicEnabled(config.isAutoCreateTopicEnabled())
          .withReplicationFactor(config.topicReplicationFactor())
      );
    }
  }

  protected DeadLetterTopicExceptionHandlerConfig config() {
    return config;
  }

  protected String applicationId() {
    return applicationId;
  }

  protected List<Header> customHeaders() {
    return customHeaders;
  }

  protected HandlerResponse getHandlerResponseForExceptionOrElse(
      final Exception exception, final HandlerResponse defaultResponse) {
    for (Class<?> cls : config.getFatalExceptions()) {
      if (cls.isAssignableFrom(exception.getClass())) {
        return HandlerResponse.FAIL;
      }
    }
    for (Class<?> cls : config.getIgnoredExceptions()) {
      if (cls.isAssignableFrom(exception.getClass())) {
        return HandlerResponse.CONTINUE;
      }
    }
    return config.defaultHandlerResponseOrElse(defaultResponse);
  }
}
