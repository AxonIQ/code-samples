package io.axoniq.dev.samples.orderprocessor;

import io.axoniq.dev.samples.order.api.CompleteOrderProcessCommand;
import io.axoniq.dev.samples.order.api.OrderConfirmedEvent;
import io.axoniq.dev.samples.payment.api.OrderPaidEvent;
import io.axoniq.dev.samples.payment.api.OrderPaymentCancelledEvent;
import io.axoniq.dev.samples.payment.api.PayOrderCommand;
import io.axoniq.dev.samples.shipment.api.CancelShipmentCommand;
import io.axoniq.dev.samples.shipment.api.ShipOrderCommand;
import io.axoniq.dev.samples.shipment.api.ShipmentStatus;
import io.axoniq.dev.samples.shipment.api.ShipmentStatusUpdatedEvent;
import io.axoniq.dev.samples.uuid.PaymentId;
import io.axoniq.dev.samples.uuid.ShipmentId;
import io.axoniq.dev.samples.uuid.UUIDProvider;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("OrderProcessor")
class OrderProcessor {

    private final CommandGateway commandGateway;
    private final OrderProcessRepository orderProcessRepository;

    @Autowired
    public OrderProcessor(CommandGateway commandGateway, OrderProcessRepository orderProcessRepository) {
        this.commandGateway = commandGateway;
        this.orderProcessRepository = orderProcessRepository;
    }

    @EventHandler
    public void on(OrderConfirmedEvent event, UUIDProvider uuidProvider) {
        //Send a command to paid to get the order paid.
        PaymentId paymentId = uuidProvider.generatePaymentId();
        commandGateway.send(new PayOrderCommand(paymentId));

        //Send a command to logistics to ship the order.
        ShipmentId shipmentId = uuidProvider.generateShipmentId();
        commandGateway.send(new ShipOrderCommand(shipmentId));

        OrderProcess orderProcess = new OrderProcess(event.orderId(), paymentId, shipmentId);
        orderProcessRepository.save(orderProcess);
    }

    @EventHandler
    public void on(OrderPaidEvent event) {
        orderProcessRepository.findByPaymentId(event.paymentId()).ifPresent(
                orderProcess -> {
                    orderProcess.markAsPaid();
                    if (orderProcess.orderIsDelivered()) {
                        completeOrderProcess(orderProcess);
                    }
                }
        );
    }

    @EventHandler
    public void on(OrderPaymentCancelledEvent event) {
        orderProcessRepository.findByPaymentId(event.paymentId()).ifPresent(
                orderProcess -> {
                    commandGateway.send(new CancelShipmentCommand(orderProcess.getShipmentId()));
                    completeOrderProcess(orderProcess);
                }
        );
    }

    @EventHandler
    public void on(ShipmentStatusUpdatedEvent event) {
        if (!ShipmentStatus.DELIVERED.equals(event.shipmentStatus())) {
            return;
        }

        orderProcessRepository.findByShipmentId(event.shipmentId()).ifPresent(
                orderProcess -> {
                    orderProcess.markAsDelivered();
                    if (orderProcess.orderIsPaid()) {
                        completeOrderProcess(orderProcess);
                    }
                }
        );
    }

    private void completeOrderProcess(OrderProcess orderProcess) {
        commandGateway.send(new CompleteOrderProcessCommand(
                orderProcess.getOrderId(), orderProcess.orderIsPaid(), orderProcess.orderIsDelivered()
        ));
        orderProcessRepository.delete(orderProcess);
    }
}
