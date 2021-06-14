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
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Saga
public class ProcessOrderSaga {

    public static final String PAYMENT_ID_ASSOCIATION = "paymentId";

    public static final String SHIPMENT_ID_ASSOCIATION = "shipmentId";
    public static final String ORDER_ID_ASSOCIATION = "orderId";

    public static final String ORDER_COMPLETE_DEADLINE = "OrderCompleteDeadline";

    @Autowired
    private transient CommandGateway commandGateway;

    private OrderId orderId;

    private ShipmentId shipmentId;

    private String orderDeadlineId;

    private boolean orderIsPaid;

    private boolean orderIsDelivered;

    @StartSaga
    @SagaEventHandler(associationProperty = ORDER_ID_ASSOCIATION)
    public void on(OrderConfirmedEvent event, DeadlineManager deadlineManager, UUIDProvider uuidProvider) {
        this.orderId = event.getOrderId();

        //Send a command to paid to get the order paid. Associate this Saga with the payment Id used.
        PaymentId paymentId = uuidProvider.generatePaymentId();
        SagaLifecycle.associateWith(PAYMENT_ID_ASSOCIATION, paymentId.toString());
        commandGateway.send(new PayOrderCommand(paymentId));

        //Send a command to logistics to ship the order. Associate this Saga with the shipment Id used.
        ShipmentId shipmentId = uuidProvider.generateShipmentId();
        SagaLifecycle.associateWith(SHIPMENT_ID_ASSOCIATION, shipmentId.toString());
        commandGateway.send(new ShipOrderCommand(shipmentId));
        this.shipmentId = shipmentId;

        //This order should be completed in 5 days
        this.orderDeadlineId = deadlineManager.schedule(Duration.of(5, ChronoUnit.DAYS), ORDER_COMPLETE_DEADLINE);
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(OrderPaidEvent event, DeadlineManager deadlineManager) {
        this.orderIsPaid = true;
        if (orderIsDelivered) {
            completeOrderProcess(deadlineManager);
        }
    }

    @SagaEventHandler(associationProperty = PAYMENT_ID_ASSOCIATION)
    public void on(OrderPaymentCancelledEvent event, DeadlineManager deadlineManager) {
        // Cancel the shipment and update the Order
        commandGateway.send(new CancelShipmentCommand(this.shipmentId));
        completeOrderProcess(deadlineManager);
    }

    @SagaEventHandler(associationProperty = SHIPMENT_ID_ASSOCIATION)
    public void on(ShipmentStatusUpdatedEvent event, DeadlineManager deadlineManager) {
        this.orderIsDelivered = ShipmentStatus.DELIVERED.equals(event.getShipmentStatus());
        if (orderIsPaid && orderIsDelivered) {
            completeOrderProcess(deadlineManager);
        }
    }

    @DeadlineHandler(deadlineName = ORDER_COMPLETE_DEADLINE)
    @EndSaga
    public void on() {
        commandGateway.send(new CompleteOrderProcessCommand(this.orderId, this.orderIsPaid, this.orderIsDelivered));
    }

    private void completeOrderProcess(DeadlineManager deadlineManager) {
        commandGateway.send(new CompleteOrderProcessCommand(this.orderId, this.orderIsPaid, this.orderIsDelivered));
        deadlineManager.cancelSchedule(ORDER_COMPLETE_DEADLINE, orderDeadlineId);
        SagaLifecycle.end();
    }
}
