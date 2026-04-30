# Workflow Saga — replacing the legacy `ProcessOrderSaga`

This module is a one-to-one rewrite of the [`saga`](../saga/README.md) sample, using the **Axon
Workflow Engine** instead of `@Saga`. The exact same orchestration — start on order confirmation,
request payment + shipment in parallel, react to either of two payment outcomes, complete the
process — is expressed as plain imperative Java.

## What got simpler

Side-by-side: same business behaviour, expressed two different ways.

|                                  | `saga` module (`@Saga`)                                                                                                                                                                                                                                                                                                                                                                                                                          | `workflow-saga` module (`@Workflow`)                                                                                                                                                                                                                                                                                                                |
|----------------------------------|---|---|
| Lines in the orchestration class | 107                                                                                                                                                                                                                                                                                                                                                                                                                                              | ~70                                                                                                                                                                                                                                                                                                                                                  |
| Saga state                       | 4 mutable fields (`orderId`, `shipmentId`, `orderDeadlineId`, `orderIsPaid`, `orderIsDelivered`)                                                                                                                                                                                                                                                                                                                                                 | local variables — no fields needed                                                                                                                                                                                                                                                                                                                  |
| Associating to multiple ids      | Manual `SagaLifecycle.associateWith(...)` calls + `@SagaEventHandler(associationProperty = ...)` per handler                                                                                                                                                                                                                                                                                                                                     | One `associate(payloadProperty(...), equalsTo(...))` per `waitForEvent` call                                                                                                                                                                                                                                                                        |
| Branching on outcomes            | Multiple `@SagaEventHandler` methods, with `if (orderIsPaid && orderIsDelivered)` flags; a separate `@DeadlineHandler` for the deadline; explicit `SagaLifecycle.end()` plus `deadlineManager.cancelSchedule(...)`                                                                                                                                                                                                                              | Standard Java `if`/`else`. The deadline is just `Duration.ofDays(5)` passed to `waitForEvent`. No lifecycle plumbing — when the workflow returns, it ends.                                                                                                                                                                                          |
| Race / parallelism                | Implicit, via independent event handlers and shared mutable fields                                                                                                                                                                                                                                                                                                                                                                              | Explicit, via `ctx.anyMatch(WorkflowStepResult::success, paid, paymentCancelled)`                                                                                                                                                                                                                                                                  |
| Cancellation of pending waits    | Manual: cancel deadlines via `DeadlineManager`, end saga via `SagaLifecycle.end()`                                                                                                                                                                                                                                                                                                                                                              | `delivered.cancel("payment cancelled")` and the workflow simply returns                                                                                                                                                                                                                                                                              |

The whole orchestration is one `execute(SimpleWorkflowContext ctx)` method:

```java
@Workflow(idProperty = "orderId", startOnEvent = "io.axoniq.demo.workflowsaga.api.OrderConfirmedEvent")
public void execute(SimpleWorkflowContext ctx) {
    var orderId    = (String) ctx.workflowPayload().get("orderId");
    var paymentId  = UUID.randomUUID().toString();
    var shipmentId = UUID.randomUUID().toString();

    var paid = ctx.waitForEvent("paid", OrderPaidEvent.class,
            associate(payloadProperty("paymentId"), equalsTo(paymentId)), Duration.ofDays(5));
    var cancelled = ctx.waitForEvent("paymentCancelled", OrderPaymentCancelledEvent.class,
            associate(payloadProperty("paymentId"), equalsTo(paymentId)), Duration.ofDays(5));
    var delivered = ctx.waitForEvent("delivered", ShipmentStatusUpdatedEvent.class,
            associate(payloadProperty("shipmentId"),     equalsTo(shipmentId))
                    .and(payloadProperty("shipmentStatus"), "=", DELIVERED.name()),
            Duration.ofDays(5));

    // The Started event of each awaitExecute step is the durable signal — `RequestPaymentStarted`,
    // `RequestShipmentStarted`, `CancelShipmentStarted`, `CompleteOrderStarted`. The workflow does
    // not call eventGateway.publish anywhere; the engine emits those events automatically.
    ctx.awaitExecute("requestPayment",  payload("orderId", orderId, "paymentId",  paymentId).getValues(),
                     (pc, p) -> Map.of(), STEP_TIMEOUT, apiNamespace());
    ctx.awaitExecute("requestShipment", payload("orderId", orderId, "shipmentId", shipmentId).getValues(),
                     (pc, p) -> Map.of(), STEP_TIMEOUT, apiNamespace());

    var outcome = ctx.anyMatch(WorkflowStepResult::success, paid, cancelled);
    outcome.await();

    var matched = outcome.matched().stream().map(WorkflowStepResult::getStepName).toList();
    if (matched.contains("paymentCancelled")) {
        ctx.awaitExecute("cancelShipment", payload(...).getValues(), (pc, p) -> Map.of(),
                         STEP_TIMEOUT, apiNamespace());
        delivered.cancel("payment cancelled");
        ctx.awaitExecute("completeOrder",
                         payload("orderId", orderId, "paid", false, "delivered", false).getValues(),
                         (pc, p) -> Map.of(), STEP_TIMEOUT, apiNamespace());
        return;
    }

    delivered.await();
    ctx.awaitExecute("completeOrder",
                     payload("orderId", orderId, "paid", true, "delivered", delivered.success()).getValues(),
                     (pc, p) -> Map.of(), STEP_TIMEOUT, apiNamespace());
}
```

There is no `@StartSaga`, no `@EndSaga`, no `@DeadlineHandler`, no `SagaLifecycle.associateWith`, no
serialized saga state, and no `eventGateway.publish` calls — the workflow never publishes events
itself. The engine event-sources every step and emits a Started/Completed event pair for each
`awaitExecute`; projections subscribe to those naturally-emitted events.

## Running the application

```bash
docker compose up -d
mvn spring-boot:run
```

The application listens on port **9091**. Endpoints (provided to drive the demo manually):

| Method | Path                                                | Description                                  |
|--------|-----------------------------------------------------|----------------------------------------------|
| POST   | `/orders`                                           | Confirm a new order. Returns the order id.   |
| GET    | `/orders/{orderId}`                                 | Read the projected status of the order.      |
| GET    | `/orders/{orderId}/ids`                             | Look up the workflow's payment & shipment id.|
| POST   | `/payments/{paymentId}/paid`                        | Simulate the payment system marking paid.    |
| POST   | `/payments/{paymentId}/cancel`                      | Simulate the payment system cancelling.      |
| POST   | `/shipments/{shipmentId}/status?status=DELIVERED`   | Push a shipment status update.               |

## Tests

```bash
mvn verify
```

`ProcessOrderWorkflowIT` boots the whole application against a real Axon Server (Testcontainers)
and exercises both the happy path and the payment-cancelled branch.

## Cleanup

```bash
docker compose down -v
```
