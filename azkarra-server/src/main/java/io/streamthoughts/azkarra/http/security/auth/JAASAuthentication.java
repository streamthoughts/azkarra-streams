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

import io.undertow.security.idm.PasswordCredential;

import javax.security.auth.login.LoginContext;
import javax.security.auth.spi.LoginModule;
import java.security.Principal;

public class JAASAuthentication extends UsernamePasswordAuthentication {

    private LoginContext loginContext;

    /**
     * Creates a new {@link UsernamePasswordAuthentication} instance.
     *
     * @param principal    the {@link Principal} of the user to authenticate.
     * @param credential   the {@link PasswordCredential} of the user to authenticate.
     */
    public JAASAuthentication(final Principal principal,
                              final PasswordCredentials credential) {
        this(principal, credential, null);
    }

    /**
     * Creates a new {@link UsernamePasswordAuthentication} instance.
     *
     * @param principal    the {@link Principal} of the user to authenticate.
     * @param credential   the {@link PasswordCredential} of the user to authenticate.
     * @param loginContext the {@link LoginContext} used to authenticate the user.
     */
    public JAASAuthentication(final Principal principal,
                              final PasswordCredentials credential,
                              final LoginContext loginContext) {
        super(principal, credential);
        this.loginContext = loginContext;
    }

    public void setLoginContext(final LoginContext loginContext) {
        this.loginContext = loginContext;
    }

    /**
     * Gets the {@link LoginModule} used to authenticate the user.
     *
     * @return  the {@link LoginModule}.
     */
    public LoginContext getLoginContext() {
        return loginContext;
    }
}
