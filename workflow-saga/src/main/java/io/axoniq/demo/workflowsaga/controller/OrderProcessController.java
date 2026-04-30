package io.axoniq.demo.workflowsaga.controller;

import io.axoniq.demo.workflowsaga.api.OrderConfirmedEvent;
import io.axoniq.demo.workflowsaga.api.OrderPaidEvent;
import io.axoniq.demo.workflowsaga.api.OrderPaymentCancelledEvent;
import io.axoniq.demo.workflowsaga.api.ShipmentStatus;
import io.axoniq.demo.workflowsaga.api.ShipmentStatusUpdatedEvent;
import io.axoniq.demo.workflowsaga.projection.IdRegistry;
import io.axoniq.demo.workflowsaga.projection.OrderProcessProjection;
import io.axoniq.demo.workflowsaga.projection.OrderProcessStatus;
import org.axonframework.messaging.eventhandling.gateway.EventGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class OrderProcessController {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessController.class);

    private final EventGateway eventGateway;
    private final OrderProcessProjection projection;
    private final IdRegistry idRegistry;

    public OrderProcessController(EventGateway eventGateway,
                                  OrderProcessProjection projection,
                                  IdRegistry idRegistry) {
        this.eventGateway = eventGateway;
        this.projection = projection;
        this.idRegistry = idRegistry;
    }

    @PostMapping("/orders")
    public String confirmOrder() {
        var orderId = UUID.randomUUID().toString();
        logger.info("Publishing OrderConfirmedEvent for order {}.", orderId);
        eventGateway.publish(null, new OrderConfirmedEvent(orderId));
        return orderId;
    }

    @PostMapping("/payments/{paymentId}/paid")
    public void markPaid(@PathVariable("paymentId") String paymentId) {
        eventGateway.publish(null, new OrderPaidEvent(paymentId));
    }

    @PostMapping("/payments/{paymentId}/cancel")
    public void cancelPayment(@PathVariable("paymentId") String paymentId) {
        eventGateway.publish(null, new OrderPaymentCancelledEvent(paymentId));
    }

    @PostMapping("/shipments/{shipmentId}/status")
    public void updateShipmentStatus(@PathVariable("shipmentId") String shipmentId,
                                     @RequestParam("status") ShipmentStatus status) {
        eventGateway.publish(null, new ShipmentStatusUpdatedEvent(shipmentId, status));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderProcessStatus> getStatus(@PathVariable("orderId") String orderId) {
        return projection.findById(orderId)
                         .map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/orders/{orderId}/ids")
    public ResponseEntity<IdRegistry.Ids> ids(@PathVariable("orderId") String orderId) {
        return idRegistry.forOrder(orderId)
                         .map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }
}
