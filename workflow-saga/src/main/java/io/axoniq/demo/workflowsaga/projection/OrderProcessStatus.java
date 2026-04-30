package io.axoniq.demo.workflowsaga.projection;

public record OrderProcessStatus(
        String orderId,
        Phase phase,
        boolean paid,
        boolean delivered
) {

    public enum Phase {
        IN_PROGRESS,
        COMPLETED
    }

    public OrderProcessStatus completed(boolean paid, boolean delivered) {
        return new OrderProcessStatus(orderId, Phase.COMPLETED, paid, delivered);
    }
}
