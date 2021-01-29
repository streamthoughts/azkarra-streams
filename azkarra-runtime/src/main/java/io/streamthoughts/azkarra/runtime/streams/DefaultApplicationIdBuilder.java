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
package io.streamthoughts.azkarra.runtime.streams;

import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentAware;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.ApplicationId;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.api.util.Utils;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import org.apache.kafka.streams.StreamsConfig;

import static java.lang.Character.isUpperCase;

/**
 * The default {@link ApplicationIdBuilder} implementation.
 */
public class DefaultApplicationIdBuilder implements ApplicationIdBuilder, StreamsExecutionEnvironmentAware {

    private static final String CHAR_SEPARATOR = "-";
    private static final char[] AUTHORIZED_CHAR_SEPARATOR = {' ', '-', '_', '.'};
    private static final String INTERNAL_ENV_NAME_PREFIX = "__";

    private StreamsExecutionEnvironment environment;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExecutionEnvironment(final StreamsExecutionEnvironment environment) {
        this.environment = environment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId buildApplicationId(final TopologyMetadata metadata, final Conf streamsConfig) {
        var id = streamsConfig.getOptionalString("streams." + StreamsConfig.APPLICATION_ID_CONFIG);
        return id.map(ApplicationId::new).orElseGet(() -> new ApplicationId(normalize(build(metadata, environment))));
    }

    private String build(final TopologyMetadata metadata,
                         final StreamsExecutionEnvironment<?> environment) {
        StringBuilder sb = new StringBuilder();
        final String name = environment.name();
        if (isNotDefaultOrInternalEnvironmentName(name)) {
            sb.append(name).append(CHAR_SEPARATOR);
        }
        return normalize(sb.append(metadata.name()).append(CHAR_SEPARATOR).append(metadata.version()).toString());
    }

    private boolean isNotDefaultOrInternalEnvironmentName(String name) {
        return !(name.startsWith(INTERNAL_ENV_NAME_PREFIX) || name.equals(DefaultAzkarraContext.DEFAULT_ENV_NAME));
    }

    private static String normalize(final String name) {

        StringBuilder sb = new StringBuilder();
        char[] chars = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (Utils.contains(AUTHORIZED_CHAR_SEPARATOR, c)) {
                sb.append(CHAR_SEPARATOR);
                continue;
            }
            sb.append(c);

            if (i + 1 < chars.length && isUpperCase(chars[i+1])) {
                sb.append(CHAR_SEPARATOR);
            }
        }
        return sb.toString().toLowerCase();
    }
}
