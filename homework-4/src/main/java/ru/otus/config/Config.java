package ru.otus.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Map;

import static java.util.UUID.randomUUID;

public class Config {
    public static final String TOPIC_EVENTS = "events-topic";
    private static final String HOST = "localhost:9091";

    public static final Map<String, Object> STREAMS_CONFIG = Map.of(
            StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, HOST,
            StreamsConfig.APPLICATION_ID_CONFIG, "appId-%s".formatted(randomUUID().hashCode()),
            StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 500);

    public static final Map<String, Object> ADMIN_CONFIG = Map.of(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, HOST);
}
