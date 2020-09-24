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
package io.streamthoughts.azkarra.streams.components.isolation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComponentResolverTest {


    @Test
    public void shouldReturnSingleComponentGivenComponentComponentFolderWithClassFiles(
            final @TempDir Path topLevelComponentPath) throws IOException {

        final ComponentResolver resolver = new ComponentResolver(topLevelComponentPath);

        Path componentPath = createComponentFolder(topLevelComponentPath, "component-folder");

        Files.createFile(Paths.get(componentPath.toString(), "file-1.class"));
        Files.createFile(Paths.get(componentPath.toString(), "file-2.class"));

        final List<ExternalComponent> components = resolver.resolves();

        assertEquals(1, components.size());
        assertEquals(newComponent(componentPath, componentPath), components.get(0));
    }

    private Path createComponentFolder(final Path topLevelComponentPath,
                                       final String componentName) throws IOException {
        return Files.createDirectories(Paths.get(topLevelComponentPath.toString(), componentName));
    }

    @Test
    public void shouldReturnSingleComponentWithJarURIsGivenComponentFolder(
            final @TempDir Path topLevelComponentPath) throws IOException {

        final ComponentResolver resolver = new ComponentResolver(topLevelComponentPath);

        Path componentPath = createComponentFolder(topLevelComponentPath, "component-folder");
        Path subComponentPath = createComponentFolder(componentPath, "sub-component");

        final ExternalComponent expected = newComponent(
            componentPath,
            Files.createFile(Paths.get(componentPath.toString(), "component-lib1.jar")),
            Files.createFile(Paths.get(subComponentPath.toString(), "component-lib2.jar")));

        final List<ExternalComponent> components = resolver.resolves();

        assertEquals(1, components.size());
        assertEquals(expected, components.get(0));
    }

    @Test
    public void shouldReturnSingleComponentWithJARsOnlyGivenDirectoryWithBothJARsAndClassFiles(
            final @TempDir Path topLevelComponentPath) throws IOException {
        final ComponentResolver resolver = new ComponentResolver(topLevelComponentPath);

        Path componentPath = createComponentFolder(topLevelComponentPath, "component-folder");

        Files.createFile(Paths.get(componentPath.toString(), "file.class"));
        Path archive = Files.createFile(Paths.get(componentPath.toString(), "component-lib1.jar"));

        final ExternalComponent expected = newComponent(componentPath, archive);

        final List<ExternalComponent> components = resolver.resolves();

        assertEquals(1, components.size());
        assertEquals(expected, components.get(0));
    }

    @Test
    public void shouldReturnOneComponentPerJARGivenComponentJARs(final @TempDir Path topLevelComponentPath) throws IOException {

        final ComponentResolver resolver = new ComponentResolver(topLevelComponentPath);

        Path componentJar1 = Files.createFile(Paths.get(topLevelComponentPath.toString(), "component1.jar"));
        Path componentJar2 = Files.createFile(Paths.get(topLevelComponentPath.toString(), "component2.jar"));

        final ExternalComponent expected1 = newComponent(componentJar1, componentJar1);
        final ExternalComponent expected2 = newComponent(componentJar2, componentJar2);

        final List<ExternalComponent> components = resolver.resolves();
        components.sort(Comparator.comparing(o -> o.location().toString()));
        assertEquals(2, components.size());

        assertEquals(expected1, components.get(0));
        assertEquals(expected2, components.get(1));
    }

    @Test
    public void shouldReturnOneComponentPerArchiveGivenComponentZIPs(final @TempDir Path topLevelComponentPath) throws IOException {

        final ComponentResolver resolver = new ComponentResolver(topLevelComponentPath);

        Path componentArchive1 = Files.createFile(Paths.get(topLevelComponentPath.toString(), "component1.zip"));
        Path componentArchive2 = Files.createFile(Paths.get(topLevelComponentPath.toString(), "component2.zip"));

        final ExternalComponent expected1 = newComponent(componentArchive1, componentArchive1);
        final ExternalComponent expected2 = newComponent(componentArchive2, componentArchive2);

        final List<ExternalComponent> components = resolver.resolves();
        components.sort(Comparator.comparing(o -> o.location().toString()));
        assertEquals(2, components.size());
        assertEquals(expected1, components.get(0));
        assertEquals(expected2, components.get(1));
    }

    private ExternalComponent newComponent(final Path location, final Path...resources) throws MalformedURLException {
        URL[] urls = Arrays.stream(resources)
            .map(p -> {
                try {
                    return p.toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(URL[]::new);
        return new ExternalComponent(location.toUri().toURL(), urls);
    }
}
