package io.github.debeee.travelagency.command;

import io.github.debeee.travelagency.command.infrastructure.kafka.properties.BookingTopicProperties;
import io.github.debeee.travelagency.command.infrastructure.kafka.properties.OutboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableConfigurationProperties({BookingTopicProperties.class, OutboxProperties.class})
public class TravelAgencyCommandSideApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelAgencyCommandSideApplication.class, args);
    }

}
