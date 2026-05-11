package io.axoniq.demo.workflowsaga.projection;

import io.axoniq.demo.workflowsaga.api.CompleteOrderStarted;
import io.axoniq.demo.workflowsaga.api.OrderConfirmedEvent;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderProcessProjection {

    private final Map<String, OrderProcessStatus> orders = new ConcurrentHashMap<>();

    @EventHandler
    public void on(OrderConfirmedEvent event) {
        orders.put(event.orderId(),
                   new OrderProcessStatus(event.orderId(),
                                          OrderProcessStatus.Phase.IN_PROGRESS,
                                          false,
                                          false));
    }

    @EventHandler
    public void on(CompleteOrderStarted event) {
        orders.computeIfPresent(event.orderId(),
                                (id, current) -> current.completed(event.paid(), event.delivered()));
    }

    public Optional<OrderProcessStatus> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
