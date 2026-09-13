package io.github.debeee.travelagency.command.infrastructure.kafka.topic;

import io.github.debeee.travelagency.command.infrastructure.kafka.properties.BookingTopicProperties;
import io.github.debeee.travelagency.command.infrastructure.kafka.properties.HotelsTopicProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {
    private final BookingTopicProperties bookingTopicProperties;
    private final HotelsTopicProperties hotelsTopicProperties;

    @Bean
    public NewTopic bookingsTopic() {
        return TopicBuilder
                .name(bookingTopicProperties.name())
                .partitions(bookingTopicProperties.partitions())
                .replicas(bookingTopicProperties.replicas())
                .build();
    }

    @Bean
    public NewTopic hotelsTopic() {
        return TopicBuilder
                .name(hotelsTopicProperties.name())
                .partitions(hotelsTopicProperties.partitions())
                .replicas(hotelsTopicProperties.replicas())
                .compact()
                .build();
    }
}
