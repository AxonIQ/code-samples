# Workflow Saga — replacing the legacy `ProcessOrderSaga`

This module is a one-to-one rewrite of the [`saga`](../saga/README.md) sample, using the **Axoniq
Workflow Engine** instead of `@Saga`. The exact same orchestration — start on order confirmation,
request payment + shipment in parallel, react to either of two payment outcomes, complete the
process — is expressed as plain imperative Java.

## Comparison: `@Saga` vs `@Workflow`

Same business behaviour, expressed two different ways. Neither model is a strict
upgrade over the other — they make different trade-offs, and which one fits
better depends on the orchestration you're modelling.

|                                  | `saga` module (`@Saga`)                                                                                                                                                                                                                                                                                                                                                                                                                          | `workflow-saga` module (`@Workflow`)                                                                                                                                                                                                                                                                                                                |
|----------------------------------|---|---|
| Lines in the orchestration class | 107                                                                                                                                                                                                                                                                                                                                                                                                                                              | ~70                                                                                                                                                                                                                                                                                                                                                  |
| Saga state                       | 4 mutable fields (`orderId`, `shipmentId`, `orderDeadlineId`, `orderIsPaid`, `orderIsDelivered`)                                                                                                                                                                                                                                                                                                                                                 | local variables — no fields needed                                                                                                                                                                                                                                                                                                                  |
| Associating to multiple ids      | Manual `SagaLifecycle.associateWith(...)` calls + `@SagaEventHandler(associationProperty = ...)` per handler                                                                                                                                                                                                                                                                                                                                     | One `associate(payloadProperty(...), equalsTo(...))` per `waitForEvent` call                                                                                                                                                                                                                                                                        |
| Branching on outcomes            | Multiple `@SagaEventHandler` methods, with `if (orderIsPaid && orderIsDelivered)` flags; a separate `@DeadlineHandler` for the deadline; explicit `SagaLifecycle.end()` plus `deadlineManager.cancelSchedule(...)`                                                                                                                                                                                                                              | Standard Java `if`/`else`. The deadline is just `Duration.ofDays(5)` passed to `waitForEvent`. No lifecycle plumbing — when the workflow returns, it ends.                                                                                                                                                                                          |
| Race / parallelism                | Implicit, via independent event handlers and shared mutable fields                                                                                                                                                                                                                                                                                                                                                                              | Explicit, via `ctx.anyMatch(WorkflowStepResult::success, paid, paymentCancelled)`                                                                                                                                                                                                                                                                  |
| Cancellation of pending waits    | Manual: cancel deadlines via `DeadlineManager`, end saga via `SagaLifecycle.end()`                                                                                                                                                                                                                                                                                                                                                              | `delivered.cancel("payment cancelled")` and the workflow simply returns                                                                                                                                                                                                                                                                              |

### Where `@Workflow` reads more naturally

- The control flow is one ordinary Java method. `if`/`else`, early `return`, and
  local variables describe the orchestration directly, without translating it
  into a state machine spread across multiple handler methods.
- Correlation, timeouts, and cancellation are local to the call site
  (`waitForEvent("...", ..., Duration.ofDays(5))`, `delivered.cancel(...)`),
  rather than spread between annotations, deadline managers, and lifecycle
  calls.
- The engine event-sources every step automatically, so there's no
  `eventGateway.publish` inside the orchestration and nothing to keep in sync
  between the orchestration and a projection.

### Where `@Saga` reads more naturally

- A saga naturally decomposes into small, individually-named methods — one per
  `@SagaEventHandler`. The workflow's `execute(SimpleWorkflowContext ctx)` is a
  single ~70-line method, which is harder to scan at a glance and harder to
  unit-test in isolation. For sagas with more branches than this one, that
  single method grows fast.
- Saga state lives in named class fields, so "what does this saga remember
  between events?" is answered by reading the class. In the workflow, the
  equivalent information is encoded in local variables and the engine's stored
  state — less direct when debugging.
- `@SagaEventHandler` makes each correlation explicit at the method level,
  which is convenient when you want to grep the codebase for every place a
  particular event participates in an orchestration.
- The annotation-driven model has been the Axon Framework idiom for years —
  existing teams won't need to learn a new programming model to maintain it.

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

In the workflow model, lifecycle and persistence are handled by the engine rather than by code in
the orchestration class: there's no `@StartSaga`, `@EndSaga`, `@DeadlineHandler`,
`SagaLifecycle.associateWith`, or `eventGateway.publish`. The engine event-sources every step and
emits a Started/Completed event pair for each `awaitExecute`, which projections subscribe to.
That moves boilerplate out of the orchestration, at the cost of a single longer `execute(...)`
method — see the trade-off table above.

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
