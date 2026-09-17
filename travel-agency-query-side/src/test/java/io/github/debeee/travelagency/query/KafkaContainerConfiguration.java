package io.github.debeee.travelagency.query;

import io.github.debeee.travelagency.query.infrastructure.configuration.properties.AppTopicsProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaContainerConfiguration {

    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka-native:3.8.1");

    @Bean
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(KAFKA_IMAGE);
    }
    
    @Bean
    DynamicPropertyRegistrar kafkaPropertiesRegistrar(KafkaContainer kafkaContainer) {
        return registry -> registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }
    
    @Bean
    NewTopic upstreamBookingsTopic(AppTopicsProperties topics) {
        return TopicBuilder.name(topics.bookings()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic upstreamHotelsTopic(AppTopicsProperties topics) {
        return TopicBuilder.name(topics.hotels()).partitions(3).replicas(1).compact().build();
    }
}
