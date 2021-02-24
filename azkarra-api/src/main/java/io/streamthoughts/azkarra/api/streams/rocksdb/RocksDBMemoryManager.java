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

import io.streamthoughts.azkarra.api.streams.rocksdb.internal.OpaqueMemoryResource;
import io.streamthoughts.azkarra.api.streams.rocksdb.internal.ResourceDisposer;
import io.streamthoughts.azkarra.api.streams.rocksdb.internal.ResourceInitializer;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@code RocksDBMemoryManager} is used to manage allocated resources shared across RocksDB instances.
 */
final class RocksDBMemoryManager {

    private final ReentrantLock lock = new ReentrantLock();

    private LeasedResource<RocksDBSharedResources> leasedResource;

    /**
     * Gets the shared {@link RocksDBSharedResources} and registers a lease. If the object does not yet exist,
     * it will be allocated through the given initializer.
     *
     * @param initializer   the initializer function.
     * @param leaseHolder   the lease to register.
     *
     * @return              the shared {@link RocksDBSharedResources}.
     */
    OpaqueMemoryResource<RocksDBSharedResources> getOrAllocateSharedResource(
            final ResourceInitializer<RocksDBSharedResources> initializer,
            final Object leaseHolder
    ) throws Exception {
        lock.lock();
        try {
            if (leasedResource == null) {
                RocksDBSharedResources resource = initializer.apply();
                leasedResource = new LeasedResource<>(resource);
            }

            leasedResource.addLeaseHolder(leaseHolder);
            ResourceDisposer<Exception> disposer = () -> release(leaseHolder);
            return new OpaqueMemoryResource<>(leasedResource.getResource(), disposer);
        } finally {
            lock.unlock();
        }
    }

    void release(final Object leaseHolder) throws Exception {
        lock.lock();
        try {
            if (leasedResource == null) {
                return;
            }

            if (leasedResource.removeLeaseHolder(leaseHolder)) {
                leasedResource.close();
            }
        } finally {
            lock.unlock();
        }
    }

    private final static class LeasedResource<T extends AutoCloseable> implements AutoCloseable {

        private final T resource;
        private final HashSet<Object> leaseHolders = new HashSet<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);

        /**
         * Creates a new {@link LeasedResource} instance.
         *
         * @param resource  the resource.
         */
        public LeasedResource(final T resource) {
            this.resource = Objects.requireNonNull(resource, "resource should not be null");
        }

        /**
         * Gets the resource.
         */
        public T getResource() {
            return resource;
        }

        /**
         * Adds a new lease to the handle resource.
         *
         * @param leaseHolder   the lease holder object.
         */
        void addLeaseHolder(final Object leaseHolder) {
            leaseHolders.add(leaseHolder);
        }

        /**
         * Removes the given lease holder.
         *
         * @param leaseHolder   the lease holder object.
         * @return              {@code true} is not use any more, and can be disposed.
         */
        boolean removeLeaseHolder(final Object leaseHolder) {
            leaseHolders.remove(leaseHolder);
            return leaseHolders.isEmpty();
        }

        /**
         * Disposes the resource handled.
         */
        @Override
        public void close() throws Exception {
            if (closed.compareAndSet(false, true)) {
                resource.close();
            }
        }
    }
}
