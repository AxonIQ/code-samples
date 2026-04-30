package io.axoniq.demo.orderfulfillment.projection;

import io.axoniq.demo.orderfulfillment.api.InitiatingPaymentForCustomerStarted;
import io.axoniq.demo.orderfulfillment.api.OrderPlaced;
import io.axoniq.demo.orderfulfillment.api.ShipOrderCompleted;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Builds order status by listening to the externally-published {@link OrderPlaced} and to the
 * workflow engine's auto-emitted {@link InitiatingPaymentForCustomerStarted} (the Started event
 * of the {@code initiatePayment} step) and {@link ShipOrderCompleted} (the Completed event of
 * the {@code shipOrder} step).
 */
@Component
public class OrderStatusProjection {

    private final Map<String, OrderStatus> orders = new ConcurrentHashMap<>();

    @EventHandler
    public void on(OrderPlaced event) {
        orders.put(event.orderId(),
                   new OrderStatus(event.orderId(),
                                   event.customerId(),
                                   event.email(),
                                   event.amount(),
                                   OrderStatus.Status.PLACED,
                                   null));
    }

    @EventHandler
    public void on(InitiatingPaymentForCustomerStarted event) {
        orders.computeIfPresent(event.orderId(), (id, current) -> current.awaitingPayment());
    }

    @EventHandler
    public void on(ShipOrderCompleted event) {
        orders.computeIfPresent(event.orderId(),
                                (id, current) -> current.shipped(event.trackingNumber()));
    }

    public Optional<OrderStatus> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
