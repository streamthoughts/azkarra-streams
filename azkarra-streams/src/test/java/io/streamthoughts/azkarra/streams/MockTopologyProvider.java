package io.streamthoughts.azkarra.streams;

import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import org.apache.kafka.streams.Topology;

public class MockTopologyProvider implements TopologyProvider {

    @Override
    public String version() {
        return "0";
    }

    @Override
    public Topology get() {
        return null;
    }
}
