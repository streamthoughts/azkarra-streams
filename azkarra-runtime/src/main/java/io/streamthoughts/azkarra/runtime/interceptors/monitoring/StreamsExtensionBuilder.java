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
package io.streamthoughts.azkarra.runtime.interceptors.monitoring;

import io.streamthoughts.azkarra.api.AzkarraVersion;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.ce.CloudEventsExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StreamsExtensionBuilder {

    private static final String EXTENSION_PREFIX = "ioazkarra";

    private String applicationId;
    private String applicationServer;
    private Map<String, Object> additional = new HashMap<>();

    public static StreamsExtensionBuilder newBuilder() {
        return new StreamsExtensionBuilder();
    }

    private StreamsExtensionBuilder() {
    }

    /**
     * Sets the Kafka Streams application .erver.
     *
     * @param   applicationServer the{@link org.apache.kafka.streams.StreamsConfig#APPLICATION_SERVER_CONFIG} property.
     * @return  {@code this}
     */
    public StreamsExtensionBuilder withApplicationServer(final String applicationServer) {
        this.applicationServer = applicationServer;
        return this;
    }

    /**
     * Sets the Kafka Streams application.id.
     *
     * @param   applicationId the{@link org.apache.kafka.streams.StreamsConfig#APPLICATION_ID_CONFIG} property.
     * @return  {@code this}
     */
    public StreamsExtensionBuilder withApplicationId(final String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public StreamsExtensionBuilder with(final String key, final Object value) {
        additional.put(EXTENSION_PREFIX + key, value);
        return this;
    }

    public CloudEventsExtension build() {
        return () -> {
            final Map<String, Object> extensions = new HashMap<>(additional);
            extensions.put(EXTENSION_PREFIX + "version", AzkarraVersion.getVersion());
            extensions.put(EXTENSION_PREFIX + "streamsappid", applicationId);
            extensions.put(EXTENSION_PREFIX + "streamsappserver", applicationServer);
            return Collections.unmodifiableMap(extensions);
        };
    }
}
