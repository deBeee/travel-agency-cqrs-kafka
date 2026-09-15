package io.github.debeee.travelagency.query.infrastructure.configuration;

import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityWriteRepository;
import io.github.debeee.travelagency.query.application.port.out.HotelCapacityProvider;
import io.github.debeee.travelagency.query.application.port.out.HotelCapacityWriteRepository;
import io.github.debeee.travelagency.query.application.service.AvailabilityService;
import io.github.debeee.travelagency.query.application.service.HotelCapacityService;
import io.github.debeee.travelagency.query.domain.policy.AvailabilityStatusPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfiguration {

    @Bean
    public AvailabilityStatusPolicy availabilityStatusPolicy(
            @Value("${app.last-rooms-threshold}") double lastRoomsThreshold
    ) {
        return new AvailabilityStatusPolicy(lastRoomsThreshold);
    }

    @Bean
    public AvailabilityService availabilityService(
            AvailabilityWriteRepository writeRepository,
            AvailabilityReadRepository readRepository,
            HotelCapacityProvider capacityProvider,
            AvailabilityStatusPolicy statusPolicy
    ) {
        return new AvailabilityService(readRepository, writeRepository, statusPolicy, capacityProvider);
    }

    @Bean
    public HotelCapacityService hotelCapacityService(
            HotelCapacityWriteRepository capacityWriteRepository,
            AvailabilityReadRepository readRepository,
            AvailabilityWriteRepository writeRepository,
            AvailabilityStatusPolicy statusPolicy
    ) {
        return new HotelCapacityService(capacityWriteRepository, readRepository, writeRepository, statusPolicy);
    }
}
