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
package io.streamthoughts.azkarra.api.streams.rocksdb;

import org.rocksdb.Cache;
import org.rocksdb.LRUCache;
import org.rocksdb.WriteBufferManager;

/**
 * Utility class for creating {@link Cache} and {@link WriteBufferManager} for RocksDB.
 */
final class RocksDBMemoryUtils {

    /**
     * Helper method to create a new {@link RocksDBSharedResources} for RocksDB.
     */
    static RocksDBSharedResources allocateSharedResource(
            final long totalMemorySize,
            final double writeBufferRatio,
            final double highPriorityPoolRatio,
            final boolean strictCapacityLimit
    ) {

        final Cache cache = createCache(totalMemorySize, strictCapacityLimit, highPriorityPoolRatio);

        final long writeBufferManagerCapacity = calculateWriteBufferCapacity(totalMemorySize, writeBufferRatio);
        final WriteBufferManager writeBufferManager = createWriteBufferManager(writeBufferManagerCapacity, cache);

        return new RocksDBSharedResources(
                cache,
                writeBufferManager,
                writeBufferManagerCapacity);
    }

    /**
     * Helper method to create a new {@link Cache} for RocksDB.
     */
    static Cache createCache(final long cacheCapacity,
                             final boolean strictCapacityLimit,
                             final double highPriorityPoolRatio) {
        return new LRUCache(cacheCapacity, -1, strictCapacityLimit, highPriorityPoolRatio);
    }

    /**
     * Helper method to create a new {@link WriteBufferManager} for RocksDB.
     */
    static WriteBufferManager createWriteBufferManager(final long writeBufferManagerCapacity,
                                                       final Cache cache) {
        return new WriteBufferManager(writeBufferManagerCapacity, cache);
    }

    private static long calculateWriteBufferCapacity(final long totalMemorySize,
                                                     final double writeBufferRatio) {
        return (long) (totalMemorySize * writeBufferRatio);
    }
}
