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

/**
 * Class used to hold information about current authenticated principal.
 */
public class AuthenticationContextHolder {

    private static final ThreadLocal<AuthenticationContext> LOCAL_CONTEXT = new InheritableThreadLocal<>();

    /**
     * Gets the {@link AuthenticationContext} attached to the current thread of execution.
     *
     * @return  the current {@link AuthenticationContext} instance.
     */
    public static AuthenticationContext getAuthenticationContext() {
        return LOCAL_CONTEXT.get();
    }

    /**
     * Sets a new {@link AuthenticationContext} with the current thread of execution.
     *
     * @param authenticationContext   the new {@link AuthenticationContext} instance.
     */
    public static void setAuthenticationContext(final AuthenticationContext authenticationContext) {
        LOCAL_CONTEXT.set(authenticationContext);
    }

    /**
     * Clears the context value from the current thread.
     */
    public static void clearContext() {
        LOCAL_CONTEXT.remove();
    }

}
