package io.axoniq.demo.orderfulfillment.controller;

import io.axoniq.demo.orderfulfillment.api.OrderPlaced;
import io.axoniq.demo.orderfulfillment.api.PaymentConfirmed;
import io.axoniq.demo.orderfulfillment.projection.OrderStatus;
import io.axoniq.demo.orderfulfillment.projection.OrderStatusProjection;
import org.axonframework.messaging.eventhandling.gateway.EventGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final EventGateway eventGateway;
    private final OrderStatusProjection projection;

    public OrderController(EventGateway eventGateway, OrderStatusProjection projection) {
        this.eventGateway = eventGateway;
        this.projection = projection;
    }

    @PostMapping
    public String placeOrder(@RequestParam("customerId") String customerId,
                             @RequestParam("email") String email,
                             @RequestParam("amount") double amount) {
        var orderId = UUID.randomUUID().toString();
        logger.info("Publishing OrderPlaced for order {}.", orderId);
        eventGateway.publish(null, new OrderPlaced(orderId, customerId, email, amount));
        return orderId;
    }

    @PostMapping("/{orderId}/payment")
    public void confirmPayment(@PathVariable("orderId") String orderId) {
        logger.info("Publishing PaymentConfirmed for order {}.", orderId);
        eventGateway.publish(null, new PaymentConfirmed(orderId, "txn-" + UUID.randomUUID()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderStatus> getStatus(@PathVariable("orderId") String orderId) {
        return projection.findById(orderId)
                         .map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }
}
