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
package io.streamthoughts.azkarra.api.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VersionTest {

    @Test
    public void shouldCreateVersionFromStringGivenMajorVersion() {
        Version version = Version.parse("1");
        Assertions.assertEquals(1, version.majorVersion());
    }

    @Test
    public void shouldCreateVersionFromStringGivenMajorMinorVersion() {
        Version version = Version.parse("1.2");
        Assertions.assertEquals(1, version.majorVersion());
        Assertions.assertEquals(2, version.minorVersion());
    }

    @Test
    public void shouldCreateVersionFromStringGivenMajorMinorIncrementVersion() {
        Version version = Version.parse("1.2.3");
        Assertions.assertEquals(1, version.majorVersion());
        Assertions.assertEquals(2, version.minorVersion());
        Assertions.assertEquals(3, version.incrementalVersion());
    }

    @Test
    public void shouldCreateVersionFromStringGivenMajorMinorIncrementAndQualifierVersion() {
        Version version = Version.parse("1.2.3-SNAPSHOT");
        Assertions.assertEquals(1, version.majorVersion());
        Assertions.assertEquals(2, version.minorVersion());
        Assertions.assertEquals(3, version.incrementalVersion());
        Assertions.assertEquals("SNAPSHOT", version.qualifier().toString());
    }

    @Test
    public void shouldThrowIllegalArgumentGivenInvalidVersion() {
        IllegalArgumentException e = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Version.parse("bad input"));

        Assertions.assertEquals("Invalid version, cannot parse 'bad input'", e.getMessage());
    }

    @Test
    public void shouldGetLatestVersionGivenMajorVersions() {
        Version result = Version.getLatest(Version.parse("1"), Version.parse("3"), Version.parse("2"));
        Assertions.assertEquals(Version.parse("3"), result);
    }

    @Test
    public void shouldGetLatestVersionGivenMajorMinorVersions() {
        Version result = Version.getLatest(Version.parse("1.2"), Version.parse("1.0"), Version.parse("1.10"));
        Assertions.assertEquals(Version.parse("1.10"), result);
    }

    @Test
    public void shouldGetLatestVersionGivenMajorMinorIncrementalVersions() {
        Version result = Version.getLatest(Version.parse("1.0.9"), Version.parse("1.0.10"), Version.parse("1.0.11"));
        Assertions.assertEquals(Version.parse("1.0.11"), result);
    }

    @Test
    public void shouldGetLatestVersionGivenMajorMinorIncrementalAndSimpleQualifierVersions() {
        Version result = Version.getLatest(Version.parse("1.0.0"), Version.parse("1.0.0-SNAPSHOT"));
        Assertions.assertEquals(Version.parse("1.0.0"), result);

        result = Version.getLatest(Version.parse("1.0.0-ALPHA"), Version.parse("1.0.0-BETA"));
        Assertions.assertEquals(Version.parse("1.0.0-BETA"), result);

        result = Version.getLatest(Version.parse("1.0.0-RELEASE"), Version.parse("1.0.0-SNAPSHOT"));
        Assertions.assertEquals(Version.parse("1.0.0-RELEASE"), result);

        result = Version.getLatest(Version.parse("1.0.0-RC10"), Version.parse("1.0.0-RC12"));
        Assertions.assertEquals(Version.parse("1.0.0-RC12"), result);
    }
}