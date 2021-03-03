/*
 * Copyright 2019-2021 StreamThoughts.
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

import io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetter;
import io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_BACKGROUND_THREADS_COMPACTION_POOL_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_BACKGROUND_THREADS_FLUSH_POOL_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_BLOCK_CACHE_SIZE_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_COMPACTION_STYLE_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_COMPRESSION_TYPE_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_FILES_OPEN_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_LOG_DIR_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_LOG_LEVEL_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_MAX_BACKGROUND_COMPACTIONS_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_MAX_BACKGROUND_FLUSHES_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_MEMORY_MANAGED_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_STATS_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetterConfig.ROCKSDB_WRITE_BUFFER_SIZE_CONFIG;

/**
 * The {@code RocksDBConfig} can be used to easily build a {@link Conf} for RocksDB.
 *
 * @see io.streamthoughts.azkarra.api.StreamsExecutionEnvironment#addConfiguration(Supplier).
 */
public class RocksDBConfig implements Supplier<Conf> {

    private final Map<String, Object> configs = new HashMap<>();

    /**
     * Creates a new {@link RocksDBConfig} instance.
     */
    public RocksDBConfig() {
        configs.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, AzkarraRocksDBConfigSetter.class.getName());
    }

    // MEMORY & PERFORMANCE

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_WRITE_BUFFER_SIZE_CONFIG
     */
    public RocksDBConfig withBufferSize(final int writeBufferSize) {
        configs.put(ROCKSDB_WRITE_BUFFER_SIZE_CONFIG, String.valueOf(writeBufferSize));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG
     */
    public RocksDBConfig withMaxWriteBufferNumber(final int maxWriteBufferNumber) {
        configs.put(ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG, String.valueOf(maxWriteBufferNumber));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_BACKGROUND_THREADS_COMPACTION_POOL_CONFIG
     */
    public RocksDBConfig withBackgroundThreadsCompactionPool(final int backgroundThreadsCompactionPool) {
        configs.put(ROCKSDB_BACKGROUND_THREADS_COMPACTION_POOL_CONFIG, String.valueOf(backgroundThreadsCompactionPool));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_BACKGROUND_THREADS_FLUSH_POOL_CONFIG
     */
    public RocksDBConfig withBackgroundThreadsFlushPool(final int backgroundThreadsFlushPool) {
        configs.put(ROCKSDB_BACKGROUND_THREADS_FLUSH_POOL_CONFIG, String.valueOf(backgroundThreadsFlushPool));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_COMPRESSION_TYPE_CONFIG
     */
    public RocksDBConfig withCompressionType(final CompressionType compressionType) {
        configs.put(ROCKSDB_COMPRESSION_TYPE_CONFIG, compressionType.name());
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_COMPACTION_STYLE_CONFIG
     */
    public RocksDBConfig withCompactionStyle(final CompactionStyle compactionStyle) {
        configs.put(ROCKSDB_COMPACTION_STYLE_CONFIG, compactionStyle.name());
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_FILES_OPEN_CONFIG
     */
    public RocksDBConfig withMaxFileOpen(final int maxFileOpen) {
        configs.put(ROCKSDB_FILES_OPEN_CONFIG, String.valueOf(maxFileOpen));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_MAX_BACKGROUND_COMPACTIONS_CONFIG
     */
    public RocksDBConfig withMaxBackgroundCompactions(final int maxBackgroundCompaction) {
        configs.put(ROCKSDB_MAX_BACKGROUND_COMPACTIONS_CONFIG, String.valueOf(maxBackgroundCompaction));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_MAX_BACKGROUND_FLUSHES_CONFIG
     */
    public RocksDBConfig withMaxBackgroundFlushes(final int maxBackgroundFlushes) {
        configs.put(ROCKSDB_MAX_BACKGROUND_FLUSHES_CONFIG, String.valueOf(maxBackgroundFlushes));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_MEMORY_MANAGED_CONFIG
     */
    public RocksDBConfig withMemoryManaged(final boolean memoryManaged) {
        configs.put(ROCKSDB_MEMORY_MANAGED_CONFIG, String.valueOf(memoryManaged));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_CONFIG
     */
    public RocksDBConfig withMemoryHighPrioPoolRatio(final double memoryManaged) {
        configs.put(ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_CONFIG, String.valueOf(memoryManaged));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_CONFIG
     */
    public RocksDBConfig withMemoryWriteBufferRatio(final double memoryWriteBufferRatio) {
        configs.put(ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_CONFIG, String.valueOf(memoryWriteBufferRatio));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_CONFIG
     */
    public RocksDBConfig withMemoryStrictCapacityLimit(final boolean memoryStrictCapacityLimit) {
        configs.put(ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_CONFIG, String.valueOf(memoryStrictCapacityLimit));
        return this;
    }

    // STATISTICS

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_STATS_ENABLE_CONFIG
     */
    public RocksDBConfig withStatisticsEnabled(boolean enableStats) {
        configs.put(ROCKSDB_STATS_ENABLE_CONFIG, String.valueOf(enableStats));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG
     */
    public RocksDBConfig withStatsDumpPeriod(final Duration duration) {
        configs.put(ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG, String.valueOf(duration.toSeconds()));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_LOG_LEVEL_CONFIG
     */
    public RocksDBConfig withLogDir(final String logDir) {
        configs.put(ROCKSDB_LOG_DIR_CONFIG, logDir);
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_LOG_LEVEL_CONFIG
     */
    public RocksDBConfig withLogLevel(final String level) {
        configs.put(ROCKSDB_LOG_LEVEL_CONFIG, level);
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG
     */
    public RocksDBConfig withLogMaxFileSize(final int logMaxFileSize) {
        configs.put(ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG, String.valueOf(logMaxFileSize));
        return this;
    }

    /**
     * @see AzkarraRocksDBConfigSetterConfig#ROCKSDB_BLOCK_CACHE_SIZE_CONFIG
     */
    public RocksDBConfig withBlockCacheSize(final long blockCacheSize) {
        configs.put(ROCKSDB_BLOCK_CACHE_SIZE_CONFIG, String.valueOf(blockCacheSize));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return configs.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf get() {
        return Conf.of(configs);
    }
}
