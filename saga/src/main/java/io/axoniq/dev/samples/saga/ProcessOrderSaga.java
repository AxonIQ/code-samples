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
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Saga
public class ProcessOrderSaga {

    private static final String PAYMENT_ID_ASSOCIATION = "paymentId";
    private static final String SHIPMENT_ID_ASSOCIATION = "shipmentId";
    private static final String ORDER_ID_ASSOCIATION = "orderId";
    private static final String ORDER_COMPLETE_DEADLINE = "OrderCompleteDeadline";

    private OrderId orderId;
    private ShipmentId shipmentId;
    private String orderDeadlineId;
    private boolean orderIsPaid;
    private boolean orderIsDelivered;

    @StartSaga
    @SagaEventHandler(associationProperty = ORDER_ID_ASSOCIATION)
    public void on(OrderConfirmedEvent event,
                   UUIDProvider uuidProvider,
                   CommandGateway commandGateway,
                   DeadlineManager deadlineManager) {
        this.orderId = event.orderId();

        //Send a command to paid to get the order paid. Associate this Saga with the payment identifier used.
        PaymentId paymentId = uuidProvider.generatePaymentId();
        SagaLifecycle.associateWith(PAYMENT_ID_ASSOCIATION, paymentId.toString());
        commandGateway.send(new PayOrderCommand(paymentId));

        //Send a command to logistics to ship the order. Associate this Saga with the shipment identifier used.
        ShipmentId shipmentId = uuidProvider.generateShipmentId();
        SagaLifecycle.associateWith(SHIPMENT_ID_ASSOCIATION, shipmentId.toString());
        commandGateway.send(new ShipOrderCommand(shipmentId));
        this.shipmentId = shipmentId;

        //This order should be completed in 5 days
        this.orderDeadlineId = deadlineManager.schedule(Duration.of(5, ChronoUnit.DAYS), ORDER_COMPLETE_DEADLINE);
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(OrderPaidEvent event,
                   CommandGateway commandGateway,
                   DeadlineManager deadlineManager) {
        this.orderIsPaid = true;
        if (orderIsDelivered) {
            completeOrderProcess(commandGateway, deadlineManager);
        }
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(OrderPaymentCancelledEvent event,
                   CommandGateway commandGateway,
                   DeadlineManager deadlineManager) {
        // Cancel the shipment and update the Order
        commandGateway.send(new CancelShipmentCommand(this.shipmentId));
        completeOrderProcess(commandGateway, deadlineManager);
    }

    @SagaEventHandler(associationProperty = SHIPMENT_ID_ASSOCIATION)
    public void on(ShipmentStatusUpdatedEvent event,
                   CommandGateway commandGateway,
                   DeadlineManager deadlineManager) {
        this.orderIsDelivered = ShipmentStatus.DELIVERED.equals(event.shipmentStatus());
        if (orderIsPaid && orderIsDelivered) {
            completeOrderProcess(commandGateway, deadlineManager);
        }
    }

    @DeadlineHandler(deadlineName = ORDER_COMPLETE_DEADLINE)
    @EndSaga
    public void on(CommandGateway commandGateway) {
        commandGateway.send(new CompleteOrderProcessCommand(this.orderId, this.orderIsPaid, this.orderIsDelivered));
    }

    private void completeOrderProcess(CommandGateway commandGateway,
                                      DeadlineManager deadlineManager) {
        commandGateway.send(new CompleteOrderProcessCommand(this.orderId, this.orderIsPaid, this.orderIsDelivered));
        deadlineManager.cancelSchedule(ORDER_COMPLETE_DEADLINE, orderDeadlineId);
        SagaLifecycle.end();
    }
}
