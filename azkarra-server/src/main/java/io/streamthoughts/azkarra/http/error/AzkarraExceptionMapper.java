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

import io.streamthoughts.azkarra.http.data.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * The default JAX-RS {@link ExceptionMapper} implementation.
 */
public class AzkarraExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(AzkarraExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(final Exception exception) {
        final ErrorMessage errorMessage = ErrorMessage.of(exception, uriInfo.getPath());
        final int statusCode = errorMessage.getErrorCode();
        if (statusCode == 500) {
            LOG.error("Uncaught server exception in REST call to /{}", uriInfo.getPath(), exception);
        }
        return Response.status(statusCode).entity(errorMessage).build();
    }
}
