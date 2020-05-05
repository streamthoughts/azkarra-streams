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
package io.streamthoughts.azkarra.api.components.qualifier;

import io.streamthoughts.azkarra.api.components.Qualifier;
import io.streamthoughts.azkarra.api.components.Restriction;
import io.streamthoughts.azkarra.api.util.Version;

public class Qualifiers {

    public static <T> Qualifier<T> byQualifiers(final Qualifier<T>... qualifiers) {
        return new CompositeQualifier<>(qualifiers);
    }

    public static <T> Qualifier<T> byPrimary() {
        return new PrimaryQualifier<>();
    }

    public static <T> Qualifier<T> bySecondary() {
        return new SecondaryQualifier<>(false);
    }

    public static <T> Qualifier<T> excludeSecondary() {
        return new SecondaryQualifier<>(true);
    }

    public static <T> Qualifier<T> byName(final String name) {
        return new NamedQualifier<>(name);
    }

    public static <T> Qualifier<T> excludeByName(final String name) {
        return new NamedQualifier<>(name, false);
    }

    public static <T> Qualifier<T> byVersion(final String version) {
        return byVersion(Version.parse(version));
    }

    public static <T> Qualifier<T> byVersion(final Version version) {
        return new VersionQualifier<>(version);
    }

    public static <T> Qualifier<T> byRestriction(final String type, final String value) {
        return byRestriction(new Restriction(type, value));
    }

    public static <T> Qualifier<T> byRestriction(final Restriction restriction) {
        return new RestrictionQualifier<>(restriction);
    }

    public static <T> Qualifier<T> byLatestVersion() {
        return new LatestVersionQualifier<>();
    }
}
