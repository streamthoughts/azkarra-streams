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
package io.streamthoughts.azkarra.api.streams.rocksdb;

import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.rocksdb.InfoLogLevel;
import org.rocksdb.Options;
import org.rocksdb.Statistics;
import org.rocksdb.StatsLevel;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class DefaultRocksDBConfigSetter implements RocksDBConfigSetter {

    private Statistics statistics;

    private DefaultRocksDBConfigSetterConfig setterConfig;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig(final String storeName,
                          final Options options,
                          final Map<String, Object> configs) {

        setterConfig = new DefaultRocksDBConfigSetterConfig(configs);

        if (setterConfig.isStatisticsEnable()) {
            statistics = new Statistics();
            statistics.setStatsLevel(StatsLevel.ALL);
            options.setStatistics(statistics);
            options.setStatsDumpPeriodSec(setterConfig.dumpPeriodSec());

            if (setterConfig.maxLogFileSize() != null) {
                options.setMaxLogFileSize(setterConfig.maxLogFileSize());
            }

            if (setterConfig.logDir() != null) {
                options.setDbLogDir(setterConfig.logDir());
                final InfoLogLevel level = setterConfig.logLevel()
                    .map(s -> InfoLogLevel.valueOf(s.toUpperCase()))
                    .orElse(InfoLogLevel.INFO_LEVEL);
                options.setInfoLogLevel(level);

            }
        }
        setterConfig.writeBufferSize().ifPresent(options::setWriteBufferSize);
        setterConfig.maxWriteBufferNumber().ifPresent(options::setMaxWriteBufferNumber);
    }

    @VisibleForTesting
    DefaultRocksDBConfigSetterConfig getConfig() {
        return setterConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(final String storeName, final Options options) {
        if (statistics != null) {
            statistics.close();
        }
    }

    public static class DefaultRocksDBConfigSetterConfig extends AbstractConfig {

        static final String ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG   = "rocksdb.stats.dump.period.sec";
        static final String ROCKSDB_STATS_ENABLECONFIG             = "rocksdb.stats.enable";
        static final String ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG       = "rocksdb.log.max.file.size";
        static final String ROCKSDB_LOG_DIR_CONFIG                 = "rocksdb.log.dir";
        static final String ROCKSDB_LOG_LEVEL_CONFIG               = "rocksdb.log.level";
        static final String ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG = "rocksdb.max.write.buffer.number";
        static final String ROCKSDB_WRITE_BUFFER_SIZE_CONFIG       = "rocksdb.write.buffer.size";

        DefaultRocksDBConfigSetterConfig(final Map<String, ?> originals) {
            super(configDef(), originals, Collections.emptyMap(), false);
        }

        Optional<String> logLevel() {
            return Optional.ofNullable(getString(ROCKSDB_LOG_LEVEL_CONFIG));
        }
        Optional<Integer> maxWriteBufferNumber() {
            return Optional.ofNullable(getInt(ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG));
        }

        Optional<Long> writeBufferSize() {
            return Optional.ofNullable(getLong(ROCKSDB_WRITE_BUFFER_SIZE_CONFIG));
        }

        Integer dumpPeriodSec() {
            return getInt(ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG);
        }

        String logDir() {
            return getString(ROCKSDB_LOG_DIR_CONFIG);
        }

        Integer maxLogFileSize() {
            return getInt(ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG);
        }

        Boolean isStatisticsEnable() {
            return getBoolean(ROCKSDB_STATS_ENABLECONFIG);
        }

        static ConfigDef configDef() {
            return new ConfigDef()

                .define(ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG, ConfigDef.Type.INT, null,
                    ConfigDef.Importance.HIGH,
                    "The maximum number of memtables build up in memory, before they flush to SST files.")

                .define(ROCKSDB_WRITE_BUFFER_SIZE_CONFIG, ConfigDef.Type.LONG, null,
                    ConfigDef.Importance.HIGH, "The size of a single memtable.")

                .define(ROCKSDB_LOG_LEVEL_CONFIG, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.HIGH, "The RocksDB log level")

                .define(ROCKSDB_STATS_ENABLECONFIG, ConfigDef.Type.BOOLEAN, false,
                    ConfigDef.Importance.HIGH, "Enable RocksDB statistics")

                .define(ROCKSDB_LOG_DIR_CONFIG, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.HIGH, "The RocksDB log directory.")

                .define(ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG, ConfigDef.Type.INT, null,
                    ConfigDef.Importance.HIGH, "The RocksDB statistics dump period in seconds.")

                .define(ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG, ConfigDef.Type.INT, null,
                    ConfigDef.Importance.HIGH, "The RocksDB maximum log file size.");
        }
    }
}
