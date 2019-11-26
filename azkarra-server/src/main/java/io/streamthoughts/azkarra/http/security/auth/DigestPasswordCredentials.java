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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DigestPasswordCredentials extends PasswordCredentials {

    private static final Object DIGESTS_LOCK = new Object();

    private static final Map<String, MessageDigest> DIGESTS = new HashMap<>();

    private final MessageDigest messageDigest;

    private final byte[] bytesPassword;

    /**
     * Creates a new {@link PasswordCredentials} instance.
     *
     * @param password the user password.
     */
    public DigestPasswordCredentials(final String algorithm, final String password) {
        super(password);
        messageDigest = getMessageDigest(algorithm);
        bytesPassword = hexStringToByteArray(password);
    }

    private MessageDigest getMessageDigest(final String algorithm) {
        synchronized (DIGESTS_LOCK) {
            MessageDigest digest = DIGESTS.get(algorithm);
            if (digest == null) {
                try {
                    digest = MessageDigest.getInstance(algorithm);
                    DIGESTS.put(algorithm, digest);
                } catch (final NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
            return digest;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DigestPasswordCredentials)) return false;
        if (!super.equals(o)) return false;
        DigestPasswordCredentials that = (DigestPasswordCredentials) o;
        return Arrays.equals(bytesPassword, that.bytesPassword);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean verify(final Object credentials) throws UnsupportedCredentialException {

        if (credentials instanceof String) {
            final byte[] bytes = ((String) credentials).getBytes(StandardCharsets.UTF_8);
            return digestAndVerify(bytes);
        }

        if (credentials instanceof char[]) {
            final byte[] bytes = new String((char[])credentials).getBytes(StandardCharsets.UTF_8);
            return digestAndVerify(bytes);
        }

        if (credentials instanceof DigestPasswordCredentials) {
            return this.equals(credentials);
        }

        if (credentials instanceof PasswordCredentials) {
            final byte[] bytes = ((PasswordCredentials) credentials).password.getBytes(StandardCharsets.UTF_8);
            return digestAndVerify(bytes);
        }

        throw new UnsupportedCredentialException("Can't validate credentials, type is unsupported '" +
                credentials.getClass()+ "'");
    }

    private boolean digestAndVerify(final byte[] bytes) {
        byte[] digest;
        synchronized (messageDigest) {
            messageDigest.reset();
            messageDigest.update(bytes);
            digest = messageDigest.digest();
        }
        // Don't use Arrays.equals which is not time constant (i.e vulnerable to timing attack).
        return MessageDigest.isEqual(bytesPassword, digest);
    }

    private static byte[] hexStringToByteArray(final String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int d1 = Character.digit(s.charAt(i), 16) << 4;
            int d2 = Character.digit(s.charAt(i + 1), 16);
            data[i / 2] = (byte) (d1 + d2);
        }
        return data;
    }
}
