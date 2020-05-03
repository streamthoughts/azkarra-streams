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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsStateException;
import io.streamthoughts.azkarra.api.errors.NotFoundException;
import io.streamthoughts.azkarra.http.error.BadRequestException;
import io.streamthoughts.azkarra.http.error.MetricNotFoundException;
import io.streamthoughts.azkarra.http.security.UnauthorizedAccessException;
import io.streamthoughts.azkarra.serialization.SerializationException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.Serializable;

/**
 * The error message response object.
 */
public class ErrorMessage implements Serializable {

    private final int errorCode;

    private final String message;

    private final String exception;

    private final String path;

    /**
     * Creates a new {@link ErrorMessage} instance.
     *
     * @param errorCode the error code.
     * @param exception the error exception cause.
     * @param path      the uri relative path.
     */
    public ErrorMessage(final int errorCode,
                        final Throwable exception,
                        final String path) {
        this(errorCode, exception.getMessage(), exception.getClass().getName(), path);
    }


    /**
     * Creates a new {@link ErrorMessage} instance.
     *
     * @param errorCode the error code.
     * @param message   the error message.
     * @param exception the error exception cause.
     * @param path      the uri relative path.
     */
    public ErrorMessage(final int errorCode,
                        final String message,
                        final Throwable exception,
                        final String path) {
        this(errorCode, message, exception.getClass().getName(), path);
    }

    /**
     * Creates a new {@link ErrorMessage} instance.
     *
     * @param errorCode the error code.
     * @param message   the error message.
     * @param path      the uri relative path.
     */
    public ErrorMessage(final int errorCode,
                        final String message,
                        final String path) {
        this(errorCode, message, (String)null, path);
    }

    /**
     * Creates a new {@link ErrorMessage} instance.
     *
     * @param errorCode the error code.
     * @param message   the error message.
     * @param exception the error exception cause.
     * @param path      the uri relative path.
     */
    public ErrorMessage(final int errorCode,
                        final String message,
                        final String exception,
                        final String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.exception = exception;
        this.path = path;
    }

    @JsonProperty(value = "error_code")
    public int getErrorCode() {
        return errorCode;
    }

    @JsonProperty(value = "message")
    public String getMessage() {
        return message;
    }

    @JsonProperty(value = "exception")
    public String getException() {
        return exception;
    }

    @JsonProperty(value = "path")
    public String getPath() {
        return path;
    }

    /**
     * Static helper method for mapping an given exception to a new {@link ErrorMessage}.
     *
     * @param exception the exception.
     * @param path      the rest call path.
     *
     * @return  the {@link ErrorMessage}; cannot return {@code null}.
     */
    public static ErrorMessage of(final Throwable exception, final String path) {

        if (exception instanceof NotFoundException) {
           return new ErrorMessage(Status.NOT_FOUND.getStatusCode(), exception, path);
        }

        else if (exception instanceof MetricNotFoundException) {
            return new ErrorMessage(Status.NOT_FOUND.getStatusCode(), exception, path);
        }

        else if (exception instanceof BadRequestException) {
            return new ErrorMessage(Status.BAD_REQUEST.getStatusCode(), exception, path);
        }

        else if (exception instanceof SerializationException) {
            return new ErrorMessage(Status.BAD_REQUEST.getStatusCode(), exception, path);
        }

        else if (exception instanceof UnauthorizedAccessException) {
            return new ErrorMessage(Status.UNAUTHORIZED.getStatusCode(), exception, path);
        }

        else if (exception instanceof InvalidStreamsStateException) {
            return new ErrorMessage(Status.SERVICE_UNAVAILABLE.getStatusCode(), exception, path);
        }

        else if (exception instanceof WebApplicationException) {
            Response.StatusType statusInfo = ((WebApplicationException) exception).getResponse().getStatusInfo();
            return new ErrorMessage(statusInfo.getStatusCode(), exception, path);
        }

        else if (exception instanceof AzkarraException) {
            return new ErrorMessage(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Azkarra Streams API Error: " + exception.getMessage(),
                exception,
                path
            );
        }

        return new ErrorMessage(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "Uncaught internal server error: " + exception.getMessage(),
            exception,
            path
        );
    }
}
