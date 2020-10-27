package io.axoniq.distributedexceptions;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("command,rest")
public class DistributedExceptionsApplicationTests {

    @Test
    void contextLoads() {

    }

}
