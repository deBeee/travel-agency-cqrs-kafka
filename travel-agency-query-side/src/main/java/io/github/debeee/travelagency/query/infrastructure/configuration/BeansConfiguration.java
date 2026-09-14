package io.github.debeee.travelagency.query.infrastructure.configuration;

import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.application.service.AvailabilityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfiguration {

    @Bean
    public AvailabilityService availabilityService(AvailabilityReadRepository availabilityReadRepository) {
        return new AvailabilityService(availabilityReadRepository);
    }
}
