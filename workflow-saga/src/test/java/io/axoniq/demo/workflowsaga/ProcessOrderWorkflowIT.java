package io.axoniq.demo.workflowsaga;

import io.axoniq.demo.workflowsaga.projection.IdRegistry;
import io.axoniq.demo.workflowsaga.projection.OrderProcessStatus;
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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class ProcessOrderWorkflowIT {

    @Container
    static final AxonServerContainer AXON_SERVER = new AxonServerContainer()
            .withAxonServerHostname("localhost")
            .withDevMode(true)
            .withDcbContext(true);

    @DynamicPropertySource
    static void axonProperties(DynamicPropertyRegistry registry) {
        registry.add("axon.axonserver.servers",
                     () -> AXON_SERVER.getHost() + ":" + AXON_SERVER.getMappedPort(8124));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void happy_path_paid_then_delivered() {
        var orderId = restTemplate.postForObject("/orders", null, String.class);
        assertThat(orderId).isNotBlank();

        var ids = await().atMost(20, TimeUnit.SECONDS).until(
                () -> restTemplate.getForObject("/orders/" + orderId + "/ids", IdRegistry.Ids.class),
                i -> i != null && i.paymentId() != null && i.shipmentId() != null);

        restTemplate.postForEntity("/payments/" + ids.paymentId() + "/paid", null, Void.class);
        restTemplate.postForEntity("/shipments/" + ids.shipmentId() + "/status?status=DELIVERED", null, Void.class);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var status = restTemplate.getForObject("/orders/" + orderId, OrderProcessStatus.class);
            assertThat(status).isNotNull();
            assertThat(status.phase()).isEqualTo(OrderProcessStatus.Phase.COMPLETED);
            assertThat(status.paid()).isTrue();
            assertThat(status.delivered()).isTrue();
        });
    }

    @Test
    void payment_cancelled_triggers_shipment_cancellation() {
        var orderId = restTemplate.postForObject("/orders", null, String.class);
        assertThat(orderId).isNotBlank();

        var ids = await().atMost(20, TimeUnit.SECONDS).until(
                () -> restTemplate.getForObject("/orders/" + orderId + "/ids", IdRegistry.Ids.class),
                i -> i != null && i.paymentId() != null && i.shipmentId() != null);

        restTemplate.postForEntity("/payments/" + ids.paymentId() + "/cancel", null, Void.class);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var status = restTemplate.getForObject("/orders/" + orderId, OrderProcessStatus.class);
            assertThat(status).isNotNull();
            assertThat(status.phase()).isEqualTo(OrderProcessStatus.Phase.COMPLETED);
            assertThat(status.paid()).isFalse();
            assertThat(status.delivered()).isFalse();
        });
    }
}
