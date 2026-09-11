package io.github.debeee.travelagency.command.infrastructure.kafka.topic;

import io.github.debeee.travelagency.command.infrastructure.kafka.properties.BookingTopicProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {
    private final BookingTopicProperties bookingTopicProperties;

    @Bean
    public NewTopic bookingsTopic() {
        return TopicBuilder
                .name(bookingTopicProperties.name())
                .partitions(bookingTopicProperties.partitions())
                .replicas(bookingTopicProperties.replicas())
                .build();
    }
}
