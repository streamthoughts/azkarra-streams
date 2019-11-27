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
package io.streamthoughts.azkarra.api.streams.admin;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.time.SystemTime;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AdminClientUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AdminClientUtils.class);

    /**
     * Creates a new {@link AdminClient} instance.
     *
     * @param config            the client admin configuration.
     * @return                  the new {@link AdminClient} instance.
     */
    public static AdminClient newAdminClient(final Conf config){
        Properties props = getClientConfig(config);
        return AdminClient.create(props);
    }

    private static Properties getClientConfig(final Conf config) {
        final Properties props = new Properties();
        for (final String adminClientConfig : AdminClientConfig.configNames()) {
            if (config.hasPath(adminClientConfig)) {
                props.put(adminClientConfig, config.getString(adminClientConfig));
            }
        }
        return props;
    }

    /**
     * Wait indefinitely for the specified topics to be created on the cluster.
     *
     * @param client        the {@link AdminClient} instance to be used.
     * @param topics        the list of topics name to be verified.
     *
     * @throws InterruptedException while waiting for response from broker.
     */
    public static void waitForTopicToExist(final AdminClient client,
                                           final Set<String> topics) throws InterruptedException {
        Set<String> missingTopics = topics;
        LOG.debug("Checking for topic(s) to be created: {}", missingTopics);
        while (true) {
            missingTopics = checkTopicsMissing(client, missingTopics);
            if (missingTopics.isEmpty())
                return;

            LOG.debug("Waiting for topic(s) to be created: {}", missingTopics);
            SystemTime.SYSTEM.sleep(Duration.ofSeconds(1));
        }
    }

    private static Set<String> checkTopicsMissing(final AdminClient client, final Set<String> topicsToVerify)
            throws InterruptedException {

        try {
            ListTopicsResult topics = client.listTopics();
            Set<String> topicNames = topics.names().get(5, TimeUnit.SECONDS);
            if (topicNames.containsAll(topicsToVerify)) {
                return Collections.emptySet();
            } else {
                Set<String> missing = new HashSet<>(topicsToVerify);
                missing.removeAll(topicNames);
                return missing;
            }
        } catch (final ExecutionException | TimeoutException e) {
            LOG.error("Error while listing topics from broker: {}", e.getMessage());
        }
        return topicsToVerify;
    }
}