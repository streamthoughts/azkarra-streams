/*
 * Copyright 2019-2021 StreamThoughts.
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
package io.streamthoughts.azkarra.api.model;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An interface representing a model that has a name.
 */
public interface HasName {

    String name();

    /**
     * An helper method to filter a {@link Collection} of {@link HasName} object based on its name.
     *
     * @param collections   the collection of {@link HasName}.
     *
     * @return              a set containing only the string ids.
     */
    static <T extends HasName> Optional<T> filterByName(final Collection<T> collections,
                                                    final String name) {
        return collections.stream().filter(hasName -> hasName.name().equals(name)).findFirst();
    }

    /**
     * An helper method to get string names from a collection of {@link HasName} objects.
     *
     * @param collections   the collection of {@link HasName}.
     *
     * @return              a set containing only the string ids.
     */
    static Set<String> getIds(final Collection<? extends HasName> collections) {
        return collections.stream().map(HasName::name).collect(Collectors.toSet());
    }
}
