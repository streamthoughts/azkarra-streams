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
package io.streamthoughts.azkarra.api.query.result;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class AbstractResultSet {
    /**
     * Indicates if the query has been executed on remote instance.
     */
    private final boolean remote;
    /**
     * The server returning the records.
     */
    protected final String server;

    AbstractResultSet(@JsonProperty("remote") final boolean remote,
                      @JsonProperty("server") final String server) {
        this.remote = remote;
        this.server = server;
    }

    @JsonProperty("remote")
    public boolean isRemote() {
        return remote;
    }

    @JsonProperty("server")
    public String getServer() {
        return server;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractResultSet)) return false;
        AbstractResultSet that = (AbstractResultSet) o;
        return remote == that.remote &&
                Objects.equals(server, that.server);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(remote, server);
    }
}
