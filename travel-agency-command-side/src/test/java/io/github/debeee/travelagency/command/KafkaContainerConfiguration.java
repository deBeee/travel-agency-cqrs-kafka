package io.github.debeee.travelagency.command;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
}
