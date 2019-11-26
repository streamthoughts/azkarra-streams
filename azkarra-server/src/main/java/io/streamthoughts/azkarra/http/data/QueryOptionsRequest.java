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
package io.streamthoughts.azkarra.http.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class which is used for serializing and de-serializing state store query options.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueryOptionsRequest {

    private final Integer retries;

    private final Long retryBackoff;

    private final Long queryTimeout;

    private final Boolean remoteAccessAllowed;

    @JsonCreator
    public QueryOptionsRequest(@JsonProperty("retries") final Integer retries,
                               @JsonProperty("retry_backoff_ms") final Long retryBackoff,
                               @JsonProperty("query_timeout_ms") final Long queryTimeout,
                               @JsonProperty("remote_access_allowed") final Boolean remoteAccessAllowed) {
        this.retries = retries;
        this.retryBackoff = retryBackoff;
        this.queryTimeout = queryTimeout;
        this.remoteAccessAllowed = remoteAccessAllowed;
    }

    @JsonProperty("retries")
    public Integer getRetries() {
        return retries;
    }

    @JsonProperty("retry_backoff_ms")
    public Long getRetryBackoff() {
        return retryBackoff;
    }

    @JsonProperty("query_timeout_ms")
    public Long getQueryTimeout() {
        return queryTimeout;
    }

    @JsonProperty("remote_access_allowed")
    public Boolean isRemoteAccessAllowed() {
        return remoteAccessAllowed;
    }
}
