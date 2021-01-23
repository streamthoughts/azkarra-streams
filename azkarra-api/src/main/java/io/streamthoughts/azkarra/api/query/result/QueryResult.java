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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

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

    /**
     * Sets the server information.
     *
     * @param server    the server to set.
     * @return          a new {@link QueryResult} instance.
     */
    public QueryResult<K, V>  server(final String server) {
        return new QueryResult<>(
                took,
                timeout,
                server,
                status,
                result
        );
    }
    /**
     * Sets the took information.
     *
     * @param took    the query took millisecond.
     * @return        a new {@link QueryResult} instance.
     */
    public  QueryResult<K, V> took(final long took) {
        return new QueryResult<>(
                took,
                timeout,
                server,
                status,
                result
        );
    }
    /**
     * Sets the timeout information.
     *
     * @param timeout   is the query timeout.
     * @return          a new {@link QueryResult} instance.
     */
    public QueryResult<K, V> timeout(final boolean timeout) {
        return new QueryResult<>(
                took,
                timeout,
                server,
                status,
                result
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryResult)) return false;
        QueryResult<?, ?> that = (QueryResult<?, ?>) o;
        return took == that.took &&
                timeout == that.timeout &&
                Objects.equals(server, that.server) &&
                status == that.status &&
                Objects.equals(result, that.result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(took, timeout, server, status, result);
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "took=" + took +
                ", timeout=" + timeout +
                ", server='" + server + '\'' +
                ", status=" + status +
                ", result=" + result +
                '}';
    }
}