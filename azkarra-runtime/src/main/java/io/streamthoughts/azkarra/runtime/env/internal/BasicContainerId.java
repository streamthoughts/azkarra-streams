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
package io.streamthoughts.azkarra.runtime.env.internal;

import io.streamthoughts.azkarra.api.ContainerId;
import io.streamthoughts.azkarra.api.model.HasId;

import java.util.Objects;
import java.util.Random;

public class BasicContainerId implements ContainerId {

    /**
     * An helper method to create a new {@link BasicContainerId} based on a given {@link HasId}.
     *
     * @param object    the {@link HasId} object.
     * @return          a new {@link BasicContainerId}.
     */
    public static BasicContainerId create(final HasId object) {
        return new BasicContainerId(object.id());
    }

    private final String id;

    /**
     * Creates a new {@link BasicContainerId} instance.
     *
     * @param id    the id to be used.
     */
    public BasicContainerId(final String id) {
        this.id = Objects.requireNonNull(id, "id should not be null");;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return id;
    }

    /**
     * Randomizes this id with a random suffix whose length is the number of characters specified.
     *
     * @param count the length of random suffix to create
     * @return      a new {@link BasicContainerId}.
     */
    public BasicContainerId randomize(int count) {
        return new BasicContainerId(id() + "-" + randomAlphanumeric(count));
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicContainerId)) return false;
        BasicContainerId that = (BasicContainerId) o;
        return Objects.equals(id, that.id);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id();
    }

    private static String randomAlphanumeric(int limit) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
            .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
            .limit(limit)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }
}
