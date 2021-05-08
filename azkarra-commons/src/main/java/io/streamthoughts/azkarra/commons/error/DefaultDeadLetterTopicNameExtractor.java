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

import java.util.Map;
import java.util.Optional;

import static io.streamthoughts.azkarra.commons.error.DeadLetterTopicExceptionHandlerConfig.DLQ_DEFAULT_PREFIX_CONFIG;

public class DefaultDeadLetterTopicNameExtractor implements DeadLetterTopicNameExtractor {

    public static final String DEFAULT_SUFFIX = "-rejected";

    private DefaultDeadLetterTopicNameExtractorConfig config;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Map<String, ?> configs) {
        config = new DefaultDeadLetterTopicNameExtractorConfig(configs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String extract(final byte[] key,
                          final byte[] value,
                          final FailedRecordContext recordContext) {
        return recordContext.topic() + config.getSuffix();
    }

    private static class DefaultDeadLetterTopicNameExtractorConfig extends AbstractConfig {

        private static final String GROUP = "Default Dead Letter Topic Name";

        public static final String DLQ_DEFAULT_TOPIC_SUFFIX_CONFIG = DLQ_DEFAULT_PREFIX_CONFIG + "topic.suffix";
        public static final String DLQ_DEFAULT_TOPIC_SUFFIX_DOC = "The suffix to be add to source or sink record topic"
                + " for computing the Dead Letter Topic name (default: '-rejected').";

        public static final String DLQ_DEFAULT_TOPIC_NAME_CONFIG = DLQ_DEFAULT_PREFIX_CONFIG + "topic.name";
        private static final String DLQ_DEFAULT_TOPIC_NAME_DOC = "The output topic name to write rejected records.";

        /**
         * Creates a new {@link DefaultDeadLetterTopicNameExtractorConfig} instance.
         *
         * @param originals the originals config.
         */
        public DefaultDeadLetterTopicNameExtractorConfig(final Map<?, ?> originals) {
            super(configDef(), originals, false);
        }

        public String getSuffix() {
            return getString(DLQ_DEFAULT_TOPIC_SUFFIX_CONFIG);
        }

        public Optional<String> getTopic() {
            return Optional.ofNullable(getString(DLQ_DEFAULT_TOPIC_NAME_CONFIG));
        }

        public static ConfigDef configDef() {
            return new ConfigDef()
                    .define(
                            DLQ_DEFAULT_TOPIC_SUFFIX_CONFIG,
                            ConfigDef.Type.STRING,
                            DEFAULT_SUFFIX,
                            ConfigDef.Importance.HIGH,
                            DLQ_DEFAULT_TOPIC_SUFFIX_DOC,
                            GROUP,
                            0,
                            ConfigDef.Width.NONE,
                            DLQ_DEFAULT_TOPIC_SUFFIX_CONFIG
                    )
                    .define(
                            DLQ_DEFAULT_TOPIC_NAME_CONFIG,
                            ConfigDef.Type.STRING,
                            null,
                            ConfigDef.Importance.HIGH,
                            DLQ_DEFAULT_TOPIC_NAME_DOC,
                            GROUP,
                            1,
                            ConfigDef.Width.NONE,
                            DLQ_DEFAULT_TOPIC_NAME_DOC
                    );
        }
    }
}
