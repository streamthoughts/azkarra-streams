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
package io.streamthoughts.azkarra.api.query.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueryResult<K, V> implements Serializable {

    /**
     *  Time in milliseconds to execute the query.
     */
    private final long took;

    /**
     * Is the query has timeout.
     */
    private final boolean timeout;

    /**
     * The server which initially executed the query.
     */
    private final String server;

    /**
     * The query result status.
     */
    private final QueryStatus status;

    /**
     * The query result.
     */
    private final GlobalResultSet<K, V> result;

    /**
     * Creates a new {@link QueryResult} instance.
     */
    @JsonCreator
    QueryResult(@JsonProperty("took") final long took,
                @JsonProperty("timeout")final boolean timeout,
                @JsonProperty("server")final String server,
                @JsonProperty("status")final QueryStatus status,
                @JsonProperty("result")final GlobalResultSet<K, V> result) {
        this.took = took;
        this.timeout = timeout;
        this.server = server;
        this.status = status;
        this.result = result;
    }

    public long getTook() {
        return took;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public String getServer() {
        return server;
    }

    public QueryStatus getStatus() {
        return status;
    }

    public GlobalResultSet<K, V> getResult() {
        return result;
    }
}