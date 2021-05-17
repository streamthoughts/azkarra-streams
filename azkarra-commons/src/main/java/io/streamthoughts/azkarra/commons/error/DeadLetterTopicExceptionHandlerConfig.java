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

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class for configuring exception handler that support Dead Letter Topic.
 *
 * @see DeadLetterTopicProductionExceptionHandler
 * @see DeadLetterTopicDeserializationExceptionHandler
 */
public class DeadLetterTopicExceptionHandlerConfig extends AbstractConfig {

    private static final String DLQ_GROUP = "Dead Letter Topic";

    public static final String DLQ_DEFAULT_PREFIX_CONFIG = "exception.handler.dlq.default.";

    public static final String DLQ_FAIL_ERRORS_CONFIG = "fail.errors";
    public static final String DLQ_CONTINUE_CONFIG = "continue.errors";
    public static final String DLQ_HEADERS_PREFIX_CONFIG = "headers.";
    public static final String DLQ_RESPONSE_CONFIG = "response";

    public static final String DLQ_TOPIC_NAME_EXTRACTOR_CONFIG = "topic.extractor";

    public static final String DLQ_DEFAULT_TOPIC_NAME_EXTRACTOR_CONFIG = DLQ_DEFAULT_PREFIX_CONFIG + "topic.extractor";
    public static final String DLQ_DEFAULT_TOPIC_NAME_EXTRACTOR_DOC = "topic.extractor";

    public static final String DLQ_DEFAULT_RESPONSE_CONFIG
            = DLQ_DEFAULT_PREFIX_CONFIG + DLQ_RESPONSE_CONFIG;
    private static final String DLQ_DEFAULT_RESPONSE_DOC =
            "The default response that must be return by an handler [FAIL|CONTINUE]";

    public static final String DLQ_DEFAULT_FAIL_ERRORS_CONFIG
            = DLQ_DEFAULT_PREFIX_CONFIG + DLQ_FAIL_ERRORS_CONFIG;
    private static final String DLQ_DEFAULT_FATAL_ERRORS_DOC
            = "List of exception classes on which the handler must fail.";

    public static final String DLQ_DEFAULT_CONTINUE_ERRORS_CONFIG
            = DLQ_DEFAULT_PREFIX_CONFIG + DLQ_CONTINUE_CONFIG;
    private static final String DLQ_DEFAULT_CONTINUE_ERRORS_DOC
            = "List of exception classes on which the handler must continue.";

    public static final String DLQ_DEFAULT_HEADERS_PREFIX_CONFIG =
            DLQ_DEFAULT_PREFIX_CONFIG + DLQ_HEADERS_PREFIX_CONFIG;

    public static final String DLQ_PRODUCTION_PREFIX_CONFIG =
            "exception.handler.dlq.production.";

    public static final String DLQ_DESERIALIZATION_PREFIX_CONFIG
            = "exception.handler.dlq.deserialization.";

    public static final String EMPTY_PREFIX = "";

    private final EnrichedExceptionHandlerConfig overriddenConfig;

    private final ExceptionType exceptionType;
    private final Set<Class<?>> continueOnExceptions = new HashSet<>();
    private final Set<Class<?>> failOnExceptions = new HashSet<>();

    /**
     * Creates a new {@link DeadLetterTopicExceptionHandlerConfig} instance.
     *
     * @param originals the original configs.
     */
    public DeadLetterTopicExceptionHandlerConfig(final Map<String, ?> originals,
                                                 final ExceptionType exceptionType) {
        super(configDef(DLQ_DEFAULT_PREFIX_CONFIG, DLQ_GROUP), originals);

        final String configPrefix = getConfigPrefix(exceptionType);
        overriddenConfig = new EnrichedExceptionHandlerConfig(
                configDef(EMPTY_PREFIX, exceptionType.name()),
                this.originalsWithPrefix(configPrefix)
        );

        this.exceptionType = exceptionType;
        validate();
    }

    private void validate() {
        failOnExceptions.addAll(
                parseExceptionClasses(
                        getList(DLQ_DEFAULT_FAIL_ERRORS_CONFIG), DLQ_DEFAULT_FAIL_ERRORS_CONFIG)
        );

        failOnExceptions.addAll(
                parseExceptionClasses(
                        overriddenConfig.getList(DLQ_FAIL_ERRORS_CONFIG),
                        getConfigPrefix(exceptionType) + DLQ_FAIL_ERRORS_CONFIG)
        );

        continueOnExceptions.addAll(
                parseExceptionClasses(
                        getList(DLQ_DEFAULT_CONTINUE_ERRORS_CONFIG), DLQ_DEFAULT_CONTINUE_ERRORS_CONFIG)
        );

        continueOnExceptions.addAll(
                parseExceptionClasses(
                        overriddenConfig.getList(DLQ_CONTINUE_CONFIG),
                        getConfigPrefix(exceptionType) + DLQ_FAIL_ERRORS_CONFIG)
        );

        final Set<Class<?>> intersection = new HashSet<>(failOnExceptions);
        intersection.retainAll(continueOnExceptions);
        if (!intersection.isEmpty()) {
            throw new ConfigException(
                    "Some classes are configured as both fatal and continue errors: {}", intersection);
        }
    }

    private static String getConfigPrefix(final ExceptionType exceptionType) {
        String prefix = null;
        if (exceptionType == ExceptionType.PRODUCTION)
            prefix = DLQ_PRODUCTION_PREFIX_CONFIG;
        if (exceptionType == ExceptionType.DESERIALIZATION)
            prefix = DLQ_DESERIALIZATION_PREFIX_CONFIG;
        return prefix;
    }

    public DeadLetterTopicNameExtractor topicNameExtractor() {
        DeadLetterTopicNameExtractor extractor = overriddenConfig.getConfiguredInstance(
                DLQ_TOPIC_NAME_EXTRACTOR_CONFIG,
                DeadLetterTopicNameExtractor.class
        );
        if (extractor != null) return extractor;

        return getConfiguredInstance(
                DLQ_DEFAULT_TOPIC_NAME_EXTRACTOR_CONFIG,
                DeadLetterTopicNameExtractor.class
        );
    }

    public List<Header> customHeaders() {
        Map<String, Object> headers = originalsWithPrefix(DLQ_DEFAULT_HEADERS_PREFIX_CONFIG);
        headers.putAll(overriddenConfig.originalsWithPrefix(DLQ_HEADERS_PREFIX_CONFIG));

        return headers.entrySet().stream()
                .map(e -> new RecordHeader(e.getKey(), e.getValue().toString().getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.toList());
    }

    public ExceptionHandlerResponse defaultHandlerResponseOrElse(final ExceptionHandlerResponse defaultResponse) {
        return Optional.ofNullable(overriddenConfig.getString(DLQ_RESPONSE_CONFIG))
                .or(() -> Optional.ofNullable(getString(DLQ_DEFAULT_RESPONSE_CONFIG)))
                .map(it -> ExceptionHandlerResponse.valueOf(it.toUpperCase()))
                .orElse(defaultResponse);
    }

    public Set<Class<?>> getFatalExceptions() {
        return failOnExceptions;
    }

    public Set<Class<?>> getIgnoredExceptions() {
        return continueOnExceptions;
    }

    private Set<Class<?>> parseExceptionClasses(final List<String> classes, final String configKey) {
        if (classes == null) return Collections.emptySet();

        Set<Class<?>> res = new HashSet<>();
        for (String cls : classes) {
            try {
                res.add(Class.forName(cls));
            } catch (final ClassNotFoundException e) {
                throw new ConfigException(
                        "Cannot found exception class '" + cls + "' from config '" + configKey + "'");
            }
        }
        return res;
    }

    public static ConfigDef configDef(final String prefix, final String group) {
        int orderInGroup = 0;
        return new ConfigDef()
                .define(
                        prefix + DLQ_FAIL_ERRORS_CONFIG,
                        ConfigDef.Type.LIST,
                        null,
                        ConfigDef.Importance.HIGH,
                        DLQ_DEFAULT_FATAL_ERRORS_DOC,
                        group,
                        orderInGroup++,
                        ConfigDef.Width.NONE,
                        prefix + DLQ_FAIL_ERRORS_CONFIG
                )
                .define(
                        prefix + DLQ_CONTINUE_CONFIG,
                        ConfigDef.Type.LIST,
                        null,
                        ConfigDef.Importance.HIGH,
                        DLQ_DEFAULT_CONTINUE_ERRORS_DOC,
                        group,
                        orderInGroup++,
                        ConfigDef.Width.NONE,
                        prefix + DLQ_CONTINUE_CONFIG
                )
                .define(
                        prefix + DLQ_RESPONSE_CONFIG,
                        ConfigDef.Type.STRING,
                        null,
                        ConfigDef.Importance.HIGH,
                        DLQ_DEFAULT_RESPONSE_DOC,
                        group,
                        orderInGroup++,
                        ConfigDef.Width.NONE,
                        prefix + DLQ_RESPONSE_CONFIG
                )
                .define(
                        prefix + DLQ_TOPIC_NAME_EXTRACTOR_CONFIG,
                        ConfigDef.Type.CLASS,
                        DefaultDeadLetterTopicNameExtractor.class,
                        ConfigDef.Importance.HIGH,
                        DLQ_DEFAULT_TOPIC_NAME_EXTRACTOR_DOC,
                        group,
                        orderInGroup++,
                        ConfigDef.Width.NONE,
                        prefix + DLQ_TOPIC_NAME_EXTRACTOR_CONFIG
                );
    }

    public static String prefixForProductionHandler(final String configKey) {
        return getConfigPrefix(ExceptionType.PRODUCTION) + configKey;
    }

    public static String prefixForDeserializationHandler(final String configKey) {
        return getConfigPrefix(ExceptionType.DESERIALIZATION) + configKey;
    }

    private static class EnrichedExceptionHandlerConfig extends AbstractConfig {

        public EnrichedExceptionHandlerConfig(final ConfigDef definition, final Map<?, ?> originals) {
            super(definition, originals);
        }
    }
}