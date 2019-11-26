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
package io.streamthoughts.azkarra.http.error;

import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.errors.NotFoundException;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.data.ErrorMessage;
import io.streamthoughts.azkarra.http.security.UnauthorizedAccessException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.undertow.util.StatusCodes.BAD_REQUEST;
import static io.undertow.util.StatusCodes.INTERNAL_SERVER_ERROR;
import static io.undertow.util.StatusCodes.NOT_FOUND;
import static io.undertow.util.StatusCodes.UNAUTHORIZED;

/**
 * An {@link HttpHandler} which used for catching any exception thrown during request execution.
 */
public class ExceptionDefaultHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionDefaultHandler.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) {
        final Throwable throwable = exchange.getAttachment(ExceptionHandler.THROWABLE);
        sendErrorMessage(exchange, throwable);
    }

    public static void sendErrorMessage(final HttpServerExchange exchange, final Throwable throwable) {
        final String exceptionMessage = throwable.getMessage();
        final String exception = throwable.getClass().getName();
        final String path = exchange.getRelativePath();
        ErrorMessage error;
        if (throwable instanceof NotFoundException) {
            error = new ErrorMessage(NOT_FOUND, exceptionMessage, exception, path);
        } else if (throwable instanceof MetricNotFoundException) {
            error = new ErrorMessage(NOT_FOUND, exceptionMessage, exception, path);
        } else if (throwable instanceof BadRequestException) {
            error = new ErrorMessage(BAD_REQUEST, exceptionMessage, exception, path);
        } else if (throwable instanceof SerializationException) {
            error = new ErrorMessage(BAD_REQUEST, exceptionMessage, exception, path);
        } else if (throwable instanceof UnauthorizedAccessException) {
            error = new ErrorMessage(UNAUTHORIZED, exceptionMessage, exception, path);
        } else if (throwable instanceof AzkarraException) {
            error = internalServerError("Internal Azkarra Streams API Error : "
                    + exceptionMessage, exception, path);
        } else {
            error = internalServerError("Unexpected internal server error: "
                    + exceptionMessage, exception, path);
        }
        if (error.getErrorCode() == 500) {
            LOG.error("Uncaught server exception", throwable);
        }
        ExchangeHelper.sendJsonResponseWithCode(exchange, error, error.getErrorCode());
    }

    private static ErrorMessage internalServerError(final String message,
                                                    final String exception,
                                                    final String path) {
        return new ErrorMessage(INTERNAL_SERVER_ERROR, message, exception, path);
    }
}
