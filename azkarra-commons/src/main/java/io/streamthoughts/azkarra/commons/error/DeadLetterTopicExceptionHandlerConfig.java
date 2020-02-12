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
package io.streamthoughts.azkarra.commons.error;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Config class for {@link DeadLetterTopicExceptionHandler}.
 */
public class DeadLetterTopicExceptionHandlerConfig extends AbstractConfig {

    public static final String DEAD_LETTER_TOPIC_CONFIG = "exception.handler.dead.letter.topic";
    public static final String DEAD_LETTER_TOPIC_DOC = "The output topic to write rejected records.";

    public static final String FATAL_ERRORS_CONFIG = "exception.handler.dead.letter.fatal.errors";
    public static final String FATAL_ERRORS_DOC = "List of exception classes on which the handler must fail.";

    public static final String DEAD_LETTER_PRODUCER_CONFIG = "exception.handler.dead.letter.producer.";

    public static final String DEAD_LETTER_HEADERS_PREFIX = "exception.handler.dead.letter.headers.";

    /**
     * Creates a new {@link DeadLetterTopicExceptionHandlerConfig} instance.
     *
     * @param originals the original configs.
     */
    public DeadLetterTopicExceptionHandlerConfig(final Map<String, ?> originals) {
        super(configDef(), originals);
    }

    public String outputTopic() {
        return getString(DEAD_LETTER_TOPIC_CONFIG);
    }

    public Map<String, Object> customHeaders() {
        return originalsWithPrefix(DEAD_LETTER_HEADERS_PREFIX);
    }

    public Map<String, Object> producerConfigs() {
        return originalsWithPrefix(DEAD_LETTER_PRODUCER_CONFIG);
    }

    public List<Class<?>> getFatalExceptions() {
        List<String> classes = getList(FATAL_ERRORS_CONFIG);
        if (classes == null) return Collections.emptyList();

        List<Class<?>> fatal = new ArrayList<>(classes.size());
        for (String cls : classes) {
            try {
                fatal.add(Class.forName(cls));
            } catch (final ClassNotFoundException e) {
                throw new ConfigException("Cannot found exception class '" + cls + "' from config '"
                        + FATAL_ERRORS_CONFIG + "'");
            }
        }
        return fatal;
    }

    public static ConfigDef configDef() {
        return new ConfigDef()
             .define(DEAD_LETTER_TOPIC_CONFIG, ConfigDef.Type.STRING, null,
                 ConfigDef.Importance.HIGH, DEAD_LETTER_TOPIC_DOC)
            .define(FATAL_ERRORS_CONFIG, ConfigDef.Type.LIST, null,
                ConfigDef.Importance.HIGH, FATAL_ERRORS_DOC);
    }
}
