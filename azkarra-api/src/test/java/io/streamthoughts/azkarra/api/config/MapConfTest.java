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
package io.streamthoughts.azkarra.api.config;

import io.streamthoughts.azkarra.api.errors.InvalidConfException;
import io.streamthoughts.azkarra.api.errors.MissingConfException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapConfTest {

    private static final Map<String, Object> MIXIN_MAP_WITH_CONF;

    private static final String VALUE_1 = "v1";
    private static final String VALUE_2 = "v2";
    private static final String VALUE_3 = "v3";
    private static final String VALUE_4 = "v4";

    static  {
        MIXIN_MAP_WITH_CONF = new HashMap<>();
        MIXIN_MAP_WITH_CONF.put("field.foo", "v1");
        MIXIN_MAP_WITH_CONF.put("field.bar.foo", MapConf.singletonConf("bar", "v2"));
        MIXIN_MAP_WITH_CONF.put("field.bar.bar", Collections.singletonMap("bar", "v3"));
    }

    @Test
    public void shouldCreateSingletonMapConf() {
        MapConf conf = MapConf.singletonConf("foo", VALUE_1);
        Assertions.assertNotNull(conf);
        Assertions.assertEquals(VALUE_1, conf.getString("foo"));
    }

    @Test
    public void shouldThrowExceptionWhenKeyIsMissing() {
        MapConf conf = new MapConf(Collections.emptyMap());
        Assertions.assertThrows(MissingConfException.class, () -> conf.getString("foo"));
    }

    @Test
    public void shouldThrowExceptionWhenKeyIsMissingGivenPath() {
        Conf conf = MapConf.singletonConf("prefix.key1", VALUE_1)
                    .withFallback(MapConf.singletonConf("prefix.key2", VALUE_2));
        Assertions.assertThrows(MissingConfException.class, () -> conf.getString("prefix.dummy"));
    }

    @Test
    public void shouldFallbackGivenMissingKey() {
        Conf conf = new MapConf(Collections.emptyMap())
                        .withFallback(MapConf.singletonConf("foo1", VALUE_1))
                        .withFallback(MapConf.singletonConf("foo2", VALUE_2));

        Assertions.assertNotNull(conf);
        Assertions.assertEquals(VALUE_1, conf.getString("foo1"));
        Assertions.assertEquals(VALUE_2, conf.getString("foo2"));
    }

    @Test
    public void shouldConvertToMapWithFallback() {
        Conf conf = new MapConf(Collections.emptyMap())
                .withFallback(MapConf.singletonConf("foo1", VALUE_1))
                .withFallback(MapConf.singletonConf("foo2", VALUE_2));

        Map<String, Object> m = conf.getConfAsMap();
        Assertions.assertEquals(VALUE_1, m.get("foo1"));
        Assertions.assertEquals(VALUE_2, m.get("foo2"));
    }

    @Test
    public void shouldConvertToFlapMap() {
        Map<String, Object> origin = new HashMap<>();
        origin.put("field.foo", "?");
        origin.put("field.bar", new HashMap<>(){{
            put("foo", "?");
            put("bar", "?");
        }});

        Map<String, Object> result = new MapConf(origin).getConfAsMap();
        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.containsKey("field.foo"));
        Assertions.assertTrue(result.containsKey("field.bar.foo"));
        Assertions.assertTrue(result.containsKey("field.bar.bar"));
    }

    @Test
    public void shouldConvertToFlapMapGivenFallback() {
       Conf conf = new MapConf(Collections.singletonMap("prefix.key1", VALUE_1))
            .withFallback(new MapConf(new HashMap<>(){{
                put("prefix.key1", VALUE_2);
                put("prefix.key2", VALUE_3);
            }}))
            .withFallback(new MapConf(Collections.singletonMap("prefix.key3", VALUE_4)));
        Map<String, Object> result = conf.getConfAsMap();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(VALUE_1, result.get("prefix.key1"));
        Assertions.assertEquals(VALUE_3, result.get("prefix.key2"));
        Assertions.assertEquals(VALUE_4, result.get("prefix.key3"));
    }

    @Test
    public void shouldGetSubConfGivenKey() {
        Conf conf = new MapConf(MIXIN_MAP_WITH_CONF);

        Conf subConf = conf.getSubConf("field");
        Assertions.assertTrue(subConf.hasPath("bar"));
        Assertions.assertTrue(subConf.hasPath("foo"));
    }

    @Test
    public void shouldGetSubConfGivenPath() {
        Conf conf = new MapConf(MIXIN_MAP_WITH_CONF);
        Assertions.assertEquals(VALUE_2, conf.getString("field.bar.foo.bar"));
    }

    @Test
    public void shouldGetSubConfGivenPathExistingInFallback() {
        Conf conf = new MapConf(Collections.emptyMap()).withFallback(new MapConf(MIXIN_MAP_WITH_CONF));
        Assertions.assertEquals(VALUE_2, conf.getString("field.bar.foo.bar"));
    }

    @Test
    public void shouldGetSubConfGivenPathExistingInFallbackWithCommonKey() {
        Conf conf = MapConf.singletonConf("prefix.key1", VALUE_1)
                .withFallback(MapConf.singletonConf("prefix.key2", VALUE_2));
        Assertions.assertEquals(VALUE_2, conf.getString("prefix.key2"));
    }

    @Test
    public void shouldReturnTrueWhenConfContainsGivenPath() {
        Conf conf = new MapConf(MIXIN_MAP_WITH_CONF);
        Assertions.assertTrue(conf.hasPath("field.bar.foo"));
    }

    @Test
    public void shouldReturnTrueWhenFallbackConfContainsGivenPath() {
        Conf conf = new MapConf(Collections.emptyMap()).withFallback(new MapConf(MIXIN_MAP_WITH_CONF));
        Assertions.assertTrue(conf.hasPath("field.bar.foo"));
    }

    @Test
    public void shouldReturnFalseWhenFallbackConfContainsGivenInvalidPath() {
        Conf conf = new MapConf(Collections.emptyMap()).withFallback(new MapConf(MIXIN_MAP_WITH_CONF));
        Assertions.assertFalse(conf.hasPath("invalid.path"));
    }

    @Test
    public void shouldReturnTrueWhenFallbackConfContainsGivenPathAndCommonKey() {
        Conf conf = MapConf.singletonConf("prefix.key1", VALUE_1)
                .withFallback(MapConf.singletonConf("prefix.key2", VALUE_2));
        Assertions.assertTrue(conf.hasPath("prefix.key2"));
    }

    @Test
    public void shouldThrowGivenInvalidConfWhenCreating() {
        Map<String, Object> origin = new HashMap<>();
        origin.put("field", VALUE_1);
        origin.put("field.foo", MapConf.singletonConf("bar", VALUE_2));
        Assertions.assertThrows(InvalidConfException.class, () -> {
            new MapConf(origin);
        });
    }

    @Test
    public void shouldCreateConfGivenMixinMap() {
        Conf conf = new MapConf(MIXIN_MAP_WITH_CONF);
        Assertions.assertTrue(conf.hasPath("field"));
        Assertions.assertTrue(conf.getSubConf("field").hasPath("foo"));
        Assertions.assertTrue(conf.getSubConf("field").hasPath("bar"));
        Assertions.assertTrue(conf.getSubConf("field").getSubConf("bar").hasPath("bar"));
        Assertions.assertTrue(conf.getSubConf("field").getSubConf("bar").hasPath("foo"));
        Assertions.assertTrue(conf.getSubConf("field").getSubConf("bar").hasPath("bar"));
        Assertions.assertTrue(conf.getSubConf("field").getSubConf("bar").getSubConf("foo").hasPath("bar"));
        Assertions.assertTrue(conf.getSubConf("field").getSubConf("bar").getSubConf("bar").hasPath("bar"));
    }

    @Test
    public void shouldSupportTypeConversion() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("list", "one,two, three, four");
        map.put("int", "1234");
        map.put("bool", "true");
        MapConf conf = new MapConf(map);

        List<String> list = conf.getStringList("list");;
        Assertions.assertTrue(Arrays.asList("one","two", "three", "four").containsAll(list));
        Assertions.assertEquals(1234, conf.getInt("int"));
        Assertions.assertTrue(conf.getBoolean("bool"));
    }
}