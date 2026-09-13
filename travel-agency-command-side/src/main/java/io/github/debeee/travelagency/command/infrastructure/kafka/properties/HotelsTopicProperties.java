package io.github.debeee.travelagency.command.infrastructure.kafka.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics.hotels")
public record HotelsTopicProperties(String name, int partitions, int replicas) {
}
