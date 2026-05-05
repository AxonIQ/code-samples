# Order Fulfillment Workflow

Sample project showing how to model an order fulfillment process using the
**Axoniq Workflow Engine**. The workflow is written as plain imperative Java —
the engine handles event sourcing, crash recovery, and audit trails.

```java
@Workflow(idProperty = "orderId", startOnEvent = "io.axoniq.demo.orderfulfillment.api.OrderPlaced")
public void execute(SimpleWorkflowContext ctx) {
    // Register the wait FIRST, so the workflow is subscribed before any payment-confirmation
    // event has a chance to land.
    var paymentConfirmation = ctx.waitForEvent("awaitPayment", PaymentConfirmed.class,
            associate(payloadProperty("orderId"), equalsTo(orderId)), Duration.ofMinutes(15));

    var reserved = ctx.awaitExecute("reserveStock", payload, Boolean.class, inventory::reserveStock);
    if (!reserved) {
        paymentConfirmation.cancel("Stock unavailable");
        ctx.fail(new RuntimeException("Stock unavailable"));
        return;
    }
    ctx.awaitExecute("initiatePayment", payload, payment::initiatePayment,
            Duration.ofSeconds(30),
            baseName("InitiatingPaymentForCustomer").namespace("io.axoniq.demo.orderfulfillment.api"));

    paymentConfirmation.await();

    // The Completed event of `shipOrder` (`ShipOrderCompleted`, in the api namespace) is what the
    // projection subscribes to — no eventGateway.publish anywhere inside the workflow.
    var shipResult = ctx.awaitExecute("shipOrder", payload, shipping::shipOrder,
            Duration.ofSeconds(30), namespace("io.axoniq.demo.orderfulfillment.api"));
    ctx.awaitExecute("notifyCustomer", Boolean.class, () -> { notifications.sendConfirmation(email); return true; });
}
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for Axon Server)
- The Axon Workflow Engine (`io.axoniq.framework.workflow:*:1.0.0-SNAPSHOT`) installed in the local Maven repository

If the workflow engine isn't published yet, build it locally first:

```bash
git clone git@github.com:AxonIQ/extension-workflow.git
cd extension-workflow
mvn clean install -DskipTests
```

## Running the application

```bash
docker compose up -d
mvn spring-boot:run
```

The application starts on port **9090**.

### REST endpoints

| Method | Path                          | Description                                 |
|--------|-------------------------------|---------------------------------------------|
| POST   | `/orders?customerId=&email=&amount=` | Place a new order. Returns the order id.    |
| POST   | `/orders/{orderId}/payment`   | Confirm payment and resume the workflow.    |
| GET    | `/orders/{orderId}`           | Read the order's projected status.          |

### Quick demo

```bash
# 1. place an order
ORDER=$(curl -s -X POST 'http://localhost:9090/orders?customerId=alice&email=alice@example.com&amount=99.95')

# 2. confirm payment
curl -X POST "http://localhost:9090/orders/$ORDER/payment"

# 3. observe the projected status
curl "http://localhost:9090/orders/$ORDER"
```

## Integration test

```bash
mvn verify
```

`OrderFulfillmentIT` boots the application against a real Axon Server (started
via Testcontainers), places an order, confirms payment, and asserts that the
projection reaches the `SHIPPED` state.

## What this sample demonstrates

* `@Workflow` with `idProperty` and `startOnEvent`
* `awaitExecute` with payload and typed return value
* `awaitExecute` with timeout and event-name customization (`baseName(...)`)
* `awaitEvent` correlated to the workflow instance via `associate(payloadProperty(...), equalsTo(...))`
* `ctx.fail(...)` to terminate a workflow with an error
* A standard `@EventHandler` projection consuming events emitted by the workflow's actions

## Cleanup

```bash
docker compose down -v
```
