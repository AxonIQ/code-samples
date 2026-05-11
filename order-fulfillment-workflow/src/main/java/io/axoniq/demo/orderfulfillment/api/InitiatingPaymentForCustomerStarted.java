package io.axoniq.demo.orderfulfillment.api;

/**
 * Emitted automatically by the workflow engine when the {@code initiatePayment} step starts.
 * Used as a synchronisation point: by the time this event is in the store, the workflow has
 * already registered its {@code awaitPayment} wait (the wait is registered at the very top
 * of the workflow, before any {@code awaitExecute} step runs).
 */
public record InitiatingPaymentForCustomerStarted(String orderId) {
}
