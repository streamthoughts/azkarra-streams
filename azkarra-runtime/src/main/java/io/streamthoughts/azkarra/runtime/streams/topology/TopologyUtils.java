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
package io.streamthoughts.azkarra.runtime.streams.topology;

import org.apache.kafka.streams.TopologyDescription;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TopologyUtils {

    private static final String REPARTITION_TOPIC_SUFFIX = "-repartition";
    private static final String CHANGELOG_TOPIC_SUFFIX = "-changelog";

    /**
     * When the topology defines a non-key joining operation on KTable, the KafkaStreams
     * instance will create two internals topics used to re-keyed records.
     */
    private static final String FOREIGN_KEY_SUBSCRIPTION_TOPIC_SUFFIX = "-topic";
    private static final String FOREIGN_KEY_SUBSCRIPTION_RESPONSE_SUFFIX = "-subscription-response-topic";
    private static final String FOREIGN_KEY_SUBSCRIPTION_REGISTRATION_SUFFIX = "-subscription-registration-topic";

    /**
     * This pattern is used to match internal topic with increment index in the form of
     * "KSTREAM-AGGREGATE-STATE-STORE-0000000001-(repartition|changelog|topic)".
     */
    private static final String INTERNAL_TOPIC_PATTERNS = ".*-[0-9]{10}(?:"
            + REPARTITION_TOPIC_SUFFIX
            + "|" + CHANGELOG_TOPIC_SUFFIX
            + "|" + FOREIGN_KEY_SUBSCRIPTION_TOPIC_SUFFIX
            + ")$";

    /**
     * Pattern for identifying internal topics created for repartitioning or changelog purpose.
     */
    private static final Pattern INTERNAL_TOPIC_NAME_PATTERN = Pattern.compile(INTERNAL_TOPIC_PATTERNS);

    public static Set<String> getSourceTopics(final TopologyDescription topology) {
        final Set<String> topics = new HashSet<>();
        topology.globalStores().forEach(s -> {
            topics.addAll(s.source().topicSet());
        });
        topology.subtopologies().forEach(sub ->
            sub.nodes().forEach(n -> {
                if (n instanceof TopologyDescription.Source) {
                    topics.addAll(((TopologyDescription.Source) n).topicSet());
                }
            })
        );
        return topics;
    }

    public static Set<String> getSinkTopics(final TopologyDescription topology) {
        final Set<String> topics = new HashSet<>();
        topology.subtopologies().forEach(sub ->
            sub.nodes().forEach(n -> {
                if (n instanceof TopologyDescription.Sink) {
                    topics.add(((TopologyDescription.Sink) n).topic());
                }
            })
        );
        return topics;
    }

    public static Set<String> getUserDeclaredTopics(final TopologyDescription topology) {

        final Set<String> topics = new HashSet<>();
        topics.addAll(getSourceTopics(topology));
        topics.addAll(getSinkTopics(topology));

        return topics.stream()
            .filter(Predicate.not(TopologyUtils::isInternalTopic))
            .collect(Collectors.toSet());
    }

    /**
     * Checks whether the specified topic is internal for the given application id.
     *
     * @param applicationId the id of the application.
     * @param topic         the name of the topic.
     *
     * @return              {@code true} if topic is internal, otherwise {@code false}.
     */
    public static boolean isInternalTopic(final String applicationId,
                                          final String topic) {
        return checkInternalTopics(applicationId, topic);
    }

    /**
     * Checks whether the specified topic is internal.
     *
     * @param topic         the name of the topic.
     *
     * @return              {@code true} if topic is internal, otherwise {@code false}.
     */
    public static boolean isInternalTopic(final String topic) {
        return checkInternalTopics(null, topic);
    }

    private static boolean checkInternalTopics(final String applicationId, final String topic) {

        return (applicationId == null || topic.startsWith(applicationId)) &&
                (INTERNAL_TOPIC_NAME_PATTERN.matcher(topic).matches()
                || topic.endsWith(CHANGELOG_TOPIC_SUFFIX)
                || topic.endsWith(REPARTITION_TOPIC_SUFFIX)
                || topic.endsWith(FOREIGN_KEY_SUBSCRIPTION_RESPONSE_SUFFIX)
                || topic.endsWith(FOREIGN_KEY_SUBSCRIPTION_REGISTRATION_SUFFIX));
    }
}
