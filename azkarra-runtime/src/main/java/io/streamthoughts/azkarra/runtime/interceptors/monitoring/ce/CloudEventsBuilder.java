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
package io.streamthoughts.azkarra.runtime.interceptors.monitoring.ce;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

public class CloudEventsBuilder<T> extends CloudEventsAttributes {

    private T data;
    private List<CloudEventsExtension> extensions;

    public static <T> CloudEventsBuilder<T> newBuilder() {
        return new CloudEventsBuilder<>();
    }

    /**
     * Creates a new {@link CloudEventsBuilder} instance.
     */
    private CloudEventsBuilder() {
        super();
        this.extensions = new LinkedList<>();
    }

    /**
     * @see CloudEventsAttributes#id()
     */
    public CloudEventsBuilder<T> withId(final String id) {
        this.id = id;
        return this;
    }

    /**
     * @see CloudEventsAttributes#type()
     */
    public CloudEventsBuilder<T> withType(final String type) {
        this.type = type;
        return this;
    }

    /**
     * @see CloudEventsAttributes#source()
     */
    public CloudEventsBuilder<T> withSource(final String source) {
        this.source = source;
        return this;
    }

    /**
     * @see CloudEventsAttributes#source()
     */
    public CloudEventsBuilder<T> withSubject(final String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * @see CloudEventsAttributes#specVersion()
     */
    public CloudEventsBuilder<T> withSpecVersion(final String specVersion) {
        this.specVersion = specVersion;
        return this;
    }

    /**
     * @see CloudEventsAttributes#time()
     */
    public CloudEventsBuilder<T> withTime(final ZonedDateTime time) {
        this.time = time;
        return this;
    }

    /**
     * @see CloudEventsAttributes#dataContentType()
     */
    public CloudEventsBuilder<T> withDataContentType(final String dataContentType) {
        this.dataContentType = dataContentType;
        return this;
    }

    public CloudEventsBuilder<T> withData(final T data) {
        this.data = data;
        return this;
    }

    public CloudEventsBuilder<T> withExtension(final CloudEventsExtension extension) {
        this.extensions.add(extension);
        return this;
    }

    /**
     * Builds a new cloud event entity
     *
     * @return  a new {@link CloudEventsEntity} instance.
     */
    public CloudEventsEntity<T> build() {
        return new CloudEventsEntity<>(this, extensions, data);
    }
}
