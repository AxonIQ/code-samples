import test from "node:test";
import assert from "node:assert/strict";
import { buildTrace, unionDuration } from "../../main/resources/static/workflow-trace.mjs";
const start = Date.parse("2026-10-05T09:00:00Z");
const workflow = { startedAt: new Date(start).toISOString(), status: "COMPLETED" };
const event = (sequence, ms, step, status, primitive) => ({
    sequence,
    timestamp: new Date(start + ms).toISOString(),
    step,
    status,
    metadata: step ? { stepPrimitive: primitive } : { workflowStatus: status },
});

test("overlapping waits use recorded starts, count once, and stop on workflow completion", () => {
    const trace = buildTrace(
        workflow,
        [
            event(1, 0, null, "STARTED"),
            event(2, 10, "wait", "STARTED", "WAIT_FOR_EVENT"),
            event(3, 20, "action", "STARTED"),
            event(4, 30, "action", "COMPLETED"),
            event(5, 50, "secondWait", "STARTED", "WAIT_FOR_EVENT"),
            event(6, 100, "wait", "COMPLETED"),
            event(7, 150, "secondWait", "COMPLETED"),
            event(8, 160, null, "COMPLETED"),
        ],
        start + 5000,
    );
    assert.equal(trace.duration, 160);
    assert.equal(trace.rows[0].start, start + 10);
    assert.equal(trace.rows[1].duration, 10);
    assert.equal(trace.waitDuration, 140);
    assert.equal(
        unionDuration([
            [10, 20],
            [15, 25],
            [30, 40],
        ]),
        25,
    );
});
test("live spans grow, while recorded playback hides future events and results", () => {
    const events = [
        event(1, 0, null, "STARTED"),
        event(2, 10, "wait", "STARTED", "WAIT_FOR_EVENT"),
        event(3, 100, "wait", "COMPLETED"),
        event(4, 110, null, "COMPLETED"),
    ];
    const playback = buildTrace(workflow, events, start + 1000, start + 50);
    assert.equal(playback.status, "STARTED");
    assert.equal(playback.events.length, 2);
    assert.equal(playback.rows[0].duration, 40);
    assert.equal(playback.rows[0].spans[0].active, true);
    const live = buildTrace({ ...workflow, status: "STARTED" }, events.slice(0, 2), start + 500);
    assert.equal(live.rows[0].duration, 490);
});
test("publish without start remains a point; timeout and cancellation close waits", () => {
    for (const status of ["TIMED_OUT", "CANCELLED"]) {
        const trace = buildTrace(
            workflow,
            [
                event(1, 0, null, "STARTED"),
                event(2, 5, "wait", "STARTED", "WAIT_FOR_EVENT"),
                event(3, 100, "wait", status),
                event(4, 105, "recordFailure", "COMPLETED", "PUBLISH"),
                event(5, 110, null, "FAILED"),
            ],
            start + 10000,
        );
        assert.equal(trace.rows[0].duration, 95);
        assert.equal(trace.rows[0].spans[0].active, false);
        assert.equal(trace.rows[1].spans[0].instant, true);
        assert.equal(trace.rows[1].duration, 0);
    }
});
test("retries are separate spans rather than including backoff in action duration", () => {
    const trace = buildTrace(
        workflow,
        [
            event(1, 0, null, "STARTED"),
            event(2, 10, "action", "STARTED"),
            event(3, 20, "action", "RETRYING"),
            event(4, 80, "action", "RETRY_STARTED"),
            event(5, 100, "action", "COMPLETED"),
            event(6, 110, null, "COMPLETED"),
        ],
        start + 1000,
    );
    assert.equal(trace.rows[0].spans.length, 2);
    assert.equal(trace.rows[0].duration, 30);
    assert.equal(trace.rows[0].spans[0].status, "RETRYING");
});

test("replay rebuilds pending, running, completed and output state in both directions", async () => {
    const { replayTrace, inspectTrace } = await import("../../main/resources/static/workflow-trace.mjs");
    const history = [
        { ...event(1, 0, null, "STARTED"), payload: { orderId: "one" } },
        { ...event(2, 10, "reserveStock", "STARTED"), payload: { amount: 20 } },
        { ...event(3, 11, "reserveStock", "COMPLETED"), payload: { reserved: true } },
        { ...event(4, 12, "shipOrder", "STARTED"), payload: { orderId: "one" } },
        { ...event(5, 13, "shipOrder", "COMPLETED"), payload: { trackingNumber: "tracking-1" } },
        event(6, 14, null, "COMPLETED"),
    ];
    for (const cursor of [0, 2, 3, 6, 2, 0]) {
        const trace = replayTrace(workflow, history, cursor);
        assert.equal(trace.events.length, cursor);
        assert.equal(trace.rows.length, 2);
        const reserve = inspectTrace(trace, "reserveStock");
        assert.equal(reserve.status, cursor < 2 ? "PENDING" : cursor < 3 ? "STARTED" : "COMPLETED");
        assert.deepEqual(reserve.input, cursor < 2 ? undefined : { amount: 20 });
        assert.deepEqual(reserve.output, cursor < 3 ? undefined : { reserved: true });
        const shipping = inspectTrace(trace, "shipOrder");
        assert.equal(shipping.status, cursor < 4 ? "PENDING" : "COMPLETED");
        assert.deepEqual(inspectTrace(trace, "@root").input, cursor ? { orderId: "one" } : undefined);
        assert.equal(trace.status, cursor === 0 ? "PENDING" : cursor === 6 ? "COMPLETED" : "STARTED");
        if (cursor < 5) assert.equal(inspectTrace(trace, "@root").output.shipOrder, undefined);
    }
});

test("event cursor distinguishes events with identical timestamps and sparse sequences", async () => {
    const { replayTrace } = await import("../../main/resources/static/workflow-trace.mjs");
    const history = [
        event(10, 0, null, "STARTED"),
        event(20, 5, "action", "STARTED"),
        event(40, 5, "action", "COMPLETED"),
        event(50, 5, null, "COMPLETED"),
    ];
    assert.equal(replayTrace(workflow, history, 2.8).rows[0].status, "STARTED");
    assert.equal(replayTrace(workflow, history, 3).rows[0].status, "COMPLETED");
    assert.equal(replayTrace(workflow, history, 3).status, "STARTED");
    assert.equal(replayTrace(workflow, history, 4).status, "COMPLETED");
});

test("rewinding a failure hides its result and exposes retrying only when recorded", async () => {
    const { replayTrace, inspectTrace } = await import("../../main/resources/static/workflow-trace.mjs");
    const history = [
        event(1, 0, null, "STARTED"),
        event(2, 10, "action", "STARTED"),
        event(3, 20, "action", "RETRYING"),
        event(4, 40, "action", "RETRY_STARTED"),
        { ...event(5, 50, "action", "FAILED"), payload: { message: "Unavailable" } },
        event(6, 60, null, "FAILED"),
    ];
    assert.equal(inspectTrace(replayTrace(workflow, history, 3), "action").status, "RETRYING");
    assert.deepEqual(inspectTrace(replayTrace(workflow, history, 5), "action").output, { message: "Unavailable" });
    assert.equal(inspectTrace(replayTrace(workflow, history, 4), "action").output, undefined);
    assert.equal(inspectTrace(replayTrace(workflow, history, 0), "action").status, "PENDING");
});

test("BPMN branch states follow the same replay cursor and skip unvisited branches only at the end", async () => {
    const { replayTrace } = await import("../../main/resources/static/workflow-trace.mjs");
    const { bpmnStates } = await import("../../main/resources/static/workflow-bpmn.mjs");
    const history = [
        event(1, 0, null, "STARTED"),
        event(2, 10, "awaitPayment", "STARTED", "WAIT_FOR_EVENT"),
        event(3, 20, "reserveStock", "STARTED"),
        { ...event(4, 30, "reserveStock", "COMPLETED"), payload: { reserved: false } },
        event(5, 35, "awaitPayment", "CANCELLED"),
        event(6, 40, "recordOutOfStock", "COMPLETED", "PUBLISH"),
        event(7, 45, null, "FAILED"),
    ];
    const at = (cursor) => bpmnStates(replayTrace(workflow, history, cursor));
    assert.equal(at(0).reserveStock, "PENDING");
    assert.equal(at(3).reserveStock, "STARTED");
    assert.equal(at(3).f_stock_no, "PENDING");
    assert.equal(at(4).f_stock_no, "COMPLETED");
    assert.equal(at(4).f_stock_yes, "PENDING");
    assert.equal(at(5).awaitPayment, "CANCELLED");
    assert.equal(at(5).PaymentError, "PENDING"); // Stock cancellation must not take the payment-error branch.
    assert.equal(at(6).shipOrder, "PENDING");
    assert.equal(at(7).shipOrder, "SKIPPED");
    assert.equal(at(7).StockFailed, "FAILED");
    assert.equal(at(2).recordOutOfStock, "PENDING");
});

test("BPMN join waits for payment and initiation; timeout takes only the timeout branch", async () => {
    const { replayTrace } = await import("../../main/resources/static/workflow-trace.mjs");
    const { bpmnStates } = await import("../../main/resources/static/workflow-bpmn.mjs");
    const history = [
        event(1, 0, null, "STARTED"),
        event(2, 10, "awaitPayment", "STARTED", "WAIT_FOR_EVENT"),
        event(3, 20, "initiatePayment", "STARTED"),
        event(4, 30, "initiatePayment", "COMPLETED"),
        event(5, 50, "awaitPayment", "COMPLETED"),
        event(6, 60, "shipOrder", "STARTED"),
    ];
    assert.equal(bpmnStates(replayTrace(workflow, history, 4)).PaymentJoined, "STARTED");
    assert.equal(bpmnStates(replayTrace(workflow, history, 5)).PaymentJoined, "COMPLETED");
    assert.equal(bpmnStates(replayTrace(workflow, history, 4)).shipOrder, "PENDING");
    const timeout = [
        ...history.slice(0, 4),
        event(5, 50, "awaitPayment", "TIMED_OUT"),
        event(6, 60, "recordPaymentTimeout", "COMPLETED", "PUBLISH"),
        event(7, 70, null, "FAILED"),
    ];
    const failed = bpmnStates(replayTrace(workflow, timeout, 7));
    assert.equal(failed.awaitPayment, "TIMED_OUT");
    assert.equal(failed.f_timeout, "COMPLETED");
    assert.equal(failed.PaymentTimedOut, "FAILED");
    assert.equal(failed.f_paid, "SKIPPED");
    assert.equal(failed.shipOrder, "SKIPPED");
    assert.equal(bpmnStates(replayTrace(workflow, timeout, 4)).PaymentDeadline, "PENDING");
});

test("BPMN routes initiation failure without treating companion cancellation as another failure", async () => {
    const { replayTrace } = await import("../../main/resources/static/workflow-trace.mjs");
    const { bpmnStates } = await import("../../main/resources/static/workflow-bpmn.mjs");
    for (const confirmationStatus of ["COMPLETED", "CANCELLED"]) {
        const history = [
            event(1, 0, null, "STARTED"),
            event(2, 10, "awaitPayment", "STARTED", "WAIT_FOR_EVENT"),
            event(3, 20, "initiatePayment", "STARTED"),
            event(4, 30, "initiatePayment", "FAILED"),
            event(5, 40, "awaitPayment", confirmationStatus),
            event(6, 50, "recordPaymentFailure", "COMPLETED", "PUBLISH"),
            event(7, 60, null, "FAILED"),
        ];
        const states = bpmnStates(replayTrace(workflow, history, 7));
        assert.equal(states.PaymentInitiationError, "FAILED");
        assert.equal(states.f_initiation_error, "COMPLETED");
        assert.equal(states.PaymentError, "SKIPPED");
        assert.equal(states.PaymentJoined, "SKIPPED");
        assert.equal(states.shipOrder, "SKIPPED");
        assert.equal(bpmnStates(replayTrace(workflow, history, 3)).PaymentInitiationError, "PENDING");
    }
    const timeout = [
        event(1, 0, null, "STARTED"),
        event(2, 10, "awaitPayment", "STARTED", "WAIT_FOR_EVENT"),
        event(3, 20, "initiatePayment", "STARTED"),
        event(4, 30, "awaitPayment", "TIMED_OUT"),
        event(5, 40, "initiatePayment", "CANCELLED"),
        event(6, 50, "recordPaymentTimeout", "COMPLETED", "PUBLISH"),
        event(7, 60, null, "FAILED"),
    ];
    const states = bpmnStates(replayTrace(workflow, timeout, 7));
    assert.equal(states.PaymentDeadline, "TIMED_OUT");
    assert.equal(states.PaymentInitiationError, "SKIPPED");
    assert.equal(states.f_initiation_error, "SKIPPED");
});
