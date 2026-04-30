package io.axoniq.demo.orderfulfillment;

import io.axoniq.demo.orderfulfillment.projection.OrderStatus;
import io.axoniq.framework.testcontainer.AxonServerContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class OrderFulfillmentIT {

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

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void test_order_fulfillment_process() {
        var orderId = restTemplate.postForObject(
                "/orders?customerId=customer-1&email=customer-1@example.com&amount=99.95",
                null,
                String.class);
        assertThat(orderId).isNotBlank();

        // Wait for the projection to observe the workflow's `InitiatingPaymentForCustomerStarted`
        // event — this proves the `awaitPayment` wait has been registered before we post the
        // PaymentConfirmed event, eliminating a race in the previous version of this test.
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            var status = restTemplate.getForObject("/orders/" + orderId, OrderStatus.class);
            assertThat(status).isNotNull();
            assertThat(status.status()).isEqualTo(OrderStatus.Status.AWAITING_PAYMENT);
        });

        restTemplate.postForEntity("/orders/" + orderId + "/payment", null, Void.class);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var status = restTemplate.getForObject("/orders/" + orderId, OrderStatus.class);
            assertThat(status).isNotNull();
            assertThat(status.status()).isEqualTo(OrderStatus.Status.SHIPPED);
            assertThat(status.trackingNumber()).startsWith("TRK-");
        });
    }
}
