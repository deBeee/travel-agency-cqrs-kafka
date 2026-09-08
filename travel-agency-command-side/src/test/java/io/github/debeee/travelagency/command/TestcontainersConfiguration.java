package io.github.debeee.travelagency.command;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:9.7.0");
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka-native:3.8.1");

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(KAFKA_IMAGE);
    }

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(MYSQL_IMAGE);
    }

}
