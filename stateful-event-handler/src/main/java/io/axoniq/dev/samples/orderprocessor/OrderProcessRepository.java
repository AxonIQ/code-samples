package io.axoniq.dev.samples.orderprocessor;

import io.axoniq.dev.samples.uuid.OrderId;
import io.axoniq.dev.samples.uuid.PaymentId;
import io.axoniq.dev.samples.uuid.ShipmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderProcessRepository extends JpaRepository<OrderProcess, OrderId> {
    Optional<OrderProcess> findByPaymentId(PaymentId paymentId);
    Optional<OrderProcess> findByShipmentId(ShipmentId shipmentId);
}
