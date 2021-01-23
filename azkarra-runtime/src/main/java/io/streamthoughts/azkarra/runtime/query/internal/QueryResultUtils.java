/*
 * Copyright 2019-2021 StreamThoughts.
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
package io.streamthoughts.azkarra.runtime.query.internal;

import io.streamthoughts.azkarra.api.monad.Either;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;

import java.util.List;
import java.util.stream.Collectors;

public class QueryResultUtils {

    public static <K, V> QueryResult<K, V> buildNotAvailableResult(final String server,
                                                                   final String error) {
        final QueryResultBuilder<K, V> builder = QueryResultBuilder.newBuilder();
        return builder
                .setServer(server)
                .setStatus(QueryStatus.NOT_AVAILABLE)
                .setError(error)
                .build();
    }

    public static <K, V> QueryResult<K, V> buildQueryResult(final String localServerName,
                                                            final List<Either<SuccessResultSet<K, V>, ErrorResultSet>> results) {
        final List<ErrorResultSet> errors = results.stream()
                .filter(Either::isRight)
                .map(e -> e.right().get()).collect(Collectors.toList());

        final List<SuccessResultSet<K, V>> success = results.stream()
                .filter(Either::isLeft)
                .map(e -> e.left().get()).collect(Collectors.toList());

        final QueryStatus status = computeStatus(errors, success);

        final QueryResultBuilder<K, V> builder = QueryResultBuilder.newBuilder();
        return builder
                .setServer(localServerName)
                .setStatus(status)
                .setFailedResultSet(errors)
                .setSuccessResultSet(success)
                .build();
    }

    public static <K,V> QueryResult<K, V> buildInternalErrorResult(final String localServerName,
                                                       final String remoteServerName,
                                                       final Throwable t) {
        final QueryError error = QueryError.of(t);
        final ErrorResultSet errorResultSet = new ErrorResultSet(
                remoteServerName,
                true,
                error);

        final QueryResultBuilder<K, V> builder = QueryResultBuilder.newBuilder();
        return builder
                .setServer(localServerName)
                .setStatus(QueryStatus.ERROR)
                .setFailedResultSet(errorResultSet)
                .build();
    }


    private static <K, V> QueryStatus computeStatus(final List<ErrorResultSet> errors,
                                                    final List<SuccessResultSet<K, V>> success) {
        QueryStatus status;
        if ( !errors.isEmpty() && !success.isEmpty()) {
            status = QueryStatus.PARTIAL;
        } else if (!errors.isEmpty()) {
            status = QueryStatus.ERROR;
        } else if (!success.isEmpty()) {
            status = QueryStatus.SUCCESS;
        } else {
            status = QueryStatus.NO_RESULT;
        }
        return status;
    }
}
