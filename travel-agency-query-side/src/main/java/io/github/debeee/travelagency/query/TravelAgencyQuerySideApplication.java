package io.github.debeee.travelagency.query;

import io.github.debeee.travelagency.query.infrastructure.capacity.properties.HotelCapacityProperties;
import io.github.debeee.travelagency.query.infrastructure.configuration.properties.AppTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppTopicsProperties.class, HotelCapacityProperties.class})
public class TravelAgencyQuerySideApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelAgencyQuerySideApplication.class, args);
    }

}
