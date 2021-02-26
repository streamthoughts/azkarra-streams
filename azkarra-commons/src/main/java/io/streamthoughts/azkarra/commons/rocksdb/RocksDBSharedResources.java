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
package io.streamthoughts.azkarra.commons.rocksdb;

import org.rocksdb.Cache;
import org.rocksdb.WriteBufferManager;

import java.util.concurrent.atomic.AtomicBoolean;

final class RocksDBSharedResources implements AutoCloseable {

    private final Cache cache;

    private final WriteBufferManager writeBufferManager;
    private final long writeBufferManagerCapacity;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    RocksDBSharedResources(final Cache cache,
                           final WriteBufferManager writeBufferManager,
                           final long writeBufferManagerCapacity) {
        this.cache = cache;
        this.writeBufferManager = writeBufferManager;
        this.writeBufferManagerCapacity = writeBufferManagerCapacity;
    }

    public Cache getCache() {
        return cache;
    }

    public WriteBufferManager getWriteBufferManager() {
        return writeBufferManager;
    }

    public long getWriteBufferManagerCapacity() {
        return writeBufferManagerCapacity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            writeBufferManager.close();
            cache.close();
        }
    }

    /** VisibleForTesting **/
    boolean isClosed() {
        return closed.get();
    }
}
