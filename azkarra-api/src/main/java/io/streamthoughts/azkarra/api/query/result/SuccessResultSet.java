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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.model.KV;

import java.io.Serializable;
import java.util.List;

public class SuccessResultSet<K, V> extends AbstractResultSet implements Serializable {

    /**
     * The total number of records returned.
     */
    private final long total;

    /**
     * The returned records.
     */
    private List<KV<K, V>> records;

    @JsonCreator
    public SuccessResultSet(@JsonProperty("server") final String server,
                            @JsonProperty("remote") final boolean remote,
                            @JsonProperty("records") final List<KV<K, V>> records) {
        super(remote, server);
        this.records = records;
        this.total = records.size();
    }

    @JsonIgnore
    public long getTotal() {
        return total;
    }

    public List<KV<K, V>> getRecords() {
        return records;
    }

}
