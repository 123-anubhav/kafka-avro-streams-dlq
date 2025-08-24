package com.example;

import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.test.TestInputTopic;
import org.apache.kafka.streams.test.TestOutputTopic;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class StreamsTopologyTest {

    @Test
    void smokeTest() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");

        // Add more detailed tests in your project using TopologyTestDriver
    }
}
