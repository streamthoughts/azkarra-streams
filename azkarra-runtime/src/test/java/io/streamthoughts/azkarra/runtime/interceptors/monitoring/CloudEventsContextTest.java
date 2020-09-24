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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CloudEventsContextTest {

    private static final String CLUSTER_ID = "xzcevr";
    private static final String APPLICATION_ID = "myId";

    @Test
    public void shouldGetCloudEventSourceGivenApplicationServer() {
        var context = new CloudEventsContext(APPLICATION_ID, "localhost:8080", CLUSTER_ID);
        assertEquals("arn://kafka=xzcevr/host=localhost/port=8080", context.cloudEventSource());
    }

    @Test
    public void shouldGetCloudEventSourceGivenNoApplicationServer() {
        var context = new CloudEventsContext("myId", null, CLUSTER_ID);
        assertEquals("arn://kafka=xzcevr", context.cloudEventSource());
    }

    @Test
    public void shouldGetCloudEventSubject() {
        var context = new CloudEventsContext("myId", "localhost:8080", CLUSTER_ID);
        assertEquals("arn://kafka=xzcevr/host=localhost/port=8080/streams=myId", context.cloudEventSubject());
    }
}