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
package io.streamthoughts.azkarra.api.model;

import org.apache.kafka.streams.TopologyDescription;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A Serializable streams topology graph.
 */
public class StreamsTopologyGraph implements Serializable {

    private final Set<GlobalStore> globalStores;

    private final Set<SubTopologyGraph> subTopologies;

    /**
     * Creates  a new {@link StreamsTopologyGraph} instance.
     *
     * @param subTopologies the {@link SubTopologyGraph} set.
     * @param globalStores  the {@link GlobalStore} set.
     */
    private StreamsTopologyGraph(final Set<SubTopologyGraph> subTopologies,
                                 final Set<GlobalStore> globalStores) {
        this.subTopologies = subTopologies;
        this.globalStores = globalStores;
    }

    public Set<SubTopologyGraph> getSubTopologies() {
        return subTopologies;
    }

    public Set<GlobalStore> getGlobalStores() {
        return globalStores;
    }

    public static class GlobalStore {

        private final SourceNode source;
        private final ProcessorNode processor;
        private final int id;

        GlobalStore(final SourceNode source,
                    final ProcessorNode processor,
                    final int id) {
            this.source = source;
            this.processor = processor;
            this.id = id;
        }

        public SourceNode getSource() {
            return source;
        }

        public ProcessorNode getProcessor() {
            return processor;
        }

        public int getId() {
            return id;
        }
    }

    public static class SubTopologyGraph implements Serializable, Comparable<SubTopologyGraph> {

        private final int id;
        private final Set<Node> nodes;

        /**
         * Creates a new {@link SubTopologyGraph} instance.
         *
         * @param id        the sub-topology id.
         * @param nodes     the sub-topology nodes.
         */
        SubTopologyGraph(final int id, final Set<Node> nodes) {
            this.id = id;
            this.nodes = nodes;
        }

        public int getId() {
            return id;
        }

        public Set<Node> getNodes() {
            return nodes;
        }

        @Override
        public int compareTo(final SubTopologyGraph that) {
            return Integer.compare(this.id, that.id);
        }
    }

    public interface Node extends Serializable, Comparable<Node> {
        enum Type {
            SOURCE, SINK, PROCESSOR
        }

        Type getType();

        String getName();

        Set<String> getSuccessors();

        Set<String> getPredecessors();

        @Override
        default int compareTo(final Node that) {
            return getName().compareTo(that.getName());
        }

    }

    /**
     * An abstract topology node.
     */
    public static class AbstractNode implements Node {

        private final Type type;

        private final String name;

        private final Set<String> predecessors;

        private final Set<String> successors;

        /**
         * Creates a new {@link AbstractNode} instance.
         *
         * @param name          the node name.
         * @param type          the node type.
         * @param predecessors  the node predecessors.
         * @param successors    the node successors.
         */
        AbstractNode(final String name,
                     final Type type,
                     final Set<String> predecessors,
                     final Set<String> successors) {
            this.type = type;
            this.successors = successors;
            this.predecessors = predecessors;
            this.name = name;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public Set<String> getPredecessors() {
            return predecessors;
        }

        public Set<String> getSuccessors() {
            return successors;
        }
    }

    /**
     * A topology source-node.
     */
    public static class SourceNode extends AbstractNode {

        private final Set<String> topics;

        SourceNode(final Set<String> topics,
                   final Set<String> successors,
                   final String name) {
            super(name, Type.SOURCE, Collections.emptySet(), new TreeSet<>(successors));
            this.topics = topics;
        }

        public Set<String> getTopics() {
            return topics;
        }
    }

    /**
     * A topology sink-node.
     */
    public static class SinkNode extends AbstractNode {

        private final String topic;

        SinkNode(final String topic,
                 final Set<String> predecessors,
                 final String name) {
            super(name, Type.SINK, new TreeSet<>(predecessors), Collections.emptySet());
            this.topic = topic;
        }

        public String getTopic() {
            return topic;
        }
    }

    /**
     * A topology processor-node.
     */
    public static class ProcessorNode extends AbstractNode {

        private final Set<String> stores;

        ProcessorNode(final Set<String> stores,
                      final Set<String> predecessors,
                      final Set<String> successors,
                      final String name) {
            super(name, Type.PROCESSOR, predecessors, successors);
            this.stores = stores;
        }

        public Set<String> getStores() {
            return stores;
        }
    }


    private static final Map<Class<? extends TopologyDescription.Node>, NodeBuilder> NODE_BUILDERS;

    static {
        NODE_BUILDERS = new HashMap<>();
        NODE_BUILDERS.put(TopologyDescription.Sink.class, new SinkNodeBuilder());
        NODE_BUILDERS.put(TopologyDescription.Source.class, new SourceNodeBuilder());
        NODE_BUILDERS.put(TopologyDescription.Processor.class, new ProcessorNodeBuilder());
    }

    interface NodeBuilder<T extends TopologyDescription.Node> {
        Node build(T node);

        static Set<String> onlyNodeNames(final Set<TopologyDescription.Node> nodes) {
            return nodes
                .stream()
                .map(TopologyDescription.Node::name)
                .collect(Collectors.toSet());
        }
    }

    public static class SinkNodeBuilder implements NodeBuilder<TopologyDescription.Sink> {

        @Override
        public Node build(final TopologyDescription.Sink node) {
            return new SinkNode(
                node.topic(),
                NodeBuilder.onlyNodeNames(node.predecessors()),
                node.name()
            );
        }
    }

    public static class ProcessorNodeBuilder implements NodeBuilder<TopologyDescription.Processor> {

        @Override
        public Node build(final TopologyDescription.Processor node) {
            return new ProcessorNode(
                node.stores(),
                NodeBuilder.onlyNodeNames(node.predecessors()),
                NodeBuilder.onlyNodeNames(node.successors()),
                node.name()
            );
        }
    }

    public static class SourceNodeBuilder implements NodeBuilder<TopologyDescription.Source> {

        @Override
        public Node build(final TopologyDescription.Source node) {
            return new SourceNode(
                    node.topicSet(),
                    NodeBuilder.onlyNodeNames(node.successors()),
                    node.name()
            );
        }
    }

    @SuppressWarnings("unchecked")
    public static StreamsTopologyGraph build(final TopologyDescription description) {

        Set<SubTopologyGraph> subtopologies = new TreeSet<>();
        for (TopologyDescription.Subtopology subtopology : description.subtopologies()) {

            Set<Node> nodes = new TreeSet<>();
            final int id = subtopology.id();

            for (TopologyDescription.Node node : subtopology.nodes()) {
                nodes.add(findBuilderForClass(node.getClass()).build(node));
            }

            subtopologies.add(new SubTopologyGraph(id, nodes));
        }

        Set<GlobalStore> globalStores = description.globalStores()
            .stream()
            .map(s ->
                new GlobalStore(
                    (SourceNode) new SourceNodeBuilder().build(s.source()),
                    (ProcessorNode) new ProcessorNodeBuilder().build(s.processor()),
                    s.id()
                )
            ).collect(Collectors.toSet());

        return new StreamsTopologyGraph(subtopologies, globalStores);
    }

    private static <T extends TopologyDescription.Node> NodeBuilder findBuilderForClass(final Class<T> cls) {
        NodeBuilder builder = NODE_BUILDERS.get(cls);
        if (builder != null) return builder;

        for (Map.Entry<Class<? extends TopologyDescription.Node>, NodeBuilder> entry : NODE_BUILDERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(cls)) {
                NODE_BUILDERS.put(cls, entry.getValue());
                return entry.getValue();
            }
        }

        throw new IllegalArgumentException("Cannot find builder for class : " + cls.getCanonicalName());
    }

}
