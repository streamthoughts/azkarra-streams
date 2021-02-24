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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDB;

import java.util.HashMap;
import java.util.Map;

public class AzkarraRocksDBConfigSetterTest {

    @BeforeAll
    public static void beforeAll() {
        RocksDB.loadLibrary();
    }

    @Test
    public void should_init_shared_resource_given_memory_managed_true() {
        AzkarraRocksDBConfigSetter setter = new AzkarraRocksDBConfigSetter();

        Map<String, Object> props = new HashMap<>();
        props.put(AzkarraRocksDBConfigSetterConfig.ROCKSDB_MEMORY_MANAGED_CONFIG, true);
        setter.configure(props);

        Assertions.assertNotNull(setter.getSharedResources());
    }

    @Test
    public void should_not_init_shared_resource_given_memory_managed_false() {
        AzkarraRocksDBConfigSetter setter = new AzkarraRocksDBConfigSetter();

        Map<String, Object> props = new HashMap<>();
        props.put(AzkarraRocksDBConfigSetterConfig.ROCKSDB_MEMORY_MANAGED_CONFIG, false);
        setter.configure(props);

        Assertions.assertNull(setter.getSharedResources());
    }
}