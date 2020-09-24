/*
 * Copyright 2019-2020 StreamThoughts.
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
import org.junit.jupiter.api.Test;
import org.rocksdb.Options;

import java.util.HashMap;
import java.util.Map;

class DefaultRocksDBConfigSetterTest {

    @Test
    public void shouldBeConfiguredGivenValidConfiguration() {

        DefaultRocksDBConfigSetter setter = new DefaultRocksDBConfigSetter();

        Map<String, Object> config = new HashMap<>();
        config.put(DefaultRocksDBConfigSetter.DefaultRocksDBConfigSetterConfig.ROCKSDB_LOG_DIR_CONFIG, "/log/dir");
        config.put(DefaultRocksDBConfigSetter.DefaultRocksDBConfigSetterConfig.ROCKSDB_MAX_LOG_FILE_SIZE_CONFIG, "200");
        config.put(DefaultRocksDBConfigSetter.DefaultRocksDBConfigSetterConfig.ROCKSDB_STATS_ENABLECONFIG, true);
        config.put(DefaultRocksDBConfigSetter.DefaultRocksDBConfigSetterConfig.ROCKSDB_STATS_DUMP_PERIOD_SEC_CONFIG, "100");
        setter.setConfig("storeName", new Options(), config);

        DefaultRocksDBConfigSetter.DefaultRocksDBConfigSetterConfig configured = setter.getConfig();

        Assertions.assertEquals(100, configured.dumpPeriodSec());
        Assertions.assertEquals(true, configured.isStatisticsEnable());
        Assertions.assertEquals("/log/dir", configured.logDir());
        Assertions.assertEquals(200, configured.maxLogFileSize());
    }
}