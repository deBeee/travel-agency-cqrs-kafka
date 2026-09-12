package io.github.debeee.travelagency.command.infrastructure.kafka.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.outbox")
public record OutboxProperties(
        long pollInterval,
        int batchSize,
        int alertAfterRetries
) {
}
