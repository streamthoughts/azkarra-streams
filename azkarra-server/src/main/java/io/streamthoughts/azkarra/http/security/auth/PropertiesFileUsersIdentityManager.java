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

import io.streamthoughts.azkarra.api.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PropertiesFileUsersIdentityManager extends InMemoryUserIdentityManager {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesFileUsersIdentityManager.class);

    private static final AtomicInteger TASK_COUNTER = new AtomicInteger(0);

    private final String file;
    private boolean debug;

    private PropertiesFileLoaderTask task;

    /**
     * Creates a new {@link PropertiesFileUsersIdentityManager} instance.
     *
     * @param file              the path of the file to load.
     * @param debug             is debug enable.
     */
    public PropertiesFileUsersIdentityManager(final String file, boolean debug) {
        this.file = file;
        this.debug = debug;
        loadUsers();
    }

    public void startAutoReload(final Duration reloadInterval) {
        if (task == null) {
            long reloadIntervalMs = reloadInterval.toMillis();
            if (reloadIntervalMs > 0) {
                task = new PropertiesFileLoaderTask(reloadIntervalMs, this::loadUsers);
                task.setDaemon(true);
                task.start();
            }
        }
    }

    public void stopAutoReload() {
        if (task != null) {
            task.shutdown();
        }
    }

    private void loadUsers() {
        try {
            mayLog(
                "Starting to load users's principal information from properties file '" + file + "'",
                true
            );
            FileInputStream is = new FileInputStream(file);
            final Properties properties = new Properties();
            properties.load(is);

            final Set<String> knownUsers = getUsers();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                final String username = ((String) entry.getKey()).trim();
                String credentials = ((String) entry.getValue()).trim();
                String roles = null;
                int rolesIndex = credentials.indexOf(',');
                if (rolesIndex > 0) {
                    roles = credentials.substring(rolesIndex + 1).trim();
                    credentials = credentials.substring(0, rolesIndex).trim();
                }

                if (!username.isEmpty() && !credentials.isEmpty()) {

                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (roles != null && !roles.isEmpty()) {
                        authorities = Arrays
                                .stream(roles.split(","))
                                .map(r -> new RoleGrantedAuthority(r.trim()))
                                .collect(Collectors.toList());
                    }
                    final PasswordCredentials password = PasswordCredentials.get(credentials);
                    addUsers(new UserDetails(username, password, authorities));
                    knownUsers.remove(username);
                }
            }

            knownUsers.forEach(PropertiesFileUsersIdentityManager.this::deleteUsersByName);
            mayLog(
                    "Users's principal information loaded successfully",
                    true
            );
        } catch (FileNotFoundException e) {
            LOG.error("Cannot load users's principal information from properties file '" + file
                    + "' file do'est not exist");
        } catch (IOException e) {
            LOG.error("unexpected error happens while loading load users's " +
                    "principal information from properties file '" + file + "'", e);
        }
    }

    private void mayLog(final String message, final boolean prefix) {
        if (debug) {
            if (prefix) {
                LOG.info("[PropertiesFileUsersIdentityManager]: " + message);
            } else {
                LOG.info(message);
            }
        }
    }

    /**
     * Default thread scheduling periodic scans of the targeted file-system.
     */
    public class PropertiesFileLoaderTask extends Thread {

        private static final long SHUTDOWN_TIMEOUT = 5L;

        private final CountDownLatch shutdownLatch;
        private final CountDownLatch waitingLatch;
        private final long scanIntervalMs;
        private final Runnable runnable;

        /**
         * Creates a new {@link PropertiesFileLoaderTask} instance.
         *
         * @param scanIntervalMs the intervalSecond context.
         */
        PropertiesFileLoaderTask(final long scanIntervalMs,
                                 final Runnable runnable) {
            super(PropertiesFileLoaderTask.class.getSimpleName()+"-task-" + TASK_COUNTER.getAndIncrement());
            this.scanIntervalMs = scanIntervalMs;
            this.runnable = runnable;
            this.shutdownLatch = new CountDownLatch(1);
            this.waitingLatch = new CountDownLatch(1);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                mayLog("Starting daemon thread for reload file properties '" + file + "'", true);
                while (shutdownLatch.getCount() > 0) {
                    long started = Time.SYSTEM.milliseconds();
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        LOG.error("Unexpected error while reloading properties file.", e);
                    }

                    long timeout = Math.abs(scanIntervalMs - (Time.SYSTEM.milliseconds() - started));
                    mayLog("Waiting " + timeout +" ms to reload properties.", true);
                    boolean shuttingDown = shutdownLatch.await(timeout, TimeUnit.MILLISECONDS);
                    if (shuttingDown) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                LOG.error("Unexpected InterruptedException, ignoring: ", e);
            } finally {
                LOG.info("Stopped daemon thread for reload file properties '" + file + "'.");
                waitingLatch.countDown();
            }
        }

        void shutdown() {
            LOG.info("Shutting down thread monitoring filesystem.");
            this.shutdownLatch.countDown();
            try {
                this.waitingLatch.await(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
                LOG.error("Timeout : scan loop is not terminated yet.");
            }
        }
    }
}
