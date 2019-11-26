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
package io.streamthoughts.azkarra.api.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArgsConfTest {

    @Test
    public void shouldCreateConfGivenSimpleArgs() {
        ArgsConf conf = new ArgsConf("--key1 value1 --key2 value2".split(" "));
        assertEquals("value1", conf.getString("key1"));
        assertEquals("value2", conf.getString("key2"));
    }

    @Test
    public void shouldCreateConfGivenArgsWithNoValue() {
        ArgsConf conf = new ArgsConf("--key1 --key2 value2".split(" "));
        assertEquals("true", conf.getString("key1"));
        assertEquals("value2", conf.getString("key2"));
    }

    @Test
    public void shouldCreateConfGivenArgsWithNumericValue() {
        ArgsConf conf = new ArgsConf("--key1 -2 --key2 value2".split(" "));
        assertEquals("-2", conf.getString("key1"));
        assertEquals("value2", conf.getString("key2"));
    }
}