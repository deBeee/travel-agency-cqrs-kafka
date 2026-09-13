package io.github.debeee.travelagency.query;

import org.springframework.boot.SpringApplication;

public class TestTravelAgencyQuerySideApplication {

    public static void main(String[] args) {
        SpringApplication.from(TravelAgencyQuerySideApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
