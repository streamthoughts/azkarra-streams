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
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ErrorResultSet extends AbstractResultSet {

    private final List<QueryError> errors;

    public ErrorResultSet(final String server,
                          final boolean remote,
                          final QueryError error) {
        this(server, remote, Collections.singletonList(error));
    }

    @JsonCreator
    public ErrorResultSet(@JsonProperty("server") final String server,
                          @JsonProperty("remote") final boolean remote,
                          @JsonProperty("errors") final List<QueryError> errors) {
        super(remote, server);
        this.errors = errors;
    }

    public List<QueryError> getErrors() {
        return errors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorResultSet)) return false;
        if (!super.equals(o)) return false;
        ErrorResultSet that = (ErrorResultSet) o;
        return Objects.equals(errors, that.errors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), errors);
    }
}
