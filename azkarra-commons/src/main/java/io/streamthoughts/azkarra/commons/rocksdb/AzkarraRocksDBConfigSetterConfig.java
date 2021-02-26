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
package io.streamthoughts.azkarra.commons.rocksdb;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.rocksdb.InfoLogLevel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The {@code AzkarraRocksDBConfigSetterConfig} is used to configure the {@link AzkarraRocksDBConfigSetter}.
 */
public class AzkarraRocksDBConfigSetterConfig extends AbstractConfig {

    private static final String CONFIG_PREFIX = "rocksdb.";

    public static final String ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG = CONFIG_PREFIX + "stats.dump.period.sec";
    public static final String ROCKSDB_STATS_DUMP_PERIOD_SEC_DOC = "The RocksDB statistics dump period in seconds.";

    public static final String ROCKSDB_STATS_ENABLE_CONFIG = CONFIG_PREFIX + "stats.enable";
    private static final String ROCKSDB_STATS_ENABLE_DOC = "Enable RocksDB statistics";

    public static final String ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG = CONFIG_PREFIX + "log.max.file.size";
    private static final String ROCKSDB_MAX_LOG_FILE_SIZE_DOC = "The RocksDB maximum log file size.";

    public static final String ROCKSDB_LOG_DIR_CONFIG = CONFIG_PREFIX + "log.dir";
    private static final String ROCKSDB_LOG_DIR_DOC = "The RocksDB log directory.";

    public static final String ROCKSDB_LOG_LEVEL_CONFIG = CONFIG_PREFIX + "log.level";
    private static final String ROCKSDB_LOG_LEVEL_DOC = "The RocksDB log level";

    // corresponds to write_buffer_number in RocksDB
    public static final String ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG = CONFIG_PREFIX + "max.write.buffer.number";
    private static final String ROCKSDB_MAX_WRITE_BUFFER_NUMBER_DOC =
            "The maximum number of Memtables build up in memory, before they flush to SST files.";

    // corresponds to write_buffer_size in RocksDB
    public static final String ROCKSDB_WRITE_BUFFER_SIZE_CONFIG = CONFIG_PREFIX + "write.buffer.size";
    private static final String ROCKSDB_WRITE_BUFFER_SIZE_DOC = "The size of single Memtable.";
    // see org.apache.kafka.streams.state.internals.RocksDBStore

    public static final String ROCKSDB_MEMORY_MANAGED_CONFIG = CONFIG_PREFIX + "memory.managed";
    public static final String ROCKSDB_MEMORY_MANAGED_DOC = "Enable automatic memory management across all RocksDB instances.";

    public static final String ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_CONFIG = CONFIG_PREFIX + "memory.write.buffer.ratio";
    public static final double ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_DEFAULT = 0.5; // 50%
    private static final String ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_DOC =
             "The ratio of total cache memory which will be reserved for write buffer manager. "
            + "This property is only used when '" + CONFIG_PREFIX + "memory.managed' is set to true.";

    // corresponds to high_prio_pool_ratio in RocksDB
    public static final String ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_CONFIG = CONFIG_PREFIX + "memory.high.prio.pool.ratio";
    public static final double ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_DEFAULT = 0.1; // 10%
    private static final String ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_DOC =
             "The ratio of cache memory that is reserved "
            + "for high priority blocks (e.g.: indexes, filters and compressions blocks). ";

    public static final String ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_CONFIG = CONFIG_PREFIX + "memory.strict.capacity.limit";
    private static final String ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_DOC =
            "Create a block cache with strict capacity limit, ti.e.,  insert to the cache will fail when cache is full"
            + "This property is only used when '" + CONFIG_PREFIX + "memory.managed' is set to true or "
            + "'" + CONFIG_PREFIX + "block.cache.size' is set";
    private static final boolean ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_DEFAULT = false;

    // corresponds to block_cache in RocksDB
    public static final String ROCKSDB_BLOCK_CACHE_SIZE_CONFIG = CONFIG_PREFIX + "block.cache.size";
    private static final String ROCKSDB_BLOCK_CACHE_SIZE_DOC =
            "The total size to be used for caching uncompressed data blocks. ";

    public static final String ROCKSDB_COMPACTION_STYLE_CONFIG = CONFIG_PREFIX + "compaction.style";
    private static final String ROCKSDB_COMPACTION_STYLE_DOC = "The compaction style.";

    public static final String ROCKSDB_COMPRESSION_TYPE_CONFIG = CONFIG_PREFIX + "compression.type";
    private static final String ROCKSDB_COMPRESSION_TYPE_DOC = "The compression type.";

    public static final String ROCKSDB_FILES_OPEN_CONFIG = CONFIG_PREFIX + "files.open";
    private static final String ROCKSDB_FILES_OPEN_DOC =
            "The maximum number of open files that can be used per RocksDB instance.";

    public static final String ROCKSDB_BACKGROUND_THREADS_FLUSH_POOL_CONFIG = CONFIG_PREFIX + "background.thread.flush.pool";
    private static final String ROCKSDB_BACKGROUND_THREADS_FLUSH_POOL_DOC =
            "The number of threads to be used for the background flush process.";

    public static final String ROCKSDB_BACKGROUND_THREADS_COMPACTION_POOL_CONFIG = CONFIG_PREFIX + "background.thread.compaction.pool";
    private static final String ROCKSDB_BACKGROUND_THREADS_COMPACTION_POOL_DOC =
            "The number of threads to be used for the background compaction process.";

    public static final String ROCKSDB_MAX_BACKGROUND_COMPACTIONS_CONFIG = CONFIG_PREFIX + "max.background.compactions";
    private static final String ROCKSDB_MAX_BACKGROUND_COMPACTIONS_DOC =
            "The maximum number of concurrent background compactions";

    public static final String ROCKSDB_MAX_BACKGROUND_FLUSHES_CONFIG = CONFIG_PREFIX + "max.background.flushes";
    private static final String ROCKSDB_MAX_BACKGROUND_FLUSHES_DOC =
            "The maximum number of concurrent flush operations";

    /**
     * Creates a new {@link AzkarraRocksDBConfigSetterConfig} instance.
     *
     * @param originals the config.
     */
    AzkarraRocksDBConfigSetterConfig(final Map<String, ?> originals) {
        super(configDef(), originals, Collections.emptyMap(), false);
    }

    /**
     * Gets the {@link #ROCKSDB_LOG_LEVEL_CONFIG} option.
     */
    Optional<String> getLogLevel() {
        return Optional.ofNullable(getString(ROCKSDB_LOG_LEVEL_CONFIG));
    }

    /**
     * Gets the {@link #ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG} option.
     */
    Optional<Integer> getMaxWriteBufferNumber() {
        return Optional.ofNullable(getInt(ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG));
    }

    /**
     * Gets the {@link #ROCKSDB_WRITE_BUFFER_SIZE_CONFIG} option.
     */
    Optional<Long> getWriteBufferSize() {
        return Optional.ofNullable(getLong(ROCKSDB_WRITE_BUFFER_SIZE_CONFIG));
    }

    /**
     * Gets the {@link #ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG} option.
     */
    int getDumpPeriodSec() {
        return getInt(ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG);
    }

    /**
     * Gets the {@link #ROCKSDB_LOG_DIR_CONFIG} option.
     */
    String getLogDir() {
        return getString(ROCKSDB_LOG_DIR_CONFIG);
    }

    /**
     * Gets the {@link #ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG} option.
     */
    Optional<Integer> getMaxLogFileSize() {
        return Optional.ofNullable(getInt(ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG));
    }

    /**
     * Gets the {@link #ROCKSDB_STATS_ENABLE_CONFIG} option.
     */
    boolean isStatisticsEnable() {
        return getBoolean(ROCKSDB_STATS_ENABLE_CONFIG);
    }

    /**
     * Gets the {@link #ROCKSDB_BLOCK_CACHE_SIZE_CONFIG} option.
     */
    boolean isMemoryManaged() {
        return getBoolean(ROCKSDB_MEMORY_MANAGED_CONFIG);
    }

    /**
     * Gets the {@link #ROCKSDB_BLOCK_CACHE_SIZE_CONFIG} option.
     */
    Optional<Long> getBlockCacheSize() {
        return Optional.ofNullable(getLong(ROCKSDB_BLOCK_CACHE_SIZE_CONFIG));
    }

    /**
     * Gets the {@link #ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_CONFIG} option.
     */
    double getMemoryHighPrioPoolRatio() {
        return getDouble(ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_CONFIG);
    }

    /**
     * Gets the {@link #ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_CONFIG} option.
     */
    double getMemoryWriteBufferRatio() {
        return getDouble(ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_CONFIG);
    }

    /**
     * Gets the {@link #ROCKSDB_COMPACTION_STYLE_CONFIG} option.
     */
    Optional<CompactionStyle> getCompactionStyle() {
        return Optional.ofNullable(getString(ROCKSDB_COMPACTION_STYLE_CONFIG)).map(CompactionStyle::valueOf);
    }

    /**
     * Gets the {@link #ROCKSDB_COMPRESSION_TYPE_CONFIG} option.
     */
    Optional<CompressionType> getCompressionType() {
        return Optional.ofNullable(getString(ROCKSDB_COMPRESSION_TYPE_CONFIG)).map(CompressionType::valueOf);
    }

    /**
     * Gets the {@link #ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_CONFIG} option.
     */
    Boolean getMemoryStrictCapacityLimit() {
        return getBoolean(ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_CONFIG);
    }

    /**
     * Gets the {@link #ROCKSDB_BACKGROUND_THREADS_FLUSH_POOL_CONFIG} option.
     */
    Optional<Integer> getBackgroundThreadsFlushPool() {
        return Optional.ofNullable(getInt(ROCKSDB_BACKGROUND_THREADS_FLUSH_POOL_CONFIG));
    }

    /**
     * Gets the {@link #ROCKSDB_BACKGROUND_THREADS_COMPACTION_POOL_CONFIG} option.
     */
    Optional<Integer> getBackgroundThreadsCompactionPool() {
        return Optional.ofNullable(getInt(ROCKSDB_BACKGROUND_THREADS_COMPACTION_POOL_CONFIG));
    }

    /**
     * Gets the {@link #ROCKSDB_MAX_BACKGROUND_COMPACTIONS_CONFIG} option.
     */
    Optional<Integer> getMaxBackgroundCompactions() {
        return Optional.ofNullable(getInt(ROCKSDB_MAX_BACKGROUND_COMPACTIONS_CONFIG));
    }

    /**
     * Gets the {@link #ROCKSDB_MAX_BACKGROUND_FLUSHES_CONFIG} option.
     */
    Optional<Integer> getMaxBackgroundFlushes() {
        return Optional.ofNullable(getInt(ROCKSDB_MAX_BACKGROUND_FLUSHES_CONFIG));
    }


    int getMaxOpenFile() {
        return getInt(ROCKSDB_FILES_OPEN_CONFIG);
    }

    static ConfigDef configDef() {
        return new ConfigDef()
                .define(
                        ROCKSDB_MAX_BACKGROUND_COMPACTIONS_CONFIG,
                        ConfigDef.Type.INT,
                        null,
                        ConfigDef.Importance.MEDIUM,
                        ROCKSDB_MAX_BACKGROUND_COMPACTIONS_DOC
                )

                .define(
                        ROCKSDB_MAX_BACKGROUND_FLUSHES_CONFIG,
                        ConfigDef.Type.INT,
                        null,
                        ConfigDef.Importance.LOW,
                        ROCKSDB_MAX_BACKGROUND_FLUSHES_DOC
                )

                .define(
                        ROCKSDB_BACKGROUND_THREADS_COMPACTION_POOL_CONFIG,
                        ConfigDef.Type.INT,
                        null,
                        ConfigDef.Importance.LOW,
                        ROCKSDB_BACKGROUND_THREADS_COMPACTION_POOL_DOC
                )

                .define(
                        ROCKSDB_BACKGROUND_THREADS_FLUSH_POOL_CONFIG,
                        ConfigDef.Type.INT,
                        null,
                        ConfigDef.Importance.LOW,
                        ROCKSDB_BACKGROUND_THREADS_FLUSH_POOL_DOC
                )

                .define(
                        ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_CONFIG,
                        ConfigDef.Type.BOOLEAN,
                        ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_DEFAULT,
                        ConfigDef.Importance.LOW,
                        ROCKSDB_MEMORY_STRICT_CAPACITY_LIMIT_DOC
                )

                .define(
                        ROCKSDB_FILES_OPEN_CONFIG,
                        ConfigDef.Type.INT,
                        -1,
                        ConfigDef.Importance.LOW,
                        ROCKSDB_FILES_OPEN_DOC
                )

                .define(
                        ROCKSDB_COMPACTION_STYLE_CONFIG,
                        ConfigDef.Type.STRING,
                        null,
                        new ValidEnum(CompactionStyle.class),
                        ConfigDef.Importance.LOW,
                        ROCKSDB_COMPACTION_STYLE_DOC
                )

                .define(
                        ROCKSDB_COMPRESSION_TYPE_CONFIG,
                        ConfigDef.Type.STRING,
                        null,
                        new ValidEnum(CompressionType.class),
                        ConfigDef.Importance.LOW,
                        ROCKSDB_COMPRESSION_TYPE_DOC
                )

                .define(
                        ROCKSDB_BLOCK_CACHE_SIZE_CONFIG,
                        ConfigDef.Type.LONG,
                        null,
                        ConfigDef.Importance.HIGH,
                        ROCKSDB_BLOCK_CACHE_SIZE_DOC
                )

                .define(
                        ROCKSDB_MEMORY_MANAGED_CONFIG,
                        ConfigDef.Type.BOOLEAN,
                        false,
                        ConfigDef.Importance.HIGH,
                        ROCKSDB_MEMORY_MANAGED_DOC
                )

                .define(
                        ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_CONFIG,
                        ConfigDef.Type.DOUBLE,
                        ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_DEFAULT,
                        ConfigDef.Range.between(0, 1),
                        ConfigDef.Importance.MEDIUM,
                        ROCKSDB_MEMORY_HIGH_PRIO_POOL_RATIO_DOC
                )

                .define(
                        ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_CONFIG,
                        ConfigDef.Type.DOUBLE,
                        ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_DEFAULT,
                        ConfigDef.Range.between(0, 1),
                        ConfigDef.Importance.MEDIUM,
                        ROCKSDB_MEMORY_WRITE_BUFFER_RATIO_DOC
                )

                .define(
                        ROCKSDB_MAX_WRITE_BUFFER_NUMBER_CONFIG,
                        ConfigDef.Type.INT,
                        null,
                        ConfigDef.Importance.HIGH,
                        ROCKSDB_MAX_WRITE_BUFFER_NUMBER_DOC
                )

                .define(
                        ROCKSDB_WRITE_BUFFER_SIZE_CONFIG,
                        ConfigDef.Type.LONG,
                        null,
                        ConfigDef.Importance.HIGH,
                        ROCKSDB_WRITE_BUFFER_SIZE_DOC
                )

                .define(
                        ROCKSDB_LOG_LEVEL_CONFIG,
                        ConfigDef.Type.STRING,
                        null,
                        new ValidEnum(InfoLogLevel.class),
                        ConfigDef.Importance.MEDIUM,
                        ROCKSDB_LOG_LEVEL_DOC
                )

                .define(
                        ROCKSDB_STATS_ENABLE_CONFIG,
                        ConfigDef.Type.BOOLEAN,
                        false,
                        ConfigDef.Importance.MEDIUM,
                        ROCKSDB_STATS_ENABLE_DOC)

                .define(
                        ROCKSDB_LOG_DIR_CONFIG,
                        ConfigDef.Type.STRING,
                        null,
                        ConfigDef.Importance.MEDIUM,
                        ROCKSDB_LOG_DIR_DOC
                )

                .define(
                        ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG,
                        ConfigDef.Type.INT, null,
                        ConfigDef.Importance.MEDIUM,
                        ROCKSDB_STATS_DUMP_PERIOD_SEC_DOC
                )

                .define(
                        ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG,
                        ConfigDef.Type.INT,
                        null,
                        ConfigDef.Importance.MEDIUM,
                        ROCKSDB_MAX_LOG_FILE_SIZE_DOC
                );
    }

    private final static class ValidEnum implements ConfigDef.Validator {

        private final Set<String> validEnums = new LinkedHashSet<>();
        private final Class<?> enumClass;

        public ValidEnum(final Class<?> enumClass) {
            this.enumClass = Objects.requireNonNull(enumClass, "enumClass must not be null");
            for (Object o : enumClass.getEnumConstants()) {
                validEnums.add(o.toString());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ensureValid(final String name, final Object value) {
           if (value == null) return;

            if (value instanceof String) {
                if (!validEnums.contains(value)) {
                    throw new ConfigException(
                            name,
                            String.format(
                                    "'%s' is not a valid value for %s. Valid values are %s",
                                    value,
                                    enumClass.getSimpleName(),
                                    validEnums
                            )
                    );
                }
            } else {
                throw new ConfigException(name, value, "Must be a String");
            }
        }
    }
}
