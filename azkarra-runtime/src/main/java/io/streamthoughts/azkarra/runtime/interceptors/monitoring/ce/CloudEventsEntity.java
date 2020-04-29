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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CloudEventsEntity<T> {

      private final CloudEventsAttributes attributes;

      private final List<CloudEventsExtension> extensions;

      private final T data;

      /**
       * Creates a new {@link CloudEventsEntity} instance.
       *
       * @param attributes    the context event attributes.
       * @param extensions    the extension attributes.
       * @param data          the event data.
       */
      CloudEventsEntity(final CloudEventsAttributes attributes,
                        final List<CloudEventsExtension> extensions,
                        final T data) {
            this.attributes = Objects.requireNonNull(attributes, "attributes cannot be null");
            this.extensions = Objects.requireNonNull(extensions, "extensions cannot be null");
            this.data = Objects.requireNonNull(data, "data cannot be null");
      }

      @JsonUnwrapped
      @JsonProperty
      public CloudEventsAttributes attributes() {
            return attributes;
      }

      @JsonAnyGetter
      @JsonProperty
      public Map<String, Object> extensions() {
            return CloudEventsExtension.marshal(extensions);
      }

      @JsonProperty
      public T data() {
            return data;
      }

      @Override
      public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CloudEventsEntity)) return false;
            CloudEventsEntity<?> that = (CloudEventsEntity<?>) o;
            return Objects.equals(attributes, that.attributes) &&
                    Objects.equals(extensions, that.extensions) &&
                    Objects.equals(data, that.data);
      }

      @Override
      public int hashCode() {
            return Objects.hash(attributes, extensions, data);
      }

      @Override
      public String toString() {
            return "CloudEventsEntity{" +
                    "attributes=" + attributes +
                    ", extensions=" + extensions +
                    ", data=" + data +
                    '}';
      }
}
