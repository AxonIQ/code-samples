package io.axoniq.demo.orderfulfillment;

import io.axoniq.framework.testcontainer.AxonServerContainer;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
               properties = "spring.datasource.url=jdbc:h2:mem:order-fulfillment-it;DB_CLOSE_DELAY=-1")
@AutoConfigureTestRestTemplate
@Testcontainers
class OrderFulfillmentIT extends AbstractOrderFulfillmentTest {

    @Container
    static final AxonServerContainer AXON_SERVER = new AxonServerContainer("docker.axoniq.io/axoniq/axonserver:2026.0.0")
            .withAxonServerHostname("localhost")
            .withDevMode(true)
            .withDcbContext(true)
            .withStartupTimeout(Duration.ofMinutes(3));

    @DynamicPropertySource
    static void axonProperties(DynamicPropertyRegistry registry) {
        registry.add("axon.axonserver.servers",
                     () -> AXON_SERVER.getHost() + ":" + AXON_SERVER.getMappedPort(8124));
    }

}
