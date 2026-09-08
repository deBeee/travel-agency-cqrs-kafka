package io.github.debeee.travelagency.command;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TravelAgencyCommandSideApplicationTests {

    @Test
    void contextLoads() {
    }

}
