package io.github.debeee.travelagency.query.infrastructure.kafka.config;

import io.github.debeee.travelagency.query.infrastructure.configuration.properties.AppTopicsProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicsConfig {
    private final AppTopicsProperties topics;

    @Bean
    public NewTopic dailyAvailabilityTopic() {
        return TopicBuilder
                .name(topics.availability())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dailyAvailabilityDltTopic() {
        return TopicBuilder
                .name(topics.availabilityDlt())
                .partitions(3)
                .replicas(1)
                .build();
    }
}