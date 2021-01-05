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

package io.streamthoughts.azkarra.api.components;

import java.nio.file.Path;
import java.util.List;

public interface ComponentScanner {

    /**
     * Scans external component for the specified paths.
     *
     * @param componentPaths   the comma-separated list of top-level components directories.
     */
    void scan(String componentPaths);

    /**
     * Scans external component for the specified paths.
     *
     * @param componentPaths   the list of top-level components directories.
     */
    void scan(List<String> componentPaths);

    /**
     * Scans the specified top-level component directory for components.
     *
     * @param componentPath   the absolute path to a top-level component directory.
     */
    void scanComponentPath(Path componentPath);

    /**
     * Scans the specified package for components.
     *
     * @param source    the {@link Package} to be scanned; must not be {@code null}.
     */
    void scanForPackage(Package source);

    /**
     * Scans the specified package for components.
     *
     * @param source    the package to be scanned; must not be {@code null}.
     */
    void scanForPackage(String source);
}
