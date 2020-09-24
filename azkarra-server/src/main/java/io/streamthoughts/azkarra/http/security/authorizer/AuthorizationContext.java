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
package io.streamthoughts.azkarra.http.security.authorizer;

import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.http.security.auth.GrantedAuthority;

import java.net.InetAddress;
import java.security.Principal;
import java.util.Collection;

public interface AuthorizationContext {

    /**
     * Gets the current authenticated principal.
     *
     * @return  the {@link Principal}.
     */
    Principal principal();

    /**
     * Gets the list of authorities granted to the authenticated used.
     *
     * @return  the list of {@link GrantedAuthority}
     */
    Collection<GrantedAuthority> authorities();

    /**
     * Gets the client address.
     *
     * @return  the {@link InetAddress} of the client.
     */
    InetAddress clientAddress();

    /**
     * Gets the security mechanism used by the client to authenticate.
     *
     * @return  the {@link SecurityMechanism}.
     */
    SecurityMechanism securityMechanism();

    /**
     * Gets the requested resource.
     *
     * @return  the {@link HttpResource}.
     */
    HttpResource resource();
}
