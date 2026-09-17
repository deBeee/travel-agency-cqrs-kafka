package io.github.debeee.travelagency.query;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import({MongoContainerConfiguration.class, KafkaContainerConfiguration.class})
class TestcontainersConfiguration {
}
