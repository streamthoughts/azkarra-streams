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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DigestPasswordCredentialsTest {

    private String s = "5f4dcc3b5aa765d61d8327deb882cf99"; // MD5 hash for password (echo -n "password" | md5sum)
    private DigestPasswordCredentials credentials = new DigestPasswordCredentials("MD5", s);

    @Test
    public void testGivenString() {
        boolean verified = credentials.verify("password");
        Assertions.assertTrue(verified);
    }

    @Test
    public void testGivenPlainPassword() {
        boolean verified = credentials.verify(new PlainPasswordCredentials("password"));
        Assertions.assertTrue(verified);
    }

    @Test
    public void testGivenPlainCharArray() {
        boolean verified = credentials.verify("password".toCharArray());
        Assertions.assertTrue(verified);
    }

}