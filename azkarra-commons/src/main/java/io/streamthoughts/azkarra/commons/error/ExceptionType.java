/*
 * Copyright 2021 StreamThoughts.
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
package io.streamthoughts.azkarra.commons.error;

/**
 * The types of exceptions that may be thrown by a stream application.
 */
public enum ExceptionType {

    /**
     * An exception thrown during record deserialization.
     *
     * @see org.apache.kafka.streams.errors.DeserializationExceptionHandler
     */
    DESERIALIZATION,

    /**
     * An exception thrown during record production.
     *
     * @see org.apache.kafka.streams.errors.ProductionExceptionHandler
     */
    PRODUCTION,

    /**
     * An exception thrown during record processing.
     */
    PROCESSING,

    /**
     * An uncaught exception thrown by a stream thread.
     *
     * @see org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
     */
    STREAM,
}
