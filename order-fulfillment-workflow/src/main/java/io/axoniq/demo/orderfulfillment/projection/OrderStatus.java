package io.axoniq.demo.orderfulfillment.projection;

public record OrderStatus(
        String orderId,
        String customerId,
        String email,
        double amount,
        Status status,
        String trackingNumber
) {

    public enum Status {
        PLACED,
        AWAITING_PAYMENT,
        SHIPPED
    }

    public OrderStatus awaitingPayment() {
        return new OrderStatus(orderId, customerId, email, amount, Status.AWAITING_PAYMENT, trackingNumber);
    }

    public OrderStatus shipped(String trackingNumber) {
        return new OrderStatus(orderId, customerId, email, amount, Status.SHIPPED, trackingNumber);
    }
}
