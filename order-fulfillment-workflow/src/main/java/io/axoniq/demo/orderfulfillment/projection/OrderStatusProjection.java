package io.axoniq.demo.orderfulfillment.projection;

import io.axoniq.demo.orderfulfillment.api.InitiatingPaymentForCustomerStarted;
import io.axoniq.demo.orderfulfillment.api.OrderDelivered;
import io.axoniq.demo.orderfulfillment.api.OrderFailed;
import io.axoniq.demo.orderfulfillment.api.OrderPlaced;
import io.axoniq.demo.orderfulfillment.api.ShipOrderCompleted;
import io.axoniq.demo.orderfulfillment.api.TruckLocationUpdated;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Builds order status from workflow Started/Completed events plus the simulator's truck-movement
 * events. The projection is the system of record for the live UI — every status change here is
 * what the SSE stream broadcasts.
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
                                   null,
                                   event.originCity(),
                                   event.originLat(),
                                   event.originLng(),
                                   event.destinationCity(),
                                   event.destinationLat(),
                                   event.destinationLng(),
                                   event.originLat(),
                                   event.originLng(),
                                   0.0,
                                   event.scenario(),
                                   null));
    }

    @EventHandler
    public void on(InitiatingPaymentForCustomerStarted event) {
        orders.computeIfPresent(event.orderId(), (id, current) -> current.awaitingPayment());
    }

    @EventHandler
    public void on(ShipOrderCompleted event) {
        orders.computeIfPresent(event.orderId(),
                                (id, current) -> current.dispatched(event.trackingNumber()));
    }

    @EventHandler
    public void on(TruckLocationUpdated event) {
        orders.computeIfPresent(event.orderId(),
                                (id, current) -> current.moved(event.lat(), event.lng(), event.progress()));
    }

    @EventHandler
    public void on(OrderDelivered event) {
        orders.computeIfPresent(event.orderId(), (id, current) -> current.delivered());
    }

    @EventHandler
    public void on(OrderFailed event) {
        orders.computeIfPresent(event.orderId(), (id, current) -> current.failed(event.reason()));
    }

    public Optional<OrderStatus> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Collection<OrderStatus> findAll() {
        return orders.values();
    }
}
