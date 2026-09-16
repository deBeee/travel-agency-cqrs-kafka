package io.github.debeee.travelagency.command;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class TravelAgencyCommandSideApplicationIT {

    @Test
    void contextLoads() {
    }
}
