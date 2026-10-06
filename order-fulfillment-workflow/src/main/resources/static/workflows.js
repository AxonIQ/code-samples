import {
    buildTrace,
    replayTrace,
    inspectTrace,
    duration,
    terminal,
    friendlyStatus,
    scenarioName,
} from "./workflow-trace.mjs";
import { BpmnDiagram, BPMN_STEPS } from "./workflow-bpmn.mjs";
const diagram = new BpmnDiagram(selectStep);
let activeView = "waterfall";
const $ = (id) => document.getElementById(id);
const PAGE_SIZE = 25;
let selected = new URLSearchParams(location.search).get("id");
let workflow,
    journal = [],
    offset = 0,
    generation = 0,
    awaitingFirstEvent = false;
let refreshing = false,
    refreshAgain = false,
    refreshTimer,
    listSignature = "",
    rowSignature = "",
    inspectorSignature = "",
    journalSignature = "";
let selectedStep = "@root",
    selectedEvent = null,
    payloadTab = "Input",
    zoom = 1,
    rowNodes = [],
    axisRange = -1;
let live = true,
    connected = false,
    stream,
    frozenAt,
    serverOffset = 0;
let replaying = false,
    playing = false,
    replayPosition = 0,
    replayAnchor = 0,
    replayAnchorPosition = 0;
let model,
    fullModel,
    frameAt = 0;
const date = (value) =>
    new Date(value).toLocaleString(undefined, {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
    });
const now = () => (live ? Date.now() + serverOffset : frozenAt);
function el(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined && text !== null) node.textContent = text;
    if (className) node.className = className;
    return node;
}
function icon(name) {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    const use = document.createElementNS(svg.namespaceURI, "use");
    use.setAttribute("href", `#i-${name}`);
    svg.append(use);
    return svg;
}
function dot(status) {
    return el(
        "span",
        "",
        `status-dot ${terminal(status) ? (status === "COMPLETED" ? "green" : status === "CANCELLED" ? "gray" : "red") : "purple"}`,
    );
}
function badge(status) {
    return el("span", friendlyStatus(status), `badge ${status}`);
}
function jsonBlock(value) {
    const pre = el("pre", null, "code-block");
    const source = JSON.stringify(value ?? null, null, 2);
    // Tokenize into text nodes, never render payload or metadata as HTML.
    const pattern =
        /("(?:\\.|[^"\\])*"\s*:)|("(?:\\.|[^"\\])*")|\b(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\b|\b(true|false|null)\b/g;
    let cursor = 0;
    for (const match of source.matchAll(pattern)) {
        pre.append(document.createTextNode(source.slice(cursor, match.index)));
        pre.append(
            el(
                "span",
                match[0],
                match[1] ? "json-key" : match[2] ? "json-string" : match[3] ? "json-number" : "json-literal",
            ),
        );
        cursor = match.index + match[0].length;
    }
    pre.append(document.createTextNode(source.slice(cursor)));
    return pre;
}
async function request(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) {
        const error = new Error(
            `Request failed (${response.status}). ${response.status === 404 ? "Execution not found." : "Check that the application is running."}`,
        );
        error.status = response.status;
        throw error;
    }
    return response.status === 204 || response.headers.get("content-length") === "0" ? null : response.json();
}
function showError(error) {
    $("error").textContent = error.message;
    $("error").hidden = false;
}
function scheduleRefresh() {
    clearTimeout(refreshTimer);
    refreshTimer = setTimeout(refresh, 100);
}
function selectWorkflow(id, justStarted = false) {
    selected = id;
    awaitingFirstEvent = justStarted;
    generation++;
    workflow = null;
    journal = [];
    selectedStep = "@root";
    selectedEvent = null;
    payloadTab = "Input";
    replaying = playing = false;
    replayPosition = 0;
    zoom = 1;
    rowSignature = inspectorSignature = journalSignature = "";
    axisRange = -1;
    document.querySelector(".inspector").classList.remove("open");
    $("execution").hidden = true;
    $("empty").hidden = true;
    $("loading").hidden = false;
    $("error").hidden = true;
    history.replaceState(null, "", `?id=${encodeURIComponent(id)}`);
    refresh();
}
function renderList(data) {
    const signature = JSON.stringify([data, selected]);
    if (signature === listSignature) return;
    listSignature = signature;
    $("total").textContent = data.total;
    $("page").textContent = data.total
        ? `${offset + 1}–${Math.min(offset + PAGE_SIZE, data.total)} of ${data.total}`
        : "No executions";
    $("previous").disabled = offset === 0;
    $("next").disabled = offset + PAGE_SIZE >= data.total;
    const rows = data.items.map((run) => {
        const button = el("button", null, `run-row${run.workflowId === selected ? " selected" : ""}`);
        button.setAttribute("aria-pressed", String(run.workflowId === selected));
        button.setAttribute("aria-label", `${friendlyStatus(run.status)} execution ${run.workflowId}`);
        const top = el("span", null, "run-top");
        top.append(
            dot(run.status),
            el("strong", friendlyStatus(run.status)),
            el(
                "time",
                new Date(run.startedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", hour12: false }),
            ),
        );
        const bottom = el("span", null, "run-bottom");
        bottom.append(
            el("span", scenarioName(run.payload?.scenario), "scenario-label"),
            el(
                "span",
                terminal(run.status) ? duration(Date.parse(run.updatedAt) - Date.parse(run.startedAt)) : "In progress",
            ),
        );
        button.append(top, el("span", run.workflowId, "run-id"), bottom);
        button.onclick = () => selectWorkflow(run.workflowId);
        return button;
    });
    $("list").replaceChildren(...(rows.length ? rows : [el("p", "No executions match your filters.", "list-empty")]));
}
async function refresh() {
    if (refreshing) {
        refreshAgain = true;
        return;
    }
    refreshing = true;
    const version = generation,
        id = selected;
    try {
        const query = new URLSearchParams({ search: $("search").value, limit: PAGE_SIZE, offset });
        if ($("status").value) query.set("status", $("status").value);
        const data = await request(`/api/workflows?${query}`);
        if (version !== generation) return;
        renderList(data);
        if (!id && data.items.length) {
            selectWorkflow(data.items[0].workflowId);
            return;
        }
        if (id) {
            const detail = await request(`/api/workflows/${encodeURIComponent(id)}`);
            let after = journal.at(-1)?.sequence || 0,
                additions = [],
                batch;
            do {
                batch = await request(`/api/workflows/${encodeURIComponent(id)}/events?after=${after}&limit=500`);
                if (version !== generation) return;
                additions.push(...batch);
                after = batch.at(-1)?.sequence || after;
            } while (batch.length === 500);
            workflow = detail;
            journal.push(...additions);
            awaitingFirstEvent = false;
            $("loading").hidden = $("empty").hidden = true;
            $("execution").hidden = false;
            renderHeader();
            updateTrace();
            renderJournal();
            renderInspector();
            $("sync").textContent =
                `${live ? "Synced" : "Snapshot"} ${new Date().toLocaleTimeString([], { hour12: false })} · durable history`;
            // Catch a journal/state read that crossed a commit. Notifications are invalidations,
            // and reconnect also fetches the journal, so missing an SSE message cannot lose history.
            if (additions.at(-1) && Date.parse(additions.at(-1).timestamp) > Date.parse(detail.updatedAt))
                refreshAgain = true;
        } else {
            $("loading").hidden = true;
            $("empty").hidden = false;
        }
        $("error").hidden = true;
    } catch (error) {
        if (version === generation) {
            if (error.status === 404 && awaitingFirstEvent) scheduleRefresh();
            else {
                $("loading").hidden = true;
                showError(error);
            }
        }
    } finally {
        refreshing = false;
        if (refreshAgain) {
            refreshAgain = false;
            scheduleRefresh();
        }
    }
}
function renderHeader() {
    $("definition").textContent = workflow.definition.split(".").pop();
    $("workflow-id").textContent = workflow.workflowId;
    $("version").textContent = `v${workflow.version}`;
    $("started").textContent = date(workflow.startedAt);
    $("last-event").textContent =
        `Last event ${new Date(journal.at(-1)?.timestamp || workflow.updatedAt).toLocaleTimeString([], { hour12: false })}`;
    const waiting =
        workflow.payload?.scenario === "manual-payment" &&
        workflow.status === "STARTED" &&
        journal.some((e) => e.step === "initiatePayment" && e.status === "COMPLETED") &&
        !journal.some((e) => e.step === "awaitPayment" && terminal(e.status));
    $("payment-callout").hidden = !waiting;
    $("confirm-payment").disabled = false;
}
function currentTrace() {
    fullModel = buildTrace(workflow, journal, now());
    if (playing) {
        // Give even millisecond actions a visible running state. Source timestamps
        // still determine span widths; only the playback pace is stretched.
        replayPosition = Math.min(journal.length, replayAnchorPosition + (performance.now() - replayAnchor) / 900);
        if (replayPosition >= journal.length) playing = false;
    }
    return replaying ? replayTrace(workflow, journal, replayPosition) : fullModel;
}
function updateTrace() {
    if (!workflow || !journal.length) return;
    model = currentTrace();
    const stateBadge = $("execution-status");
    stateBadge.textContent = `${replaying ? "Replay · " : ""}${friendlyStatus(model.status)}`;
    stateBadge.className = `badge ${model.status}`;
    $("duration").textContent = duration(model.duration);
    const done = model.rows.filter((row) => row.status === "COMPLETED").length;
    $("step-count").textContent = `${done} / ${fullModel.rows.length}`;
    $("step-hint").textContent = model.rows.some((row) => row.spans.some((span) => span.active))
        ? "Execution in progress"
        : model.status === "COMPLETED"
          ? "All steps completed"
          : friendlyStatus(model.status);
    $("wait-time").textContent = duration(model.waitDuration);
    $("event-count").textContent = model.events.length;
    $("journal-count").textContent = model.events.length;
    $("last-event").textContent = model.events.length
        ? `Last event ${new Date(model.events.at(-1).timestamp).toLocaleTimeString([], { hour12: false })}`
        : "No events played yet";
    if (selectedEvent !== null && !model.events.some((event) => event.sequence === selectedEvent)) selectedEvent = null;
    const rows = [
        {
            name: "@root",
            kind: "WORKFLOW",
            status: model.status,
            events: model.events.filter((e) => !e.step),
            duration: model.duration,
            spans:
                model.status === "PENDING"
                    ? []
                    : [{ start: model.start, end: model.end, status: model.status, active: !terminal(model.status) }],
        },
        ...model.rows,
    ];
    const signature = JSON.stringify([
        rows.map((row) => [row.name, row.status, row.spans.map((span) => [span.start, span.status, span.instant])]),
        selectedStep,
        replaying,
    ]);
    if (signature !== rowSignature) {
        rowSignature = signature;
        createRows(rows);
    }
    draw(rows);
    $("replay").disabled = !terminal(fullModel.status);
    $("replay").querySelector("span").textContent = playing
        ? "Pause replay"
        : replaying && replayPosition < journal.length
          ? "Play replay"
          : "Replay trace";
    $("scrubber").disabled = !terminal(fullModel.status);
    $("scrubber").max = String(journal.length);
    $("scrubber").value = String(replaying ? Math.floor(replayPosition) : journal.length);
    $("scrubber").setAttribute("aria-valuetext", `Event ${model.events.length} of ${journal.length}`);
    $("previous-event").disabled = !replaying || replayPosition < 1;
    $("next-event").disabled = !replaying || replayPosition >= journal.length;
    $("replay-state").hidden = !replaying;
    const lastPlayed = model.events.at(-1);
    $("replay-state").textContent = lastPlayed
        ? `Event ${model.events.length} / ${journal.length} · ${lastPlayed.step || "Workflow"} → ${friendlyStatus(lastPlayed.status)}`
        : "Before the first event · all recorded steps are pending";
    $("replay-time").textContent = duration(model.duration);
    $("exit-replay").hidden = !replaying;
    $("trace-caption").textContent = replaying
        ? "Event-by-event replay · every view shows the state at the playhead."
        : "Select a span to inspect. Diamond = event without a recorded start.";
    const longestWait = model.rows
        .filter((row) => row.kind === "WAIT_FOR_EVENT")
        .sort((a, b) => b.duration - a.duration)[0];
    if (longestWait) {
        $("insight-title").textContent = longestWait.spans.some((s) => s.active)
            ? "The workflow is waiting, not working."
            : `${Math.round((model.waitDuration / Math.max(1, model.duration)) * 100)}% of this execution was spent awaiting an event.`;
        $("insight-text").textContent =
            "The payment wait starts before stock reservation and overlaps the actions below it. The waterfall makes that concurrency visible.";
    } else {
        $("insight-title").textContent = "A trace of what actually happened.";
        $("insight-text").textContent =
            "Each span is positioned from recorded lifecycle events. Inspect a step to see its input, output, and event metadata.";
    }
    const deadline = journal.find((e) => e.step === "awaitPayment" && e.status === "STARTED")?.payload?.timeoutTime;
    if (deadline)
        $("payment-countdown").textContent = `Times out in ${duration(Math.max(0, Date.parse(deadline) - now()))}.`;
    renderInspector();
    renderJournal();
    diagram.update(model, selectedStep);
    const inspectorDuration = $("inspector-duration");
    if (inspectorDuration) {
        const row = model.rows.find((item) => item.name === selectedStep);
        inspectorDuration.textContent =
            selectedStep === "@root"
                ? duration(model.duration)
                : row?.status === "PENDING"
                  ? "Not started"
                  : row?.spans.every((s) => s.instant)
                    ? "Point event"
                    : duration(row?.duration);
    }
}
function createRows(rows) {
    rowNodes = rows.map((row) => {
        const root = row.name === "@root",
            wait = row.kind === "WAIT_FOR_EVENT";
        const node = el(
            "div",
            null,
            `trace-row ${row.status}${root ? " root" : ""}${wait ? " wait" : ""}${row.name === selectedStep ? " selected" : ""}`,
        );
        const label = el("button", null, "row-label");
        node.dataset.step = row.name;
        node.dataset.status = row.status;
        label.title = `${root ? workflow.workflowId : row.name} · ${friendlyStatus(row.status)}`;
        label.setAttribute("aria-label", `Inspect ${root ? "workflow" : row.name}`);
        label.setAttribute("aria-pressed", String(row.name === selectedStep));
        const symbol = el("span", null, "row-symbol");
        symbol.append(
            icon(
                row.status === "COMPLETED"
                    ? "check"
                    : root
                      ? "workflow"
                      : wait
                        ? "clock"
                        : row.kind === "PUBLISH"
                          ? "arrow"
                          : "code",
            ),
        );
        const timing = el("span", "", "row-duration");
        const identity = el("span", null, "row-identity");
        identity.append(
            el("span", root ? "Order fulfillment" : row.name, "row-name"),
            el(
                "span",
                row.status === "STARTED" && wait ? "Waiting for event" : friendlyStatus(row.status),
                `row-state ${row.status}`,
            ),
        );
        label.append(symbol, identity, timing);
        label.onclick = () => selectStep(row.name);
        const track = el("div", null, "row-track");
        if (row.status === "PENDING") track.append(el("span", "Not started", "pending-track-label"));
        const bars = row.spans.map((span, index) => {
            const bar = el(
                "button",
                null,
                `span-bar ${span.status}${span.active ? " running" : ""}${span.instant ? " instant" : ""}`,
            );
            bar.setAttribute(
                "aria-label",
                `${root ? "Workflow" : row.name}, ${friendlyStatus(span.status)}, ${span.instant ? "point event" : duration(span.end - span.start)}`,
            );
            bar.onclick = () => selectStep(row.name);
            if (!span.instant && (wait || root))
                bar.append(
                    el(
                        "span",
                        root
                            ? friendlyStatus(row.status)
                            : span.active
                              ? "Awaiting PaymentConfirmed"
                              : friendlyStatus(span.status) === "Completed"
                                ? "PaymentConfirmed received"
                                : friendlyStatus(span.status),
                        "bar-caption",
                    ),
                );
            const ms = el("span", "", "bar-duration");
            if (!wait && !root && !span.instant) bar.append(ms);
            track.append(bar);
            return { bar, ms };
        });
        const playhead = el("div", "", "playhead");
        track.append(playhead);
        node.append(label, track);
        return { node, timing, bars, playhead };
    });
    $("waterfall-rows").replaceChildren(...rowNodes.map((row) => row.node));
}
function draw(rows) {
    const displayDuration = replaying ? fullModel.duration : model.duration;
    const range = Math.max(
        100,
        terminal(fullModel.status) ? displayDuration * 1.045 : Math.ceil((displayDuration + 500) / 1000) * 1000,
    );
    const available = $("waterfall-scroll").clientWidth;
    const labelWidth = parseFloat(getComputedStyle(document.documentElement).getPropertyValue("--label"));
    const width = Math.max(available, labelWidth + (available - labelWidth) * zoom);
    $("waterfall").style.width = `${width}px`;
    const tickCount = Math.max(2, Math.min(5, Math.floor((width - labelWidth - 22) / 75)));
    const axisSignature = `${range}:${tickCount}`;
    $("waterfall").style.setProperty("--ticks", `${100 / tickCount}%`);
    if (axisRange !== axisSignature) {
        axisRange = axisSignature;
        const label = el("div", null, "axis-label");
        label.append(el("span", "STEP"), el("span", "DURATION"));
        const axis = el("div", null, "axis-track");
        for (let i = 0; i <= tickCount; i++) {
            const tick = el("span", i === 0 ? "0" : duration((range * i) / tickCount), "axis-tick");
            tick.style.left = `${(i * 100) / tickCount}%`;
            axis.append(tick);
        }
        $("time-axis").replaceChildren(label, axis);
    }
    rows.forEach((row, index) => {
        const nodes = rowNodes[index];
        nodes.timing.textContent =
            row.status === "PENDING"
                ? "—"
                : ["PENDING", "SKIPPED"].includes(row.status)
                  ? "Not started"
                  : row.spans.every((s) => s.instant)
                    ? "event"
                    : duration(row.duration);
        row.spans.forEach((span, i) => {
            const left = Math.max(0, ((span.start - model.start) / range) * 100),
                barWidth = Math.max(0, ((span.end - span.start) / range) * 100);
            nodes.bars[i].bar.style.left = `${left}%`;
            nodes.bars[i].bar.style.width = `${barWidth}%`;
            nodes.bars[i].bar.title =
                `${row.name === "@root" ? "Workflow" : row.name} · ${friendlyStatus(span.status)}\n${new Date(span.start).toISOString()}${span.instant ? " · no start event" : ` → ${new Date(span.end).toISOString()}\n${duration(span.end - span.start)}`}`;
            nodes.bars[i].ms.textContent = duration(span.end - span.start);
            nodes.bars[i].bar.setAttribute(
                "aria-label",
                `${row.name === "@root" ? "Workflow" : row.name}, ${friendlyStatus(span.status)}, ${span.instant ? "point event" : duration(span.end - span.start)}`,
            );
            nodes.bars[i].ms.hidden = left + barWidth > 83;
        });
        nodes.playhead.hidden = !replaying && (terminal(model.status) || !live);
        nodes.playhead.style.left = `${Math.min(100, (model.duration / range) * 100)}%`;
    });
    $("trace-range").textContent = `${duration(model.duration)} total`;
}
function selectStep(name) {
    selectedStep = name;
    selectedEvent = null;
    payloadTab = "Input";
    inspectorSignature = journalSignature = "";
    updateTrace();
    renderInspector();
    document.querySelector(".inspector").classList.add("open");
}
function fact(label, value) {
    const node = el("div");
    node.append(el("label", label), el("strong", value));
    return node;
}
function renderInspector() {
    if (!workflow || !model) return;
    const signature = JSON.stringify([
        workflow.workflowId,
        model.events.at(-1)?.sequence,
        model.status,
        replaying,
        selectedStep,
        selectedEvent,
        payloadTab,
    ]);
    if (signature === inspectorSignature) return;
    inspectorSignature = signature;
    const box = $("inspector-content");
    box.replaceChildren();
    const event = selectedEvent == null ? null : model.events.find((e) => e.sequence === selectedEvent);
    const row =
        model.rows.find((row) => row.name === selectedStep) ||
        (BPMN_STEPS[selectedStep]
            ? {
                  name: selectedStep,
                  kind: BPMN_STEPS[selectedStep],
                  status: terminal(model.status) ? "SKIPPED" : "PENDING",
                  spans: [],
                  events: [],
                  duration: 0,
              }
            : null);
    const details = inspectTrace(model, selectedStep) || row || inspectTrace(model, "@root");
    const root = selectedStep === "@root" || !row;
    $("inspector-kind").textContent =
        `${replaying ? "REPLAY · " : ""}${event ? "RECORDED EVENT" : root ? "EXECUTION DETAILS" : "STEP DETAILS"}`;
    if (event) {
        const back = el("button", "← Back to step", "text-button event-detail-back");
        back.onclick = () => {
            selectedEvent = null;
            renderInspector();
        };
        box.append(
            back,
            el("h4", event.type.split(".").pop()),
            badge(event.status),
            el("p", event.timestamp, "inspector-subtitle mono"),
        );
        box.append(
            el("div", "PAYLOAD", "detail-title"),
            jsonBlock(event.payload),
            el("div", "METADATA", "detail-title"),
            jsonBlock(event.metadata),
            el("div", "EVENT ID", "detail-title"),
            el("p", event.eventId, "inspector-subtitle mono"),
        );
        return;
    }
    box.append(el("h4", root ? "Order fulfillment" : row.name), badge(details.status));
    box.append(
        el(
            "p",
            root
                ? "Reserve stock, collect payment, and deliver the order. Every transition is recorded."
                : row.kind === "WAIT_FOR_EVENT"
                  ? "Suspended until a matching event arrives or the deadline is reached."
                  : row.kind === "PUBLISH"
                    ? "A published event. A point marker is used when no start event was recorded."
                    : "A workflow action with its recorded input and result.",
            "inspector-subtitle",
        ),
    );
    if (root && model.input?.originCity) {
        const route = el("div", null, "route"),
            from = el("div"),
            to = el("div");
        from.append(el("small", "ORIGIN"), el("span", model.input.originCity));
        to.append(el("small", "DESTINATION"), el("span", model.input.destinationCity));
        route.append(from, icon("arrow"), to);
        box.append(route);
    }
    const facts = el("div", null, "detail-facts");
    facts.append(
        fact(
            root ? "Scenario" : "Primitive",
            root
                ? scenarioName(model.input?.scenario)
                : row.kind === "WAIT_FOR_EVENT"
                  ? "Wait for event"
                  : row.kind === "PUBLISH"
                    ? "Publish"
                    : "Action",
        ),
        fact(
            "Duration",
            root
                ? duration(model.duration)
                : ["PENDING", "SKIPPED"].includes(row.status)
                  ? "Not started"
                  : row.spans.every((s) => s.instant)
                    ? "Point event"
                    : duration(row.duration),
        ),
        fact(
            root ? "Version" : "Started at",
            root
                ? `v${workflow.version}`
                : ["PENDING", "SKIPPED"].includes(row.status)
                  ? "Not started"
                  : `+${duration(row.start - model.start)}`,
        ),
        fact("Events", String(root ? model.events.length : row.events.length)),
    );
    facts.children[1].querySelector("strong").id = "inspector-duration";
    box.append(facts);
    const events = details.events;
    const tabs = el("div", null, "payload-tabs");
    for (const title of ["Input", "Output"]) {
        const tab = el("button", title, payloadTab === title ? "active" : "");
        tab.onclick = () => {
            payloadTab = title;
            renderInspector();
        };
        tabs.append(tab);
    }
    box.append(tabs);
    const { input, output } = details;
    if ((payloadTab === "Input" ? input : output) === undefined)
        box.append(
            el(
                "p",
                details.status === "SKIPPED"
                    ? "This branch was not taken in this execution."
                    : details.status === "PENDING"
                      ? "This step has not started at the playhead."
                      : payloadTab === "Input"
                        ? "No input event was recorded."
                        : "No result recorded at the playhead.",
                "inspector-subtitle",
            ),
        );
    else box.append(jsonBlock(payloadTab === "Input" ? input : output));
    box.append(el("div", "LIFECYCLE EVENTS", "detail-title"));
    const timeline = el("div", null, "step-events");
    for (const item of events) {
        const button = el("button", null, "step-event");
        button.append(
            dot(item.status),
            el("span", friendlyStatus(item.status)),
            el("span", `+${duration(Date.parse(item.timestamp) - model.start)}`, "mono"),
        );
        button.onclick = () => {
            selectedEvent = item.sequence;
            renderInspector();
        };
        timeline.append(button);
    }
    box.append(timeline);
}
function renderJournal() {
    if (!model) return;
    const signature = JSON.stringify([
        workflow.workflowId,
        model.events.length,
        model.events.at(-1)?.sequence,
        selectedEvent,
        replaying,
    ]);
    if (signature === journalSignature) return;
    journalSignature = signature;
    $("events").replaceChildren(
        ...model.events.map((event) => {
            const button = el("button", null, `journal-row${selectedEvent === event.sequence ? " selected" : ""}`);
            const name = el("span", event.step || "Workflow", "journal-name");
            name.append(el("small", event.type.split(".").pop()));
            button.append(
                el("span", `#${event.sequence}`, "mono"),
                name,
                badge(event.status),
                el("time", `+${duration(Date.parse(event.timestamp) - Date.parse(workflow.startedAt))}`, "mono"),
            );
            button.onclick = () => {
                selectedStep = event.step || "@root";
                selectedEvent = event.sequence;
                renderInspector();
                renderJournal();
                document.querySelector(".inspector").classList.add("open");
            };
            return button;
        }),
    );
}
function setConnection() {
    $("live-label").textContent = !live ? "Paused" : connected ? "Live" : "Reconnecting";
    $("live").className = `live-button ${!live ? "paused" : connected ? "connected" : "disconnected"}`;
    $("live").setAttribute("aria-pressed", String(live));
    $("live").title = live ? "Pause incoming updates" : "Resume live history updates";
}
function connect() {
    stream?.close();
    if (!live) return;
    stream = new EventSource("/api/workflows/stream");
    const synchronizeClock = (event) => {
        const data = JSON.parse(event.data);
        serverOffset = data.serverTime - Date.now();
    };
    stream.addEventListener("ready", (event) => {
        synchronizeClock(event);
        connected = true;
        setConnection();
        refresh();
    });
    stream.addEventListener("heartbeat", synchronizeClock);
    stream.addEventListener("workflow", () => {
        if (live) scheduleRefresh();
    });
    stream.onerror = () => {
        connected = false;
        setConnection();
    };
}
$("live").onclick = () => {
    if (live) {
        frozenAt = now();
        live = false;
        generation++;
        stream?.close();
        connected = false;
        refreshAgain = false;
        clearTimeout(refreshTimer);
        $("sync").textContent = "Live updates paused · showing a snapshot";
    } else {
        live = true;
        connect();
    }
    setConnection();
};
$("run").onclick = async () => {
    $("run").disabled = true;
    try {
        if (!live) {
            live = true;
            connect();
        }
        const result = await request(`/simulate/single?scenario=${encodeURIComponent($("scenario").value)}`, {
            method: "POST",
        });
        offset = 0;
        $("search").value = "";
        $("status").value = "";
        selectWorkflow(result.orderId, true);
    } catch (error) {
        showError(error);
    } finally {
        $("run").disabled = false;
    }
};
$("confirm-payment").onclick = async () => {
    const id = selected;
    $("confirm-payment").disabled = true;
    try {
        const response = await fetch(`/orders/${encodeURIComponent(id)}/payment`, { method: "POST" });
        if (!response.ok) throw new Error(`Payment failed (${response.status}).`);
        scheduleRefresh();
    } catch (error) {
        showError(error);
        $("confirm-payment").disabled = false;
    }
};
$("copy-id").onclick = async () => {
    try {
        await navigator.clipboard.writeText(selected);
        $("copy-id").textContent = "Copied";
        setTimeout(() => ($("copy-id").textContent = "Copy ID"), 1500);
    } catch {
        $("copy-id").textContent = "Select ID to copy";
    }
};
let searchTimer;
$("search").oninput = () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
        offset = 0;
        generation++;
        refresh();
    }, 250);
};
$("status").onchange = () => {
    offset = 0;
    generation++;
    refresh();
};
$("previous").onclick = () => {
    offset = Math.max(0, offset - PAGE_SIZE);
    generation++;
    refresh();
};
$("next").onclick = () => {
    offset += PAGE_SIZE;
    generation++;
    refresh();
};
for (const name of ["waterfall", "bpmn", "events"]) {
    $(name + "-tab").onclick = async () => {
        activeView = name;
        document.querySelector(".console-layout").classList.toggle("bpmn-mode", name === "bpmn");
        if (name === "bpmn") document.querySelector(".inspector").classList.remove("open");
        for (const view of ["waterfall", "bpmn", "events"]) {
            $(view + "-view").hidden = view !== name;
            $(view + "-tab").classList.toggle("active", view === name);
            $(view + "-tab").setAttribute("aria-selected", String(view === name));
        }
        document.querySelector(".zoom-controls").hidden = name === "events";
        updateTrace();
        if (name === "bpmn" && model) {
            try {
                await diagram.show(model, selectedStep);
            } catch (error) {
                $("bpmn-status").textContent = error.message;
                showError(error);
            }
        }
    };
}
$("zoom-in").onclick = () => {
    if (activeView === "bpmn") {
        diagram.zoom(1.25);
        return;
    }
    zoom = Math.min(16, zoom * 2);
    updateTrace();
};
$("zoom-out").onclick = () => {
    if (activeView === "bpmn") {
        diagram.zoom(0.8);
        return;
    }
    zoom = Math.max(1, zoom / 2);
    updateTrace();
};
$("fit").onclick = () => {
    if (activeView === "bpmn") {
        diagram.fit();
        return;
    }
    zoom = 1;
    $("waterfall-scroll").scrollLeft = 0;
    updateTrace();
};
$("close-inspector").onclick = () => document.querySelector(".inspector").classList.remove("open");
document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") document.querySelector(".inspector").classList.remove("open");
});
function startReplay() {
    replaying = true;
    playing = true;
    replayAnchor = performance.now();
    replayAnchorPosition = replayPosition;
}
$("replay").onclick = () => {
    if (playing) playing = false;
    else {
        if (!replaying || replayPosition >= journal.length) replayPosition = 0;
        startReplay();
    }
    updateTrace();
};
$("scrubber").oninput = () => {
    replaying = true;
    playing = false;
    replayPosition = Number($("scrubber").value);
    updateTrace();
};
for (const [id, direction] of [
    ["previous-event", -1],
    ["next-event", 1],
]) {
    $(id).onclick = () => {
        playing = false;
        replayPosition = Math.max(0, Math.min(journal.length, Math.floor(replayPosition) + direction));
        updateTrace();
    };
}
$("exit-replay").onclick = () => {
    replaying = playing = false;
    replayPosition = 0;
    updateTrace();
};
function frame(time) {
    if (time - frameAt > 80 && !document.hidden && workflow && (playing || (live && !terminal(fullModel?.status)))) {
        frameAt = time;
        updateTrace();
    }
    requestAnimationFrame(frame);
}
window.addEventListener("resize", () => {
    updateTrace();
    diagram.fit();
});
window.addEventListener("pagehide", () => stream?.close());
window.addEventListener("pageshow", (event) => {
    if (event.persisted && live) connect();
});
document.addEventListener("visibilitychange", () => {
    if (!document.hidden && live) refresh();
});
// Low-frequency reconciliation also covers a quiet dropped connection or missed invalidation.
setInterval(() => {
    if (live && !document.hidden) refresh();
}, 30000);
refresh();
connect();
requestAnimationFrame(frame);
