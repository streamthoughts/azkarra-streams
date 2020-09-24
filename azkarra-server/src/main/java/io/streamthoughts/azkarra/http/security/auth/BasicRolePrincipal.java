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
package io.streamthoughts.azkarra.http.security.auth;

import java.security.Principal;
import java.util.Objects;

public class BasicRolePrincipal implements Principal {

    private final String role;

    /**
     * Creates a new {@link BasicRolePrincipal} instance.
     *
     * @param role the principal role.
     */
    public BasicRolePrincipal(final String role) {
        this.role = Objects.requireNonNull(role, "role cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return role;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicRolePrincipal)) return false;
        BasicRolePrincipal that = (BasicRolePrincipal) o;
        return Objects.equals(role, that.role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(role);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[role=" + role + "]";
    }
}
