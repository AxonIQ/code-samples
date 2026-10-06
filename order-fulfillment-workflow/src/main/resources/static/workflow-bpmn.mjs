import { terminal, friendlyStatus, duration, inspectTrace } from "./workflow-trace.mjs";

// This model is authored alongside the Java definition, not inferred from a single run.
export const BPMN_STEPS = {
    awaitPayment: "WAIT_FOR_EVENT",
    reserveStock: "ACTION",
    initiatePayment: "ACTION",
    shipOrder: "ACTION",
    notifyCustomer: "ACTION",
    recordOutOfStock: "PUBLISH",
    recordPaymentTimeout: "PUBLISH",
    recordPaymentFailure: "PUBLISH",
};

export function bpmnStates(trace) {
    const idle = terminal(trace.status) ? "SKIPPED" : "PENDING";
    const step = (name) => trace.rows.find((row) => row.name === name);
    const seen = (name) => Boolean(step(name)?.events.length);
    const done = (name) => step(name)?.status === "COMPLETED";
    const reserved = inspectTrace(trace, "reserveStock")?.output?.reserved;
    const waitStatus = step("awaitPayment")?.status;
    const firstPaymentFailure = trace.events.find(
        (event) =>
            ["awaitPayment", "initiatePayment"].includes(event.step) &&
            ["FAILED", "TIMED_OUT", "CANCELLED"].includes(event.status),
    )?.step;
    const started = trace.events.some((event) => !event.step && event.status === "STARTED");
    const states = Object.fromEntries(Object.keys(BPMN_STEPS).map((name) => [name, step(name)?.status || idle]));
    // A row that was only included to keep replay layout stable has not executed.
    if (terminal(trace.status)) for (const name of Object.keys(BPMN_STEPS)) if (!seen(name)) states[name] = "SKIPPED";
    Object.assign(states, {
        OrderStarted: started ? "COMPLETED" : "PENDING",
        ParallelStart: seen("awaitPayment") || seen("reserveStock") ? "COMPLETED" : idle,
        StockAvailable: done("reserveStock") ? "COMPLETED" : idle,
        PaymentJoined:
            done("awaitPayment") && done("initiatePayment")
                ? "COMPLETED"
                : !terminal(trace.status) && (done("awaitPayment") || done("initiatePayment"))
                  ? "STARTED"
                  : idle,
        PaymentDeadline: waitStatus === "TIMED_OUT" ? "TIMED_OUT" : idle,
        PaymentError:
            firstPaymentFailure === "awaitPayment" &&
            (waitStatus === "FAILED" || waitStatus === "CANCELLED") &&
            reserved !== false
                ? "FAILED"
                : idle,
        PaymentInitiationError: firstPaymentFailure === "initiatePayment" ? "FAILED" : idle,
        OrderCompleted: trace.status === "COMPLETED" ? "COMPLETED" : idle,
        StockFailed: trace.status === "FAILED" && done("recordOutOfStock") ? "FAILED" : idle,
        PaymentTimedOut: trace.status === "FAILED" && done("recordPaymentTimeout") ? "FAILED" : idle,
        PaymentFailed: trace.status === "FAILED" && done("recordPaymentFailure") ? "FAILED" : idle,
    });
    const flows = {
        f_start: started,
        f_wait: seen("awaitPayment"),
        f_stock: seen("reserveStock"),
        f_stock_result: done("reserveStock"),
        f_stock_yes: reserved === true,
        f_stock_no: reserved === false,
        f_initiated: done("initiatePayment"),
        f_paid: done("awaitPayment"),
        f_ship: seen("shipOrder"),
        f_notify: seen("notifyCustomer"),
        f_done: trace.status === "COMPLETED",
        f_stock_failed: done("recordOutOfStock"),
        f_timeout: waitStatus === "TIMED_OUT",
        f_timeout_end: done("recordPaymentTimeout"),
        f_error: states.PaymentError === "FAILED",
        f_initiation_error: states.PaymentInitiationError === "FAILED",
        f_error_end: done("recordPaymentFailure"),
    };
    for (const [id, traversed] of Object.entries(flows)) states[id] = traversed ? "COMPLETED" : idle;
    return states;
}

export class BpmnDiagram {
    constructor(onSelect) {
        this.onSelect = onSelect;
        this.markers = new Map();
        this.chips = new Map();
    }
    async show(trace, selectedStep) {
        this.trace = trace;
        this.selectedStep = selectedStep;
        this.loading ||= this.initialize();
        await this.loading;
        this.update(this.trace, this.selectedStep);
        this.fit();
    }
    async initialize() {
        this.viewer = new window.BpmnJS({
            container: "#bpmn-canvas",
            bpmnRenderer: { defaultFillColor: "#ffffff", defaultStrokeColor: "#b0b8c6", defaultLabelColor: "#596577" },
        });
        const response = await fetch("/order-fulfillment.bpmn");
        if (!response.ok) throw new Error(`BPMN model unavailable (${response.status})`);
        const result = await this.viewer.importXML(await response.text());
        if (result.warnings.length) console.warn("BPMN import warnings", result.warnings);
        this.canvas = this.viewer.get("canvas");
        this.registry = this.viewer.get("elementRegistry");
        this.viewer.on("element.click", ({ element }) => {
            const id = element.labelTarget?.id || element.id;
            if (BPMN_STEPS[id]) this.onSelect(id);
            else if (id === "OrderStarted" || id === "OrderCompleted") this.onSelect("@root");
        });
        const overlays = this.viewer.get("overlays");
        for (const name of Object.keys(BPMN_STEPS)) {
            const shape = this.registry.get(name);
            const chip = document.createElement("button");
            chip.className = "bpmn-state-chip";
            chip.style.width = `${shape.width - 12}px`;
            chip.onclick = () => this.onSelect(name);
            overlays.add(name, { position: { top: shape.height - 23, left: 6 }, html: chip, scale: true });
            this.chips.set(name, chip);
        }
        document.getElementById("bpmn-status").hidden = true;
        document.getElementById("bpmn-canvas").dataset.loaded = "true";
        this.ready = true;
    }
    update(trace, selectedStep) {
        this.trace = trace;
        this.selectedStep = selectedStep;
        if (!this.ready) return;
        for (const [id, status] of Object.entries(bpmnStates(trace))) {
            const marker = `state-${status.toLowerCase()}`;
            if (this.markers.get(id) !== marker) {
                if (this.markers.has(id)) this.canvas.removeMarker(id, this.markers.get(id));
                this.canvas.addMarker(id, marker);
                this.markers.set(id, marker);
            }
            const chip = this.chips.get(id);
            if (chip) {
                const row = trace.rows.find((row) => row.name === id);
                const label =
                    status === "SKIPPED"
                        ? "Not taken"
                        : status === "STARTED" && id === "awaitPayment"
                          ? "Waiting"
                          : friendlyStatus(status);
                chip.textContent = `${label}${row?.events.length && row.spans.some((s) => !s.instant) ? " · " + duration(row.duration) : ""}`;
                chip.className = `bpmn-state-chip ${marker}`;
                chip.setAttribute("aria-label", `Inspect BPMN ${id}: ${label}`);
                chip.setAttribute("aria-pressed", String(id === selectedStep));
                if (id === selectedStep) this.canvas.addMarker(id, "bpmn-selected");
                else this.canvas.removeMarker(id, "bpmn-selected");
            }
        }
    }
    fit() {
        if (this.ready && !document.getElementById("bpmn-view").hidden) {
            this.canvas.resized();
            this.canvas.zoom("fit-viewport", "auto");
            this.canvas.zoom(this.canvas.zoom() * 0.94);
        }
    }
    zoom(factor) {
        if (this.ready) this.canvas.zoom(Math.max(0.25, Math.min(3, this.canvas.zoom() * factor)));
    }
}
