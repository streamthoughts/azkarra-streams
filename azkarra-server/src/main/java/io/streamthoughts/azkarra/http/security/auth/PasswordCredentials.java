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

import java.util.Objects;

public abstract class PasswordCredentials implements Credentials {

    public enum Type {
        MD5("MD5");

        private final String algorithm;

        Type(final String algorithm) {
            this.algorithm = algorithm;
        }

        public String prefix() {
            return algorithm + ":";
        }
    }

    public static PasswordCredentials get(final String password) {

        if (password.startsWith(Type.MD5.prefix())) {
            return new DigestPasswordCredentials(
                Type.MD5.algorithm,
                password.substring(Type.MD5.prefix().length())
            );
        }

        return new PlainPasswordCredentials(password);
    }

    protected final String password;

    /**
     * Creates a new {@link PasswordCredentials} instance.
     * @param password  the user password.
     */
    protected PasswordCredentials(final String password) {
        Objects.requireNonNull(password, "password cannot be null");
        this.password = password;
    }

    public String password() {
        return password;
    }

    public abstract boolean verify(final Object credentials) throws UnsupportedCredentialException;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordCredentials)) return false;
        PasswordCredentials that = (PasswordCredentials) o;
        return Objects.equals(password, that.password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return password.hashCode();
    }
}
