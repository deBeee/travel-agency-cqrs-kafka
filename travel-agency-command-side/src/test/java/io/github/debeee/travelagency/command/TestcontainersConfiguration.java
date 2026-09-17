package io.github.debeee.travelagency.command;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import({MySqlContainerConfiguration.class, KafkaContainerConfiguration.class})
class TestcontainersConfiguration {
}
