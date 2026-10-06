export const terminal = (status) => ["COMPLETED", "FAILED", "TIMED_OUT", "CANCELLED"].includes(status);
export const friendlyStatus = (status) =>
    ({
        STARTED: "Running",
        COMPLETED: "Completed",
        FAILED: "Failed",
        TIMED_OUT: "Timed out",
        CANCELLED: "Cancelled",
        RETRYING: "Retrying",
        RETRY_STARTED: "Retrying",
        PENDING: "Pending",
        SKIPPED: "Not taken",
    })[status] || status;
export const scenarioName = (value) =>
    ({
        happy: "Automatic payment",
        "manual-payment": "Interactive payment",
        "out-of-stock": "Out of stock",
        "payment-timeout": "Payment timeout",
    })[value] ||
    value ||
    "Standard run";
export function duration(ms) {
    if (!Number.isFinite(ms)) return "—";
    ms = Math.max(0, ms);
    if (ms < 1000) return `${Math.round(ms)} ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(ms < 10000 ? 2 : 1)} s`;
    return `${Math.floor(ms / 60000)}m ${Math.floor(ms / 1000) % 60}s`;
}
export function unionDuration(intervals) {
    const sorted = intervals.map(([a, b]) => [a, Math.max(a, b)]).sort((a, b) => a[0] - b[0]);
    let sum = 0,
        left,
        right;
    for (const [start, end] of sorted) {
        if (left === undefined) {
            left = start;
            right = end;
        } else if (start > right) {
            sum += right - left;
            left = start;
            right = end;
        } else right = Math.max(right, end);
    }
    return sum + (left === undefined ? 0 : right - left);
}
/** All timing comes from the journal. Missing start events remain point markers. */
export function buildTrace(workflow, journal, now, cutoff = Infinity, throughSequence = Infinity) {
    const ordered = [...journal].sort((a, b) => a.sequence - b.sequence);
    const replay = cutoff < Infinity || throughSequence < Infinity;
    const events = ordered.filter((e) => Date.parse(e.timestamp) <= cutoff && e.sequence <= throughSequence);
    const lifecycle = events.filter((e) => !e.step && e.metadata?.workflowStatus);
    const start = Date.parse(lifecycle.find((e) => e.status === "STARTED")?.timestamp || workflow.startedAt);
    const closing = lifecycle.findLast((e) => terminal(e.status));
    const status = lifecycle.at(-1)?.status || (replay ? "PENDING" : workflow.status);
    const ended = closing ? Date.parse(closing.timestamp) : null;
    const end = Math.max(start, ended ?? Math.min(now, cutoff));
    const grouped = new Map();
    // Preserve the recorded step order while rewinding. Only identity and primitive
    // are known before a step starts; no future events, results, or timing leak in.
    if (replay) for (const event of ordered) if (event.step && !grouped.has(event.step)) grouped.set(event.step, []);
    for (const event of events) {
        if (!event.step) continue;
        if (!grouped.has(event.step)) grouped.set(event.step, []);
        grouped.get(event.step).push(event);
    }
    const rows = [];
    for (const [name, history] of grouped) {
        const kind =
            ordered.find((e) => e.step === name && e.metadata?.stepPrimitive)?.metadata.stepPrimitive || "ACTION";
        const spans = [];
        let open;
        for (const event of history) {
            const t = Date.parse(event.timestamp);
            if (event.status === "STARTED" || event.status === "RETRY_STARTED") {
                if (open) {
                    open.end = t;
                    open.active = false;
                }
                open = { start: t, end: null, status: event.status, active: true, events: [event], instant: false };
                spans.push(open);
            } else if (terminal(event.status) || event.status === "RETRYING") {
                if (open) {
                    open.end = Math.max(open.start, t);
                    open.status = event.status;
                    open.active = false;
                    open.events.push(event);
                    open = null;
                } else
                    spans.push({
                        start: t,
                        end: t,
                        status: event.status,
                        active: false,
                        events: [event],
                        instant: true,
                    });
            } else if (open) open.events.push(event);
            else spans.push({ start: t, end: t, status: event.status, active: false, events: [event], instant: true });
        }
        for (const span of spans) {
            span.end ??= Math.max(span.start, end);
            span.active &&= !terminal(status);
        }
        rows.push({
            name,
            kind,
            events: history,
            spans,
            status: history.at(-1)?.status || "PENDING",
            start: spans[0]?.start,
            end: spans.at(-1)?.end,
            duration: spans.reduce((sum, span) => sum + (span.instant ? 0 : span.end - span.start), 0),
        });
    }
    const waitDuration = unionDuration(
        rows
            .filter((row) => row.kind === "WAIT_FOR_EVENT")
            .flatMap((row) => row.spans.filter((span) => !span.instant).map((span) => [span.start, span.end])),
    );
    return {
        start,
        end,
        status,
        ended,
        rows,
        events,
        duration: end - start,
        waitDuration,
        input: lifecycle.find((e) => e.status === "STARTED")?.payload,
    };
}

/** Position is an event cursor, not wall-clock speed. Fractions animate between events. */
export function replayTrace(workflow, journal, position) {
    const ordered = [...journal].sort((a, b) => a.sequence - b.sequence);
    const bounded = Math.max(0, Math.min(ordered.length, position));
    const count = Math.floor(bounded);
    const prefix = ordered.slice(0, count);
    const lastTime = Math.max(Date.parse(workflow.startedAt), ...prefix.map((e) => Date.parse(e.timestamp)));
    const nextTime = Math.max(lastTime, Date.parse(ordered[count]?.timestamp || workflow.startedAt));
    const clock = count === 0 ? lastTime : lastTime + (nextTime - lastTime) * (bounded - count);
    return buildTrace(workflow, ordered, clock, clock, prefix.at(-1)?.sequence ?? -Infinity);
}

/** The inspector consumes the same as-of state as the waterfall and journal. */
export function inspectTrace(trace, name) {
    if (name === "@root")
        return {
            status: trace.status,
            input: trace.input,
            duration: trace.duration,
            events: trace.events.filter((e) => !e.step),
            output: Object.fromEntries(
                trace.rows.filter((r) => r.status === "COMPLETED").map((r) => [r.name, r.events.at(-1).payload]),
            ),
        };
    const row = trace.rows.find((r) => r.name === name);
    if (!row) return undefined;
    return {
        ...row,
        input: row.events.findLast((e) => e.status === "STARTED" || e.status === "RETRY_STARTED")?.payload,
        output: terminal(row.status) ? row.events.at(-1)?.payload : undefined,
    };
}
