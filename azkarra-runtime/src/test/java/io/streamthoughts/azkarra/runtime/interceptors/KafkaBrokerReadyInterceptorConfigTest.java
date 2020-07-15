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

import io.streamthoughts.azkarra.api.config.Conf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.streamthoughts.azkarra.runtime.interceptors.KafkaBrokerReadyInterceptorConfig.*;

public class KafkaBrokerReadyInterceptorConfigTest {

    @Test
    public void shouldReturnDefaultValueGivenEmptyConfig() {
        var config = new KafkaBrokerReadyInterceptorConfig(Conf.empty());
        Assertions.assertEquals(KAFKA_READY_INTERCEPTOR_MIN_AVAILABLE_BROKERS_DEFAULT, config.getMinAvailableBrokers());
        Assertions.assertEquals(KAFKA_READY_INTERCEPTOR_RETRY_BACKOFF_MS_DEFAULT, config.getRetryBackoffMs());
        Assertions.assertEquals(KAFKA_READY_INTERCEPTOR_TIMEOUT_MS_DEFAULT, config.getTimeoutMs());
    }

    @Test
    public void shouldReturnConfiguredValueGivenNonEmptyConfig() {
        var config = new KafkaBrokerReadyInterceptorConfig(Conf.of(
                KAFKA_READY_INTERCEPTOR_MIN_AVAILABLE_BROKERS_CONFIG, "3",
                KAFKA_READY_INTERCEPTOR_RETRY_BACKOFF_MS_CONFIG, 100,
                KAFKA_READY_INTERCEPTOR_TIMEOUT_MS_CONFIG, 10
        ));
        Assertions.assertEquals(3, config.getMinAvailableBrokers());
        Assertions.assertEquals(100, config.getRetryBackoffMs());
        Assertions.assertEquals(10, config.getTimeoutMs());
    }
}