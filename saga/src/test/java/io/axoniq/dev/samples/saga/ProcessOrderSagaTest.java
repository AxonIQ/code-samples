package io.axoniq.dev.samples.saga;

import io.axoniq.dev.samples.order.api.CompleteOrderProcessCommand;
import io.axoniq.dev.samples.order.api.OrderConfirmedEvent;
import io.axoniq.dev.samples.payment.api.OrderPaidEvent;
import io.axoniq.dev.samples.payment.api.OrderPaymentCancelledEvent;
import io.axoniq.dev.samples.payment.api.PayOrderCommand;
import io.axoniq.dev.samples.shipment.api.CancelShipmentCommand;
import io.axoniq.dev.samples.shipment.api.ShipOrderCommand;
import io.axoniq.dev.samples.shipment.api.ShipmentStatus;
import io.axoniq.dev.samples.shipment.api.ShipmentStatusUpdatedEvent;
import io.axoniq.dev.samples.uuid.OrderId;
import io.axoniq.dev.samples.uuid.PaymentId;
import io.axoniq.dev.samples.uuid.ShipmentId;
import io.axoniq.dev.samples.uuid.UUIDProvider;
import org.axonframework.deadline.DeadlineMessage;
import org.axonframework.test.saga.SagaTestFixture;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static io.axoniq.dev.samples.saga.ProcessOrderSaga.ORDER_COMPLETE_DEADLINE;
import static org.mockito.Mockito.*;

public class ProcessOrderSagaTest {

    private final UUIDProvider uuidProviderMock = mock(UUIDProvider.class);
    private final OrderId orderId = new OrderId();
    private final PaymentId paymentId = new PaymentId();
    private final ShipmentId shipmentId = new ShipmentId();


    private SagaTestFixture<ProcessOrderSaga> testFixture;

    @BeforeEach
    void setUp() {
        testFixture = new SagaTestFixture<>(ProcessOrderSaga.class);
        testFixture.registerResource(uuidProviderMock);
        when(uuidProviderMock.generateOrderId()).thenReturn(orderId);
        when(uuidProviderMock.generatePaymentId()).thenReturn(paymentId);
        when(uuidProviderMock.generateShipmentId()).thenReturn(shipmentId);
    }

    @Test
    void onOrderConfirmedTest() {
        testFixture.givenNoPriorActivity()
                   .whenPublishingA(new OrderConfirmedEvent(orderId))
                   .expectDispatchedCommands(new PayOrderCommand(paymentId), new ShipOrderCommand(shipmentId))
                   .expectScheduledDeadlineWithName(Duration.of(5, ChronoUnit.DAYS), ORDER_COMPLETE_DEADLINE)
                   .expectActiveSagas(1);
    }

    @Test
    void onOrderPaidAndNotDeliveredTest() {
        testFixture.givenAPublished(new OrderConfirmedEvent(orderId))
                   .whenPublishingA(new OrderPaidEvent(paymentId))
                   .expectActiveSagas(1);
    }

    @Test
    void onOrderPaidAndDeliveredTest() {
        testFixture.givenAPublished(new OrderConfirmedEvent(orderId))
                   .andThenAPublished(new ShipmentStatusUpdatedEvent(shipmentId, ShipmentStatus.DELIVERED))
                   .whenPublishingA(new OrderPaidEvent(paymentId))
                   .expectDispatchedCommands(new CompleteOrderProcessCommand(orderId, true, true))
                   .expectNoScheduledDeadlineWithName(Duration.of(5, ChronoUnit.DAYS), ORDER_COMPLETE_DEADLINE)
                   .expectActiveSagas(0);
    }

    @Test
    void onOrderPaymentCancelledTest() {
        testFixture.givenAPublished(new OrderConfirmedEvent(orderId))
                   .whenPublishingA(new OrderPaymentCancelledEvent(paymentId))
                   .expectDispatchedCommands(new CancelShipmentCommand(shipmentId),
                                             new CompleteOrderProcessCommand(orderId, false, false))
                   .expectNoScheduledDeadlineWithName(Duration.of(5, ChronoUnit.DAYS), ORDER_COMPLETE_DEADLINE)
                   .expectActiveSagas(0);
    }

    @Test
    void onShipmentDeliveredAndPaidTest() {
        testFixture.givenAPublished(new OrderConfirmedEvent(orderId))
                   .andThenAPublished(new OrderPaidEvent(paymentId))
                   .andThenAPublished(new ShipmentStatusUpdatedEvent(shipmentId, ShipmentStatus.SHIPPED))
                   .whenPublishingA(new ShipmentStatusUpdatedEvent(shipmentId, ShipmentStatus.DELIVERED))
                   .expectDispatchedCommands(new CompleteOrderProcessCommand(orderId, true, true))
                   .expectNoScheduledDeadlineWithName(Duration.of(5, ChronoUnit.DAYS), ORDER_COMPLETE_DEADLINE)
                   .expectActiveSagas(0);
    }

    @Test
    void onShipmentDeliveredAndNotPaidTest() {
        testFixture.givenAPublished(new OrderConfirmedEvent(orderId))
                   .andThenAPublished(new ShipmentStatusUpdatedEvent(shipmentId, ShipmentStatus.SHIPPED))
                   .whenPublishingA(new ShipmentStatusUpdatedEvent(shipmentId, ShipmentStatus.DELIVERED))
                   .expectActiveSagas(1);
    }

    @Test
    void onShipmentInTransitTest() {
        testFixture.givenAPublished(new OrderConfirmedEvent(orderId))
                   .andThenAPublished(new OrderPaidEvent(paymentId))
                   .whenPublishingA(new ShipmentStatusUpdatedEvent(shipmentId, ShipmentStatus.SHIPPED))
                   .expectActiveSagas(1);
    }

    @Test
    void onOrderCompleteDeadlineTest() {
        testFixture.givenAPublished(new OrderConfirmedEvent(orderId))
                   .whenTimeElapses(Duration.of(5, ChronoUnit.DAYS))
                   .expectNoScheduledDeadlines()
                   .expectDeadlinesMetMatching(orderCompleteDeadline())
                   .expectDispatchedCommands(new CompleteOrderProcessCommand(orderId, false, false))
                   .expectActiveSagas(0);
    }

    protected static Matcher<List<DeadlineMessage>> orderCompleteDeadline() {
        return new OrderCompleteDeadline();
    }

    static class OrderCompleteDeadline extends TypeSafeMatcher<List<DeadlineMessage>> {

        @Override
        protected boolean matchesSafely(List<DeadlineMessage> deadlineMessages) {
            return deadlineMessages.stream().allMatch(deadlineMessage -> deadlineMessage.getDeadlineName()
                                                                                        .equals(ORDER_COMPLETE_DEADLINE)
                    && deadlineMessage.getPayload() == null);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("no matching deadline found");
        }
    }
}
