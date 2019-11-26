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
package io.streamthoughts.azkarra.api.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for setting internal RocksDB options.
 */
public class RocksDBConfig {

    public static String ROCKS_DB_MAX_WRITE_BUFFER_NUMBER_CONFIG = "rocksdb.max.write.buffer.number";
    public static String ROCKS_DB_WRITE_BUFFER_SIZE_CONFIG       = "rocksdb.write.buffer.size";
    public static String ROCKS_DB_STATS_ENABLE_CONFIG            = "rocksdb.stats.enable";
    public static String ROCKS_DB_STATS_DUMP_PERIOD_SEC_CONFIG   = "rocksdb.stats.dump.period.sec";
    public static String ROCKS_DB_LOG_DIR_CONFIG                 = "rocksdb.log.dir";
    public static String ROCKS_DB_LOG_LEVEL_CONFIG               = "rocksdb.log.level";
    public static String ROCKS_DB_LOG_MAX_FILE_SIZE_CONFIG       = "rocksdb.log.max.file.size";

    private Map<String, String> configs = new HashMap<>();

    public static RocksDBConfig withStatsEnable() {
        return new RocksDBConfig(true);
    }

    public static RocksDBConfig withStatsDisable() {
        return new RocksDBConfig(false);
    }

    /**
     * Creates a new {@link RocksDBConfig} instance.
     */
    private RocksDBConfig(boolean enableStats) {
        configs.put(ROCKS_DB_STATS_ENABLE_CONFIG, String.valueOf(enableStats));
    }

    public RocksDBConfig withBufferSize(final int writeBufferSize) {
        configs.put(ROCKS_DB_WRITE_BUFFER_SIZE_CONFIG, String.valueOf(writeBufferSize));
        return this;
    }

    public RocksDBConfig withMaxWriteBufferNumber(final int maxWriteBufferNumber) {
        configs.put(ROCKS_DB_MAX_WRITE_BUFFER_NUMBER_CONFIG, String.valueOf(maxWriteBufferNumber));
        return this;
    }

    public RocksDBConfig withStatsDumpPeriod(final Duration duration) {
        configs.put(ROCKS_DB_STATS_DUMP_PERIOD_SEC_CONFIG, String.valueOf(duration.toSeconds()));
        return this;
    }

    public RocksDBConfig withLogDir(final String logDir) {
        configs.put(ROCKS_DB_LOG_DIR_CONFIG, logDir);
        return this;
    }

    public RocksDBConfig withLogLevel(final String level) {
        configs.put(ROCKS_DB_LOG_LEVEL_CONFIG, level);
        return this;
    }

    public RocksDBConfig withLogMaxFileSize(final int logMaxFileSize) {
        configs.put(ROCKS_DB_LOG_MAX_FILE_SIZE_CONFIG, String.valueOf(logMaxFileSize));
        return this;
    }

    public Conf conf() {
        return new MapConf(configs);
    }

    @Override
    public String toString() {
        return configs.toString();
    }
}
