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
package io.streamthoughts.azkarra.http.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.streamthoughts.azkarra.serialization.json.TimestampSerializer;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StreamsInstanceResponse {

    private final String id;

    private final long since;

    private final String name;

    private final String version;

    private final String description;

    private final State state;

    private String exception;

    public StreamsInstanceResponse(final String id,
                                   final long since,
                                   final String name,
                                   final String version,
                                   final String description,
                                   final State state,
                                   final String exception) {
        this.id = id;
        this.since = since;
        this.name = name;
        this.version = version;
        this.description = description;
        this.state = state;
        this.exception = exception;
    }

    public String getId() {
        return id;
    }

    @JsonSerialize(using = TimestampSerializer.class)
    public long getSince() {
        return since;
    }

    public State getState() {
        return state;
    }

    public String getException() {
        return exception;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class State {

        private final String state;

        private final long since;

        public State(final String state, final long since) {
            this.state = state;
            this.since = since;
        }

        public String getState() {
            return state;
        }

        @JsonSerialize(using = TimestampSerializer.class)
        public long getSince() {
            return since;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private long since;
        private String name;
        private String version;
        private String description;
        private String exception;
        private StreamsInstanceResponse.State state;

        public Builder setId(final String id) {
            this.id = id;
            return this;
        }

        public Builder setSince(final long since) {
            this.since = since;
            return this;
        }

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setVersion(final String version) {
            this.version = version;
            return this;
        }

        public Builder setDescription(final String description) {
            this.description = description;
            return this;
        }

        public Builder setState(final String state, final long since) {
            this.state = new State(state, since);
            return this;
        }

        public Builder setException(final String exception) {
            this.exception = exception;
            return this;
        }

        public StreamsInstanceResponse build() {
            return new StreamsInstanceResponse(
                id,
                since,
                name,
                version,
                description,
                state,
                exception);
        }
    }
}
