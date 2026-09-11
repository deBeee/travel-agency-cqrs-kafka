package io.github.debeee.travelagency.command.infrastructure.kafka.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics.bookings")
public record BookingTopicProperties(
        String name,
        int partitions,
        int replicas
) {
}
