package io.github.debeee.travelagency.command;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class TravelAgencyCommandSideApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelAgencyCommandSideApplication.class, args);
    }

}
