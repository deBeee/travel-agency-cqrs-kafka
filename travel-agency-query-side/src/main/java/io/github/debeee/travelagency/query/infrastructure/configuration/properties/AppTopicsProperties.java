package io.github.debeee.travelagency.query.infrastructure.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.topics")
public record AppTopicsProperties(
        String bookings,
        String availability,
        String availabilityDlt
) {
}
