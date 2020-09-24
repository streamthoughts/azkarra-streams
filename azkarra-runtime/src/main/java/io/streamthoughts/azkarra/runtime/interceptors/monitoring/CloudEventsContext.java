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

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * This class is used to wrap information about a Cloud Events
 * in the context of Kafka Streams.
 */
public class CloudEventsContext {

    // Azkarra Resource Name (ARN) prefix.
    private static final String RESOURCE_NAME_SERVER_PREFIX = "arn://kafka=%s";

    private final String applicationId;
    private final String applicationServer;
    private final String clusterId;

    private final String cloudEventSource;
    private final String cloudEventSubject;

    /**
     * Creates a new {@link CloudEventsContext} instance.
     *
     * @param applicationId         the application-id.
     * @param applicationServer     the application-server.
     * @param clusterId             the kafka cluster-id.
     */
    public CloudEventsContext(final String applicationId,
                              final String applicationServer,
                              final String clusterId) {
        this.applicationId = Objects.requireNonNull(applicationId, "application cannot be null");
        this.applicationServer = applicationServer;
        this.clusterId = clusterId;
        this.cloudEventSource = buildCloudEventSource();
        this.cloudEventSubject = buildCloudEventSubject();
    }

    String applicationId() {
        return applicationId;
    }

    String applicationServer() {
        return applicationServer;
    }

    String cloudEventSource() {
        return cloudEventSource;
    }

    String cloudEventSubject() {
        return cloudEventSubject;
    }

    String cloudEventId(final ZonedDateTime now) {
        return "appid:" + applicationId + ";appsrv:" + applicationServer + ";ts:" + now.toInstant().toEpochMilli();
    }

    private String buildCloudEventSource() {
        if (applicationServer == null) {
            return String.format(RESOURCE_NAME_SERVER_PREFIX, clusterId);
        }

        var hostAndPort = applicationServer.split(":");
        return String.format(
            RESOURCE_NAME_SERVER_PREFIX + "/host=%s/port=%s",
            clusterId,
            hostAndPort[0],
            hostAndPort[1]
        );
    }

    private String buildCloudEventSubject() {
        final var source = buildCloudEventSource();
        return source + "/streams=" + applicationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CloudEventsContext)) return false;
        CloudEventsContext that = (CloudEventsContext) o;
        return Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(applicationServer, that.applicationServer) &&
                Objects.equals(clusterId, that.clusterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, applicationServer, clusterId);
    }

    @Override
    public String toString() {
        return "CloudEventsContext{" +
                "applicationId='" + applicationId + '\'' +
                ", applicationServer='" + applicationServer + '\'' +
                ", clusterId='" + clusterId + '\'' +
                '}';
    }
}
