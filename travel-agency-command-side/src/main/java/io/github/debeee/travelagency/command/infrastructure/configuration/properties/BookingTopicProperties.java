package io.github.debeee.travelagency.command.infrastructure.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topic.bookings")
public record BookingTopicProperties(
        String name,
        int partitions,
        int replicas
) {
}
