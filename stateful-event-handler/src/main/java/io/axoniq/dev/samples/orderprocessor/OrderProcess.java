package io.axoniq.dev.samples.orderprocessor;

import io.axoniq.dev.samples.uuid.OrderId;
import io.axoniq.dev.samples.uuid.PaymentId;
import io.axoniq.dev.samples.uuid.ShipmentId;

import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class OrderProcess {

    @EmbeddedId
    private OrderId orderId;
    @Embedded
    private PaymentId paymentId;
    @Embedded
    private ShipmentId shipmentId;
    private boolean orderIsPaid = false;
    private boolean orderIsDelivered = false;

    public OrderProcess(OrderId orderId, PaymentId paymentId, ShipmentId shipmentId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.shipmentId = shipmentId;
    }

    public OrderProcess() {
    }

    public void markAsPaid() {
        orderIsPaid = true;
    }

    public void markAsDelivered() {
        orderIsDelivered = true;
    }

    public boolean orderIsPaid() {
        return orderIsPaid;
    }

    public boolean orderIsDelivered() {
        return orderIsDelivered;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public ShipmentId getShipmentId() {
        return shipmentId;
    }
}
