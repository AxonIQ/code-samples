package io.axoniq.demo.orderfulfillment;

import io.axoniq.demo.orderfulfillment.projection.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

abstract class AbstractOrderFulfillmentTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void orderIsDeliveredAfterAutomaticPayment() {
        var orderId = restTemplate.postForObject(
                "/orders?customerId=customer-1&email=customer-1@example.com&amount=99.95",
                null,
                String.class);
        assertThat(orderId).isNotBlank();

        // Auto-payment robot confirms shortly after the workflow registers `awaitPayment`, so the
        // order should advance through IN_TRANSIT and reach DELIVERED without a manual payment POST.
        await().atMost(45, TimeUnit.SECONDS).untilAsserted(() -> {
            var status = restTemplate.getForObject("/orders/" + orderId, OrderStatus.class);
            assertThat(status).isNotNull();
            assertThat(status.status()).isEqualTo(OrderStatus.Status.DELIVERED);
            assertThat(status.trackingNumber()).startsWith("TRK-");
        });
        assertWorkflowHistory(orderId, "COMPLETED", "shipOrder");
    }

    @Test
    void concurrentOrdersCompleteWithTheirOwnShipmentAndHistory() {
        var orderIds = IntStream.range(0, 8)
                .mapToObj(i -> restTemplate.postForObject(
                        "/orders?customerId=concurrent-" + i + "&email=customer@example.com&amount=99.95",
                        null, String.class))
                .toList();
        assertThat(orderIds).doesNotContainNull().doesNotHaveDuplicates();

        await().atMost(45, TimeUnit.SECONDS).untilAsserted(() -> {
            var shipments = orderIds.stream().map(orderId -> {
                var status = restTemplate.getForObject("/orders/" + orderId, OrderStatus.class);
                assertThat(status).as("Order %s projection", orderId).isNotNull();
                assertThat(status.status()).as("Order %s", orderId).isEqualTo(OrderStatus.Status.DELIVERED);
                assertThat(status.trackingNumber()).as("Order %s tracking number", orderId).startsWith("TRK-");
                assertThat(restTemplate.getForObject("/api/workflows/" + orderId, Map.class))
                        .containsEntry("status", "COMPLETED")
                        .containsEntry("workflowId", orderId);
                return status.trackingNumber();
            }).toList();
            assertThat(shipments).doesNotHaveDuplicates();
        });
    }

    @Test
    void unavailableStockIsProjectedAsFailed() {
        assertFailure("out-of-stock", "Out of stock");
    }

    @Test
    void missingPaymentIsProjectedAsFailed() {
        assertFailure("payment-timeout", "Payment timed out");
    }

    @Test
    void interactivePaymentWaitsForAnExplicitSignal() {
        var orderId = restTemplate.postForObject(
                "/orders?customerId=customer-1&email=customer-1@example.com&amount=99.95&scenario=manual-payment",
                null, String.class);
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var events = restTemplate.getForObject("/api/workflows/" + orderId + "/events", List.class);
            assertThat(events.stream().map(e -> ((Map<?, ?>) e).get("step") + ":" + ((Map<?, ?>) e).get("status")))
                    .contains("initiatePayment:COMPLETED");
        });
        // Longer than the robot's maximum 3.5s delay: this scenario must remain suspended.
        await().during(4, TimeUnit.SECONDS).atMost(6, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(restTemplate.getForObject("/api/workflows/" + orderId, Map.class))
                        .containsEntry("status", "STARTED"));
        restTemplate.postForEntity("/orders/" + orderId + "/payment", null, Void.class);
        assertWorkflowHistory(orderId, "COMPLETED", "shipOrder");
    }

    private void assertFailure(String scenario, String reason) {
        var orderId = restTemplate.postForObject(
                "/orders?customerId=customer-1&email=customer-1@example.com&amount=99.95&scenario=" + scenario,
                null, String.class);
        assertThat(orderId).isNotBlank();

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            var status = restTemplate.getForObject("/orders/" + orderId, OrderStatus.class);
            assertThat(status).isNotNull();
            assertThat(status.status()).isEqualTo(OrderStatus.Status.FAILED);
            assertThat(status.failureReason()).isEqualTo(reason);
            assertThat(status.trackingNumber()).isNull();
        });
        assertWorkflowHistory(orderId, "FAILED", "awaitPayment");
    }

    private void assertWorkflowHistory(String orderId, String expectedStatus, String expectedStep) {
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var history = restTemplate.getForObject("/api/workflows/" + orderId, Map.class);
            assertThat(history).containsEntry("status", expectedStatus).containsEntry("workflowId", orderId);
            var events = restTemplate.getForObject("/api/workflows/" + orderId + "/events", List.class);
            assertThat(events).isNotEmpty();
            assertThat(events.stream().map(e -> ((Map<?, ?>) e).get("step")))
                    .contains(expectedStep);
            assertThat(events.stream().map(e -> ((Map<?, ?>) e).get("step") + ":" + ((Map<?, ?>) e).get("status")))
                    .contains("awaitPayment:STARTED");
            var listing = restTemplate.getForObject("/api/workflows?search=" + orderId, Map.class);
            assertThat((List<?>) listing.get("items")).hasSize(1);
        });
    }

    @Test
    void workflowHistoryEndpointsValidateRequests() {
        assertThat(restTemplate.getForEntity("/api/workflows/missing", String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(restTemplate.getForEntity("/api/workflows?limit=0", String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(restTemplate.getForEntity("/api/workflows?offset=-1", String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(restTemplate.getForEntity("/api/workflows?status=INVALID", String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(restTemplate.getForEntity("/workflows.html", String.class).getStatusCode().value()).isEqualTo(200);
    }
}
