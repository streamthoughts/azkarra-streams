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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;


/**
 * The configuration class for {@link AutoCreateTopicsInterceptor}.
 */
public class AutoCreateTopicsInterceptorConfig {

    /**
     * {@code auto.create.topics.enable}
     */
    public static String AUTO_CREATE_TOPICS_ENABLE_CONFIG = "auto.create.topics.enable";

    /**
     * {@code auto.delete.topics.enable}
     */
    public static String AUTO_DELETE_TOPICS_ENABLE_CONFIG = "auto.delete.topics.enable";

     /**
      * {@code auto.create.topics.num.partitions}
      */
     public static String AUTO_CREATE_TOPICS_NUM_PARTITIONS_CONFIG = "auto.create.topics.num.partitions";
     public static int AUTO_CREATE_TOPICS_NUM_PARTITIONS_DEFAULT = 1;

    /**
     * {@code auto.create.topics.replication.factor}
     */
    public static String AUTO_CREATE_TOPICS_REPLICATION_FACTOR_CONFIG = "auto.create.topics.replication.factor";
    public static int AUTO_CREATE_TOPICS_REPLICATION_FACTOR_DEFAULT = 1;

    /**
     * {@code auto.create.topics.configs}
     */
    public static String AUTO_CREATE_TOPICS_CONFIGS_CONFIG = "auto.create.topics.configs";

    private final Conf originals;

    /**
     * Creates a new {@link AutoCreateTopicsInterceptorConfig} instance.
     *
     * @param originals the {@link Conf} instance.
     */
    public AutoCreateTopicsInterceptorConfig(final Conf originals) {
        this.originals = originals;
    }

    /**
     * Get the default number of partitions that should be set for creating topics.
     */
    public int getAutoCreateTopicsNumPartition() {
        return originals
                .getOptionalInt(AUTO_CREATE_TOPICS_NUM_PARTITIONS_CONFIG)
                .orElse(AUTO_CREATE_TOPICS_NUM_PARTITIONS_DEFAULT);
    }

    /**
     * Get the default replication factor that should be should be set for creating topics.
     */
    public short getAutoCreateTopicsReplicationFactor() {
        return originals
                .getOptionalInt(AUTO_CREATE_TOPICS_REPLICATION_FACTOR_CONFIG)
                .orElse(AUTO_CREATE_TOPICS_REPLICATION_FACTOR_DEFAULT).shortValue();
    }

    /**
     * Get additional properties that should be should be set for creating topics.
     */
    public Map<String, String> getAutoCreateTopicsConfigs() {
        if (!originals.hasPath(AUTO_CREATE_TOPICS_CONFIGS_CONFIG)) {
            return Collections.emptyMap();
        }
        Properties props = originals.getSubConf(AUTO_CREATE_TOPICS_CONFIGS_CONFIG).getConfAsProperties();
        return props.entrySet().stream().collect(
                Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                )
        );
    }

    /**
     * Get if topics should be automatically deleted once the streams is closed.
     */
    public boolean isAutoDeleteTopicsEnable() {
        return originals.getOptionalBoolean(AUTO_DELETE_TOPICS_ENABLE_CONFIG).orElse(false);
    }

    public Conf originals() {
        return originals;
    }

}
