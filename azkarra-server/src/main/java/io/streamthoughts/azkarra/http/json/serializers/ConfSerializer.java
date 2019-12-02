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
package io.streamthoughts.azkarra.http.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.streamthoughts.azkarra.api.config.Conf;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.streams.StreamsConfig;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.kafka.streams.StreamsConfig.configDef;

/**
 * Json serializer for {@link Conf}.
 */
public class ConfSerializer extends JsonSerializer<Conf> {

    private static final String HIDDEN = "[hidden]";

    private static final Set<String> SECRET_STREAMS_KEYS = new HashSet<>();
    private static final String PASSWORD_SUFFIX_KEY = ".password";

    static {
        ConfigDef configDef = new ConfigDef(configDef());
        SslConfigs.addClientSslSupport(configDef);
        SaslConfigs.addClientSaslSupport(configDef);
        SECRET_STREAMS_KEYS.addAll(keepOnlySecretKeys(configDef.configKeys()));

        // duplicate all secrets keys for known prefixes
        Set<String> withConsumerPrefix = allPrefixedWith(SECRET_STREAMS_KEYS, StreamsConfig::consumerPrefix);
        Set<String> witProducerPrefix = allPrefixedWith(SECRET_STREAMS_KEYS, StreamsConfig::producerPrefix);
        Set<String> withAdminClientPrefix = allPrefixedWith(SECRET_STREAMS_KEYS, StreamsConfig::adminClientPrefix);

        SECRET_STREAMS_KEYS.addAll(withConsumerPrefix);
        SECRET_STREAMS_KEYS.addAll(witProducerPrefix);
        SECRET_STREAMS_KEYS.addAll(withAdminClientPrefix);
    }

    private static Set<String> allPrefixedWith(final Set<String> keys,
                                               final Function<String, String> prefix) {
        return keys.stream().map(prefix).collect(Collectors.toSet());
    }

    private static Set<String> keepOnlySecretKeys(final Map<String, ConfigDef.ConfigKey> keys) {
        return keys.entrySet().stream()
            .filter(e -> e.getValue().type().equals(ConfigDef.Type.PASSWORD))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final Conf value,
                          final JsonGenerator gen,
                          final SerializerProvider serializers) throws IOException {

        gen.writeObject(new TreeMap<>(obfuscateSecretConfig(value.getConfAsMap())));
    }

    private Map<String, Object> obfuscateSecretConfig(final Map<String, Object> config) {
        return config.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> mayObfuscateSecret(e.getKey(), e.getValue())));
    }

    /**
     * Obfuscates the config value if the key references a config of type {@link ConfigDef.Type#PASSWORD}.
     *
     * @param key       the key of the config.
     * @param value     the value of the config.
     *
     * @return          the obfuscated value if it's secret, the original value otherwise.
     */
    private Object mayObfuscateSecret(final String key, final Object value) {
        if (SECRET_STREAMS_KEYS.contains(key))
            return HIDDEN;

        if (key.startsWith("streams.") && SECRET_STREAMS_KEYS.contains(key.substring("streams.".length())))
            return HIDDEN;

        // as a general rule, we obfuscate any key that end with .password.
        if (key.endsWith(PASSWORD_SUFFIX_KEY))
            return HIDDEN;

        return value;
    }
}
