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
package io.streamthoughts.examples.azkarra.security;

import io.streamthoughts.azkarra.http.ServerConfig;
import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.ComponentScan;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.EnableAutoConfig;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.EnableAutoStart;
import io.streamthoughts.azkarra.streams.config.AzkarraConf;
import io.streamthoughts.examples.azkarra.topology.BasicWordCountTopology;

/**
 * Example to configure an azkarra application with Basic authentication.
 *
 * 1 ) Create a JAAS configuration file 'azkarra-server-jaas.conf' as follows :
 *
 * AzkarraServer {
 *   io.streamthoughts.azkarra.http.security.jaas.spi.PropertiesFileLoginModule required
 *   file="/tmp/azkarra.password"
 *   reloadInterval="60"
 *   reload="true"
 *   debug="true";
 * };
 * 2 ) Create a file containing user's principal information (e.g 'azkarra.password').
 *
 * #user:password [,roles]
 * admin:admin,Administrator
 * user:user,User
 *
 * 3) Configure your application with :
 *
 * -Djava.security.auth.login.config=/path/to/azkarra-server-jaas.conf
 */
@EnableAutoStart
@EnableAutoConfig
@ComponentScan
public class BasicAuthenticationExample {

    public static void main(final String[] args) {

        final ServerConfig serverConfig = ServerConfig.newBuilder()
            .setListener("localhost")
            .setPort(8080)
            // Enable authentication
            .setAuthenticationMethod(SecurityMechanism.BASIC_AUTH.name())
            .setAuthenticationRealm("AzkarraServer")
            .setAuthenticationRoles("Administrator, User")
            .setAuthenticationRestricted("User")
            .build();

        new AzkarraApplication()
            .setConfiguration(AzkarraConf.create("application"))
            .addSource(BasicWordCountTopology.class)
            .enableHttpServer(true, serverConfig)
            .run(args);
    }
}
