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

import io.streamthoughts.azkarra.commons.rocksdb.internal.OpaqueMemoryResource;
import io.streamthoughts.azkarra.commons.rocksdb.internal.ResourceInitializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDB;

public class RocksDBMemoryManagerTest {

    private final RocksDBMemoryManager manager = new RocksDBMemoryManager();

    @BeforeAll
    public static void beforeAll() {
        RocksDB.loadLibrary();
    }

    @Test
    public void should_allocate_and_dispose_shared_resource_once() throws Exception {

        final ResourceInitializer<RocksDBSharedResources> initializer = () -> RocksDBMemoryUtils
                .allocateSharedResource(
                    4 * 1024,
                    0.5,
                    0.1,
                    true
                );

        Object leaseHolder1 = new Object();
        Object leaseHolder2 = new Object();
        OpaqueMemoryResource<RocksDBSharedResources> resourceOne = manager
                .getOrAllocateSharedResource(initializer, leaseHolder1);

        OpaqueMemoryResource<RocksDBSharedResources> resourceTwo = manager
                .getOrAllocateSharedResource(initializer, leaseHolder2);

        Assertions.assertNotNull(resourceOne);
        Assertions.assertNotNull(resourceTwo);

        Assertions.assertEquals(resourceOne.getResource(), resourceTwo.getResource());

        resourceOne.close();
        Assertions.assertFalse(resourceOne.getResource().isClosed());
        Assertions.assertFalse(resourceTwo.getResource().isClosed());

        resourceTwo.close();
        Assertions.assertTrue(resourceOne.getResource().isClosed());
        Assertions.assertTrue(resourceTwo.getResource().isClosed());

    }
}