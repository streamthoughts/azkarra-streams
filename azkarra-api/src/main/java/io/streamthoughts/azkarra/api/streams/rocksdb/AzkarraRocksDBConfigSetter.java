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
package io.streamthoughts.azkarra.api.streams.rocksdb;

import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.streams.rocksdb.internal.OpaqueMemoryResource;
import io.streamthoughts.azkarra.api.streams.rocksdb.internal.ResourceInitializer;
import io.streamthoughts.azkarra.api.util.Utils;
import org.apache.kafka.common.Configurable;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.Cache;
import org.rocksdb.Env;
import org.rocksdb.InfoLogLevel;
import org.rocksdb.Options;
import org.rocksdb.RocksObject;
import org.rocksdb.Statistics;
import org.rocksdb.StatsLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * This {@link RocksDBConfigSetter} implementation allows fine-tuning of RocksDB instances.
 * It also allows to manage shared memory across all RocksDB instances and to enable dump of RocksDB statistics.
 */
public class AzkarraRocksDBConfigSetter implements RocksDBConfigSetter, Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(AzkarraRocksDBConfigSetter.class);

    /**
     * @see org.apache.kafka.streams.state.internals.RocksDBStore
     */
    public static final int ROCKSDB_BLOCK_CACHE_SIZE_DEFAULT = 50 * 1024 * 1024; // 50 MB

    private static final RocksDBMemoryManager MEMORY_MANAGER = new RocksDBMemoryManager();

    private final List<RocksObject> objectsToClose = new ArrayList<>();

    private OpaqueMemoryResource<RocksDBSharedResources> sharedResources;

    private AzkarraRocksDBConfigSetterConfig rocksDBConfig;

    private final AtomicBoolean configured = new AtomicBoolean(false);

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Map<String, ?> configs) {
        if (configured.compareAndSet(false, true)) {
            rocksDBConfig = new AzkarraRocksDBConfigSetterConfig(configs);
            if (rocksDBConfig.isMemoryManaged()) {
                final Object leaseHolder = new Object();
                final ResourceInitializer<RocksDBSharedResources> initializer = () -> RocksDBMemoryUtils
                        .allocateSharedResource(
                                rocksDBConfig.getBlockCacheSize().orElse(ROCKSDB_BLOCK_CACHE_SIZE_DEFAULT),
                                rocksDBConfig.getMemoryWriteBufferRatio(),
                                rocksDBConfig.getMemoryHighPrioPoolRatio(),
                                rocksDBConfig.getMemoryStrictCapacityLimit()
                        );
                try {
                    sharedResources = MEMORY_MANAGER.getOrAllocateSharedResource(initializer, leaseHolder);
                } catch (Exception e) {
                    throw new ConfigException("Failed to allocate new WriteBufferManager", e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig(final String storeName,
                          final Options options,
                          final Map<String, Object> configs) {

        configure(configs); //  RocksDBStore class does not invoke the configure method.

        if (rocksDBConfig.isStatisticsEnable()) {
            LOG.info("Enabling RocksDB statistics for state store '{}'", storeName);
            Statistics statistics = new Statistics();
            statistics.setStatsLevel(StatsLevel.ALL);
            options.setStatistics(statistics);
            options.setStatsDumpPeriodSec(rocksDBConfig.getDumpPeriodSec());

            rocksDBConfig.getMaxLogFileSize()
                    .ifPresent(options::setMaxLogFileSize);

            if (rocksDBConfig.getLogDir() != null) {
                options.setDbLogDir(rocksDBConfig.getLogDir());
                final InfoLogLevel level = rocksDBConfig.getLogLevel()
                        .map(s -> InfoLogLevel.valueOf(s.toUpperCase()))
                        .orElse(InfoLogLevel.INFO_LEVEL);
                options.setInfoLogLevel(level);

            }
            objectsToClose.add(statistics);
        } else {
            options.setStatsDumpPeriodSec(0); // ensure statistics are disable
        }

        final BlockBasedTableConfig blockBasedTableConfig = (BlockBasedTableConfig) options.tableFormatConfig();

        if (sharedResources != null) {
            RocksDBSharedResources resource = sharedResources.getResource();
            LOG.info(
                    "Configuring state store '{}' using shared buffer manager with a capacity of '{}' bytes",
                    storeName,
                    resource.getWriteBufferManagerCapacity()
            );
            options.setWriteBufferManager(resource.getWriteBufferManager());

            blockBasedTableConfig.setBlockCache(resource.getCache());
            // Store index and filter blocks in cache with other data blocks
            blockBasedTableConfig.setCacheIndexAndFilterBlocks(true);
            blockBasedTableConfig.setCacheIndexAndFilterBlocksWithHighPriority(true);
            blockBasedTableConfig.setPinL0FilterAndIndexBlocksInCache(true);
            options.setTableFormatConfig(blockBasedTableConfig);
        } else {
            if (rocksDBConfig.getBlockCacheSize().isPresent()) {
                final Cache cache = RocksDBMemoryUtils.createCache(
                        rocksDBConfig.getBlockCacheSize().get(),
                        rocksDBConfig.getMemoryStrictCapacityLimit(),
                        rocksDBConfig.getMemoryHighPrioPoolRatio()
                );
                objectsToClose.add(cache);
                blockBasedTableConfig.setBlockCache(cache);
            }
        }

        // Sets the max open-files.
        options.setMaxOpenFiles(rocksDBConfig.getMaxOpenFile());

        // Sets the size of a single Memtable.
        rocksDBConfig.getWriteBufferSize().
                ifPresent(options::setWriteBufferSize);

        // Sets the number of Memtables (default 3).
        rocksDBConfig.getMaxWriteBufferNumber().
                ifPresent(options::setMaxWriteBufferNumber);

        // Sets the compaction style.
        rocksDBConfig.getCompactionStyle().
                ifPresent(options::setCompactionStyle);

        // Sets the compression type.
        rocksDBConfig.getCompressionType().
                ifPresent(options::setCompressionType);

        // TUNING FLUSHED AND COMPACTION
        // Sets the number of background threads to be used for the flush pool.
        rocksDBConfig.getBackgroundThreadsFlushPool()
                .ifPresent(num -> options.getEnv().setBackgroundThreads(num, Env.FLUSH_POOL));

        // Sets the number of background threads to be used for the compaction pool.
        rocksDBConfig.getBackgroundThreadsCompactionPool()
                .ifPresent(num -> options.getEnv().setBackgroundThreads(num, Env.COMPACTION_POOL));

        rocksDBConfig.getMaxBackgroundCompactions()
                .ifPresent(options::setMaxBackgroundCompactions);

        rocksDBConfig.getMaxBackgroundFlushes()
                .ifPresent(options::setMaxBackgroundFlushes);
    }

    @VisibleForTesting
    AzkarraRocksDBConfigSetterConfig getConfig() {
        return rocksDBConfig;
    }

    @VisibleForTesting
    OpaqueMemoryResource<RocksDBSharedResources> getSharedResources() {
        return sharedResources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(final String storeName, final Options options) {
        objectsToClose.forEach(Utils::closeQuietly);
        objectsToClose.clear();

        if (sharedResources != null) {
            Utils.closeQuietly(sharedResources);
        }
    }
}
