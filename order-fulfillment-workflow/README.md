# Order Fulfillment Workflow

An order fulfillment demo using **Axoniq Workflows** and Spring Boot 4.
The workflow reserves stock, initiates payment, waits for confirmation, ships the
order, and notifies the customer. The live map shows shipments moving between
cities over server-sent events. A second UI at `/workflows.html` inspects persisted
workflow executions, step state, and the complete workflow event timeline.

## Dependencies

This sample uses Axoniq Framework artifacts from Maven Central:

- `io.axoniq.framework:axoniq-workflow-dsl` provides the Java DSL and workflow engine.
- `io.axoniq.framework:axoniq-spring-boot-starter` provides workflow infrastructure.
  This sample registers its `WorkflowModule` explicitly to configure history storage.
  The former workflow-specific Spring Boot artifact is no longer used.
- `io.axoniq.framework:axoniq-workflow-test` provides the workflow test fixture.
- Axon Framework and Axoniq Framework versions are pinned in [`pom.xml`](pom.xml).

Java 21+, Maven 3.9+, and Docker are required to run the complete demo.

## Workflow API

The workflow uses imports under `io.axoniq.framework.workflow`. Its entry point
reads as the order process, with step implementation in private methods below it:

```java
@Workflow(idProperty = "orderId",
          startOnEventClass = OrderPlaced.class,
          workflowName = "OrderFulfillmentWorkflow")
public void execute(SimpleWorkflowContext ctx) {
    var paymentConfirmation = registerPaymentWait(ctx);

    if (!reserveStock(ctx)) {
        rejectOutOfStock(ctx, paymentConfirmation);
        return;
    }

    awaitPayment(ctx, paymentConfirmation);
    var trackingNumber = shipOrder(ctx);
    notifyCustomer(ctx, trackingNumber);
}
```

[`OrderFulfillmentWorkflow`](src/main/java/io/axoniq/demo/orderfulfillment/workflow/OrderFulfillmentWorkflow.java)
keeps payload mapping, timeouts, and failure handling in those methods. Payment
and shipping share a `serviceStep` helper for their timeout and event namespace.
Their event names and payloads remain stable for the payment robot, shipment
listeners, and UI. All step IDs remain stable for history and replay.

The payment wait is registered without blocking, before any action that could
trigger a response. The `awaitPayment` helper initiates payment with nonblocking
`execute`, joins both results with `allMatch(WorkflowStepResult::success, ...)`,
and returns only after both succeed. Either can finish first. A failure or timeout
ends the join, cancels any unfinished companion step, and records the failure.
Its association helpers come from
`runtime.association.Associations.associate` and
`dsl.api.EventAssociationsUtils.equalsTo` / `payloadProperty` within the workflow
package.

`awaitExecute` with an input payload returns the action's result map. The
three-argument `awaitExecute(name, resultType, supplier)` convenience is used for
customer notification.

The engine's `InitiatingPaymentForCustomerStarted` and `ShipOrderCompleted`
events drive the projection, SSE stream, and simulators. Payment's Started event
includes the scenario, so the robot can skip forced timeouts without depending on
projection timing. Shipping generates its tracking number inside the execute
action and returns it in the recorded result; replay and customer notification
therefore use the same number.

Failures use `awaitPublish` to publish `OrderFailed` as a durable workflow step,
then `ctx.fail(...)` to terminate the workflow. A completed publish is not
repeated during replay.

## Run

From this directory:

```bash
docker compose up -d
mvn spring-boot:run
```

Open [the demo UI](http://localhost:9090). Axon Server is available at
[localhost:8024](http://localhost:8024).
Compose uses a dedicated project name to keep its data
separate from older versions of this demo. It persists Axon Server's data, events,
and replication log so container recreation retains all three.
Open the [workflow console](http://localhost:9090/workflows.html) directly or follow
the link in the map header. Its waterfall shows actual step intervals, overlapping
event waits, and failures. Select a span for its inputs, output, and lifecycle
events; open the event journal for the full payload and metadata.

For a live walkthrough, choose **Interactive payment** and **Run workflow**. Watch
the waiting span grow, then click **Confirm payment** to see shipping and
notification complete. The wait has a 45-second deadline. **Automatic payment**,
**Payment timeout**, and **Out of stock** show the other paths.

Switch between **Waterfall**, **BPMN**, and **Event journal**. Use **+ / − / Fit**
to zoom the waterfall or BPMN diagram. **Replay trace** reconstructs state one
recorded event at a time, with enough playback time to see even short actions.
The slider and **Previous / Next event** controls move through the same event
cursor. Every view, including the inspector, shows only the state known at that
cursor: future steps are pending, results appear upon completion, and rewinding
removes them. Source timestamps still determine actual durations. Replay executes
no workflow actions. The
**Live** button pauses or resumes incoming updates. At narrower widths, the step
inspector opens as a drawer; close it with **×** or **Escape**.

The BPMN view uses the locally bundled [bpmn-js viewer](https://bpmn.io/toolkit/bpmn-js/walkthrough/)
and [`order-fulfillment.bpmn`](src/main/resources/static/order-fulfillment.bpmn).
This illustrative BPMN 2.0 model is authored alongside the Java workflow; it is
not generated by the framework or used to execute the workflow. It includes the
parallel payment wait, stock decision, deadline, and failure paths. State colors
come from the same history cursor as the waterfall. Unvisited branches become
**Not taken** once the execution finishes. The XML can be downloaded from the view.

The delivery map uses standard OpenStreetMap tiles without an API key and keeps
visible attribution and normal browser caching. Only the basemap is tinted for
the dark theme; route and truck colors are unchanged. See the
[tile usage policy](https://operations.osmfoundation.org/policies/tiles/) for hosting details.

Use a fresh demo event store when switching from the old `0.1.0` sample. This
sample changes the recorded workflow steps and does not migrate existing workflow
histories. The map's order projection and its processor tokens remain in memory;
the truck and payment simulators are demo components. Workflow history and its
processor token are stored in embedded, file-backed H2.

## Extending the framework history component

Start with [`WorkflowHistoryConfig`](src/main/java/io/axoniq/demo/orderfulfillment/config/WorkflowHistoryConfig.java).
The sample keeps the framework's standard `WorkflowHistoryProjector` and configures
its storage and processor through two module extension points:

```java
WorkflowModule.defaults("OrderFulfillment", SimpleWorkflowContext.class)
    .definition(definitions -> definitions.autodetected(configuration -> workflow))
    .withHistory(configuration -> {
        history.initialize(configuration.getComponent(UnitOfWorkFactory.class));
        return history;
    })
    .historyProcessorConfiguration(processor -> processor
        .tokenStore(historyTokens)
        .initialSegmentCount(1)
        .batchSize(1)
        .withInterceptor(history));
```

`axon.workflow.enabled=false` disables automatic workflow module registration;
Spring registers the explicit module bean instead. It still autodetects the
workflow definition's `@Workflow` method.

[`JdbcWorkflowHistoryRepository`](src/main/java/io/axoniq/demo/orderfulfillment/history/JdbcWorkflowHistoryRepository.java)
implements `MutableWorkflowHistoryRepository`. Its processor interceptor provides
the current event to `save(WorkflowHistory)`, because that repository method only
receives the resulting state. It stores the event and a UI document in one H2
transaction. It uses no business `@EventHandler` methods and does not subclass the
projector.

The event journal has two purposes: it supplies the inspection timeline, and it
restores the framework's actual state for further projection after a restart. The
projector requires `EventSourcedWorkflowState`; it cannot continue
from an arbitrary UI DTO. The repository reconstructs that state by replaying its
stored events through a fresh standard projector and a temporary in-memory repository.
This applies recorded state changes only; it does not execute workflow actions.

The durable data consists of:

| H2 table | Contents |
|---|---|
| `workflow_history` | Latest workflow status, payload, version, and step state for the UI. |
| `workflow_history_event` | Workflow event IDs, type/version, time, payload, and metadata, in recorded order. |
| `TokenEntry` | The history processor's durable progress and segment claim. |

The demo processes history one event per batch. Event and state writes commit
before the processor advances its token. They are separate transactions, so an
interrupted batch may be delivered again. Unique
event IDs and duplicate checks make that redelivery a no-op. A failed history
write rolls back both the event and UI state. The original workflow events and
execution recovery remain in Axon Server; H2 is the inspection read model.

### Live history updates

`WorkflowHistoryConfig` supplies `updates::publish` to the repository. An
`afterCommit` callback sends the workflow ID to
[`WorkflowHistoryUpdates`](src/main/java/io/axoniq/demo/orderfulfillment/history/WorkflowHistoryUpdates.java).
This component coalesces IDs and broadcasts SSE invalidations at
`/api/workflows/stream`. It observes committed repository writes, using no additional
business event handlers. Rolled-back writes and duplicate deliveries send nothing.
Network sends run off the projection thread.

The browser reloads the persisted state and incrementally reads new journal events
after an invalidation. On reconnect it catches up from the same journal; SSE is
only a notification channel, not a second history store. Heartbeats maintain the
connection, with a 30-second reconciliation fetch as a fallback. Running spans
animate against the server clock between updates; completed spans use their
recorded end time. Events without a recorded start appear as diamonds, not invented
durations. Waiting-time totals count overlapping intervals once.

**Extension scope:** the mutable history repository and projector are marked `@Internal`
in the released framework. This is a version-specific extension example, not a
framework-provided JDBC implementation. The interceptor relies on the synchronous
history projector and checks that assumption. Reconstructing state reads all recorded
events for one workflow on each update; this is suitable for short demo workflows.
The framework query API uses its query matcher over reconstructed states, while
the UI reads persisted documents directly with SQL filtering and pagination.

## Persistence and restart demo

The default database is `.data/workflow-history.mv.db`, relative to the process's
working directory. Start the sample from this directory to keep its location
consistent. Override `spring.datasource.url` to select another file. No separate
database container is required.

1. Run a few scenarios at `/workflows.html`, including a failure.
2. Stop the application, keeping Axon Server and `.data` intact.
3. Start the application from the same directory and revisit `/workflows.html`.
4. The execution list and timelines are still available; the history processor
   resumes from its stored token. New events continue updating the same history.

The timeline shows workflow lifecycle and step events. The independent truck
location feed remains on the live map; workflow completion precedes truck delivery.
Keep Axon Server's data and the H2 file together. When resetting this demo to an
empty event store, also remove `.data` while the application is stopped.

## REST endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/orders?customerId=&email=&amount=&scenario=` | Place an order; returns its ID. |
| POST | `/orders/{orderId}/payment` | Manually confirm payment. |
| GET | `/orders/{orderId}` | Read the projected order status. |
| GET | `/orders/stream` | Subscribe to the initial snapshot and live SSE updates. |
| GET | `/api/workflows?search=&status=&limit=50&offset=0` | Page through persisted executions; search matches workflow ID. |
| GET | `/api/workflows/stream` | SSE notifications after history commits; reconnect and read the journal to catch up. |
| GET | `/api/workflows/{workflowId}` | Read persisted workflow and step state. |
| GET | `/api/workflows/{workflowId}/events?after=0&limit=200` | Read the workflow timeline; `after` is the last returned sequence. |
| POST | `/simulate/single?scenario=` | Place a random order. |
| POST | `/simulate/burst?count=10&scenario=` | Place a burst of random orders. |
| POST | `/simulate/scenario?type=` | Run a selected scenario. |

The default `happy` scenario automatically confirms payment and progresses
through `PLACED`, `AWAITING_PAYMENT`, `IN_TRANSIT`, and `DELIVERED`.
`out-of-stock` fails before payment initiation. `payment-timeout` disables the
payment robot for that order and fails after four seconds; other orders have a
45-second payment deadline. `manual-payment` also disables the robot and waits for
an explicit payment confirmation from the console or REST endpoint.

```bash
# Place an order; the payment robot confirms it automatically.
ORDER=$(curl -s -X POST 'http://localhost:9090/orders?customerId=alice&email=alice@example.com&amount=99.95')
curl "http://localhost:9090/orders/$ORDER"

# Exercise the two failure paths.
curl -X POST 'http://localhost:9090/simulate/scenario?type=out-of-stock'
curl -X POST 'http://localhost:9090/simulate/scenario?type=payment-timeout'
```

The workflow completes after shipping and notification. Truck movement and final
delivery are simulated separately, so `DELIVERED` appears a few seconds later.

## Tests

```bash
# Workflow fixture and Spring application tests; no Docker required.
mvn test

# Also run the application against an Axon Server Testcontainer.
mvn verify

# Waterfall timing, state replay, and BPMN branch mapping (Node.js 20+).
node --test src/test/js/workflow-trace.test.mjs
```

`OrderFulfillmentWorkflowTest` uses the released `WorkflowTestFixture` and a
manual clock to verify normal fulfillment, payment arriving before initiation,
correlation to the correct order, timeout, and stock failure.
`OrderFulfillmentApplicationTest` checks automatic delivery and the projected
reasons for both failure scenarios through the HTTP API with an in-memory event
store, including the history endpoints, scenario timelines, and an interactive
payment that remains suspended until explicitly confirmed. Tests use isolated
databases and never touch the demo's `.data` directory. `OrderFulfillmentIT` runs
the same HTTP scenarios against Axon Server.

`JdbcWorkflowHistoryRepositoryTest` closes and reopens a real H2 file, continues an
unfinished projection, checks failure/retry/version state, verifies duplicate
delivery is harmless, and verifies rollback and retry after a failed projection.
It also verifies that live notifications happen only after successful commits.

## Cleanup

```bash
# Removes this demo's containers and stored events.
docker compose down -v
# With the application stopped, also remove its history and processor token.
rm -rf .data
```
