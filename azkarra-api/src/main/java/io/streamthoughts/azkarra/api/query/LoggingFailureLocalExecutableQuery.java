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
package io.streamthoughts.azkarra.api.query;

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.monad.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LoggingFailureLocalExecutableQuery<K, V>
        extends DecorateQuery<LocalExecutableQuery<K, V>>
        implements LocalExecutableQuery<K, V> {

    private final Logger LOG = LoggerFactory.getLogger(LoggingFailureLocalExecutableQuery.class);

    /**
     * Creates a new {@link LoggingFailureLocalExecutableQuery} instance.
     *
     * @param query the {@link LocalExecutableQuery}.
     */
    public LoggingFailureLocalExecutableQuery(final LocalExecutableQuery<K, V> query) {
        super(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<List<KV<K, V>>> execute(LocalStoreAccessProvider provider, long limit) {
        return logFailure(Try.success(query).flatMap(q -> q.execute(provider, limit)));
    }

    private Try<List<KV<K, V>>> logFailure(final Try<List<KV<K, V>>> executed) {
        if (executed.isFailure()) {
            LOG.error("Error happens while executing query '{}' on state store '{}' with params '{}': {}",
                getStoreOperation(),
                getStoreName(),
                getParams(),
                executed.getThrowable().getMessage());
        }
        return executed;
    }
}
