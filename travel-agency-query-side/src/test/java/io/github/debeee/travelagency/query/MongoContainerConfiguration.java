package io.github.debeee.travelagency.query;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class MongoContainerConfiguration {

    private static final DockerImageName MONGO_IMAGE = DockerImageName.parse("mongo:8.0.4");

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer(MONGO_IMAGE);
    }
}
