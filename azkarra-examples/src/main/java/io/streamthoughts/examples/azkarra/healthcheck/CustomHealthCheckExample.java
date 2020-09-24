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
package io.streamthoughts.examples.azkarra.healthcheck;

import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.http.health.Health;
import io.streamthoughts.azkarra.http.health.HealthIndicator;
import io.streamthoughts.azkarra.http.health.Status;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.AzkarraStreamsApplication;

import javax.inject.Singleton;

/**
 * Example for registering custom health-check.
 */
@AzkarraStreamsApplication
public class CustomHealthCheckExample {

    public static void main(final String[] args) {
        AzkarraApplication.run(CustomHealthCheckExample.class, args);
    }

    @Component
    @Singleton
    public static class MyCustomerHealthCheck implements HealthIndicator {

        @Override
        public Health getHealth() {
            return new Health.Builder()
                    .withName("my-custom-service")
                    .withStatus(Status.UP)
                    .build();
        }
    }
}
