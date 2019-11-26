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
package io.streamthoughts.azkarra.http.security.auth;

import java.security.Principal;

public interface Authentication {

    /**
     * The {@link Credentials} of the user to authenticate.
     *
     * @return  the {@link Credentials}.
     */
    Credentials getCredentials();

    /**
     * The {@link Principal} of the user to authenticate.
     *
     * @return  the {@link Principal}.
     */
    Principal getPrincipal();

    /**
     * Gets details about the authenticated user.
     *
     * @return the {@link UserDetails} if user is authenticated, {@code null} otherwise.
     */
    default UserDetails getUserDetails() {
        return null;
    }

    /**
     * Checks whether the user has been authenticated successfully.
     *
     * @return  {@code true} if the user if authenticated, {@code false} otherwise.
     */
    boolean isAuthenticated();

    void setAuthenticated(final boolean isAuthenticated);

}
