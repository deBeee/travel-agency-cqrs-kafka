package io.github.debeee.travelagency.command;

import org.springframework.boot.SpringApplication;

public class TestTravelAgencyCommandSideApplication {

    public static void main(String[] args) {
        SpringApplication.from(TravelAgencyCommandSideApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
