package io.axoniq.demo.orderfulfillment.projection;

public record OrderStatus(
        String orderId,
        String customerId,
        String email,
        double amount,
        Status status,
        String trackingNumber,
        String originCity,
        double originLat,
        double originLng,
        String destinationCity,
        double destinationLat,
        double destinationLng,
        double currentLat,
        double currentLng,
        double progress,
        String scenario,
        String failureReason
) {

    public enum Status {
        PLACED,
        AWAITING_PAYMENT,
        IN_TRANSIT,
        DELIVERED,
        FAILED
    }

    public OrderStatus awaitingPayment() {
        return new OrderStatus(orderId, customerId, email, amount, Status.AWAITING_PAYMENT,
                               trackingNumber,
                               originCity, originLat, originLng,
                               destinationCity, destinationLat, destinationLng,
                               currentLat, currentLng, progress,
                               scenario, failureReason);
    }

    public OrderStatus dispatched(String trackingNumber) {
        return new OrderStatus(orderId, customerId, email, amount, Status.IN_TRANSIT,
                               trackingNumber,
                               originCity, originLat, originLng,
                               destinationCity, destinationLat, destinationLng,
                               originLat, originLng, 0.0,
                               scenario, failureReason);
    }

    public OrderStatus moved(double lat, double lng, double progress) {
        return new OrderStatus(orderId, customerId, email, amount, status,
                               trackingNumber,
                               originCity, originLat, originLng,
                               destinationCity, destinationLat, destinationLng,
                               lat, lng, progress,
                               scenario, failureReason);
    }

    public OrderStatus delivered() {
        return new OrderStatus(orderId, customerId, email, amount, Status.DELIVERED,
                               trackingNumber,
                               originCity, originLat, originLng,
                               destinationCity, destinationLat, destinationLng,
                               destinationLat, destinationLng, 1.0,
                               scenario, failureReason);
    }

    public OrderStatus failed(String reason) {
        return new OrderStatus(orderId, customerId, email, amount, Status.FAILED,
                               trackingNumber,
                               originCity, originLat, originLng,
                               destinationCity, destinationLat, destinationLng,
                               currentLat, currentLng, progress,
                               scenario, reason);
    }
}
