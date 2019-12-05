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

import io.streamthoughts.azkarra.http.security.SecurityMechanism;

import java.net.InetAddress;

public interface AuthenticationContext {

    /**
     * Gets the security mechanism used to authenticate the current user.
     *
     * @return  the {@link SecurityMechanism}.
     */
    SecurityMechanism getSecurityMechanism();

    /**
     * Gets the client address.
     *
     * @return  the {@link InetAddress} instance.
     */
    InetAddress getClientAddress();

    /**
     * Gets the currently authenticated principal.
     *
     * @return  the {@link Authentication}.
     */
    Authentication getAuthentication();

    /**
     * Sets the currently authenticated principal.
     */
    void setAuthentication(final Authentication authentication);
}
