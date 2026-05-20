# Workflow Versioning Demo

Live demo of definition-level workflow versioning against a real Axon Server. Two workflow definitions
share the same `(workflowName, startOnEventName, idProperty)` triple and differ only by `version`. New
starts go to the highest registered version; in-flight instances replay on the version they were
started under (read from the started event's `MessageType.version()`).

## What this shows

Two `@Workflow` classes, both registered as Spring components:

- **`OrderWorkflow`** — `@Workflow(version = "1.0.0")`. Steps: `reserveStock` → `chargePayment` →
  wait for payment → `notifyCustomer`. Includes a commented-out `ctx.version("address-validation", "0.0.2")`
  block you can re-enable to demonstrate the additive-change primitive within the same definition.
- **`OrderWorkflowV2`** — `@Workflow(version = "2.0.0")`. Copy-pasted from `OrderWorkflow` with a
  structurally different body: `validateAddress` always runs, non-premium orders go through a
  `fraudReview` step.

While both are registered, the runtime:

- Spawns every new instance on the highest version (`2.0.0` → `OrderWorkflowV2`).
- Replays any in-flight `1.0.0` instances on `OrderWorkflow` — the engine picks the matching
  definition by looking up the version recorded in the workflow's started event metadata
  (`MessageType.version()` via AF5's native event-versioning).
- If a NEW start arrives for an `orderId` whose v1.0.0 instance is still in flight, the engine
  augments the new workflow id with the version suffix (`<orderId>#2.0.0`) so v1 and v2 can run in
  parallel for the same domain key.
- Stamps every event the workflow emits with the workflow's current version on its `MessageType`,
  so you can see which definition each event belongs to directly in the Axon Server UI.

## Prerequisites

- Java 21+
- Docker (for Axon Server)
- A local `axon-flow-spec` build: from that repo run `./mvnw install -DskipTests` so the
  `0.1.1-SNAPSHOT` artifacts are in your local Maven repo.

## Run the demo

```bash
docker compose up -d              # starts Axon Server on http://localhost:8024
./mvnw spring-boot:run            # starts the app
```

App listens on `http://localhost:9099`. Axon Server UI at `http://localhost:8024`.

### Test UI

Open `http://localhost:9099/` for a small dashboard that places orders, sends payments, and
inspects workflow state. The `GET /orders/{orderId}` response is a list of `instances` — one entry
per workflow id matching the `orderId` (the base id, plus any version-disambiguated siblings).

## Lifecycle walkthrough

### Step 1 — Start a v1.0.0 workflow, then add OrderWorkflowV2

Comment out `OrderWorkflowV2` (e.g. remove the `@Component` annotation) and start the app. With only
`OrderWorkflow` registered at `1.0.0`, every new instance runs that body. Place an order but don't pay
yet:

```bash
curl -X POST localhost:9099/orders \
     -H 'Content-Type: application/json' \
     -d '{"orderId":"order-1","customerId":"alice","amount":100}'

curl localhost:9099/orders/order-1
# → instances[0]: workflowId=order-1, workflowVersion=1.0.0, workflowStatus=STARTED
```

In Axon Server, every event for `order-1` carries `MessageType.version() = "1.0.0"`.

### Step 2 — Restart with both versions registered

Stop the app, re-enable `OrderWorkflowV2` (restore `@Component`), and restart:

```bash
./mvnw spring-boot:run
```

On startup:

- Replay routes `order-1`'s history to `OrderWorkflow` (its started event carries
  `MessageType.version() = "1.0.0"`). The workflow resumes its wait on `awaitPayment` — unchanged.
- New instances spawn on `OrderWorkflowV2` (highest registered version = `2.0.0`).

Place a new order with a **different** orderId:

```bash
curl -X POST localhost:9099/orders \
     -H 'Content-Type: application/json' \
     -d '{"orderId":"order-2","customerId":"bob","amount":200,"premium":false}'

curl localhost:9099/orders/order-2
# → instances[0]: workflowId=order-2, workflowVersion=2.0.0, steps include validateAddress + fraudReview
```

### Step 3 — Cross-version parallel: same orderId triggers a v2 sibling

Now POST a NEW order using the SAME `orderId=order-1` (the one already running at v1.0.0):

```bash
curl -X POST localhost:9099/orders \
     -H 'Content-Type: application/json' \
     -d '{"orderId":"order-1","customerId":"alice","amount":150,"premium":true}'
```

The engine detects the cross-version collision and spawns a parallel v2.0.0 instance with the
disambiguated id `order-1#2.0.0`:

```
INFO  Spawning a parallel workflow at version '2.0.0' alongside the existing instance 'order-1' at
version '1.0.0'. Disambiguated workflow id: 'order-1#2.0.0'.
```

```bash
curl localhost:9099/orders/order-1
# → instances[0]: workflowId=order-1,           workflowVersion=1.0.0  (in-flight v1)
# → instances[1]: workflowId=order-1#2.0.0,     workflowVersion=2.0.0  (parallel v2)
```

### Step 4 — Pay both instances, verify routing held

```bash
curl -X POST localhost:9099/orders/order-1/payment \
     -H 'Content-Type: application/json' \
     -d '{"amount":100}'

curl -X POST localhost:9099/orders/order-2/payment \
     -H 'Content-Type: application/json' \
     -d '{"amount":200}'
```

Inspect the event stream for each order in Axon Server:

- `order-1`: every event has `MessageType.version() = "1.0.0"`. Steps match `OrderWorkflow`'s body.
- `order-1#2.0.0`: every event has `MessageType.version() = "2.0.0"`. Steps include `validateAddress`
  and `fraudReview` (v2.0.0-only).
- `order-2`: every event has `MessageType.version() = "2.0.0"`. Same v2 body.

Each instance ran on its own definition without any manual intervention.

### Step 5 — Try a downgrade — rejected

The runtime guards against accidental downgrades. Adding `ctx.version("payment-redesign", "0.5.0")`
inside `OrderWorkflowV2` (where current version is `2.0.0`) at runtime throws:

```
IllegalArgumentException: ctx.version("payment-redesign", "0.5.0") rejected: requested version
is not strictly greater than the workflow's current version "2.0.0". Versions must only move
forward (semver-ordered).
```

## What to verify in Axon Server's event store

| Workflow id | Started event version | All events versioned with | Steps |
|---|---|---|---|
| `order-1` | `1.0.0` | `1.0.0` | `reserveStock`, `chargePayment`, `awaitPayment`, `notifyCustomer` |
| `order-1#2.0.0` | `2.0.0` | `2.0.0` | `reserveStock`, `validateAddress`, `fraudReview`, `chargePayment`, `awaitPayment`, `notifyCustomer` |
| `order-2` | `2.0.0` | `2.0.0` | same v2 body |

## How the demo maps to the implementation

| Behaviour shown | Code path |
|---|---|
| `@Workflow(version=...)` resolves to `MessageType.DEFAULT_VERSION` when empty | `AutoDetectionUtils.workflowVersion(...)` |
| Highest-version-wins for new spawns | `WorkflowEngine.checkAndCreateNewWorkflow()` → `WorkflowConfigurationRegistry.getHighestVersionConfigurations(...)` |
| Cross-version workflowId disambiguation | `WorkflowEngine.resolveWorkflowIdForNewSpawn(...)` |
| Replay routes to the matching definition by version | `SimpleWorkflowExecution.resolveVersionedDefinition()` → `WorkflowConfigurationRegistry.findByWorkflowNameAndVersion(...)` |
| Workflow version stamped on every emitted event | `EventMessageUtils` factories pass `state.currentWorkflowVersion()` to `new MessageType(name, version)` |
| State sources `currentWorkflowVersion` from the started event | `EventSourcedWorkflowState.evolve(...)` |
| Downgrade guard on `ctx.version` | `VersionDelegate.version(...)` via `WorkflowVersionComparator.isGreater(...)` |
| Multi-bean grouping into one engine | `WorkflowModuleConfigurer.register(...)` (Spring Boot autoconfig) |

See `docs/reference/modules/workflows/pages/versioning.adoc` and ADR 004 in the `axon-flow-spec`
repo for the full design rationale.
