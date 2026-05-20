package io.axoniq.demo.versioning.controller;

import io.axoniq.demo.versioning.api.OrderPlacedEvent;
import io.axoniq.demo.versioning.api.PaymentReceivedEvent;
import io.axoniq.workflow.history.api.WorkflowHistory;
import io.axoniq.workflow.history.api.WorkflowHistoryRepository;
import org.axonframework.messaging.eventhandling.gateway.EventGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints to drive the demo:
 *
 * <ul>
 *   <li>{@code POST /orders} with a JSON body to place an order (starts a workflow).</li>
 *   <li>{@code POST /orders/{orderId}/payment} to signal payment received (resumes the wait step).</li>
 *   <li>{@code GET  /orders/{orderId}} to inspect the workflow's projected state.</li>
 * </ul>
 */
@RestController
public class DemoController {

    private final EventGateway eventGateway;
    private final WorkflowHistoryRepository historyRepository;

    public DemoController(EventGateway eventGateway,
                          WorkflowHistoryRepository historyRepository) {
        this.eventGateway = eventGateway;
        this.historyRepository = historyRepository;
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> place(@RequestBody PlaceOrderRequest req) {
        eventGateway.publish(null, new OrderPlacedEvent(req.orderId(), req.customerId(), req.amount()));
        return ResponseEntity.accepted().body(Map.of("orderId", req.orderId(), "status", "placed"));
    }

    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<Map<String, Object>> pay(@PathVariable String orderId,
                                                    @RequestBody PayRequest req) {
        eventGateway.publish(null, new PaymentReceivedEvent(orderId, req.amount()));
        return ResponseEntity.accepted().body(Map.of("orderId", orderId, "amount", req.amount()));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> get(@PathVariable String orderId) {
        // Find ALL workflow instances for this orderId — both the base id and any cross-version
        // disambiguated variants ("<orderId>#<version>"). With the engine's cross-version routing,
        // two versions of the same domain key can run in parallel, and both should be visible to the
        // demo UI.
        var matches = findAllByOrderId(orderId);
        if (matches.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(Map.of("orderId", orderId, "status", "not found"));
        }
        var view = new LinkedHashMap<String, Object>();
        view.put("orderId", orderId);
        view.put("instances", matches.stream().map(this::renderInstance).toList());
        return ResponseEntity.ok(view);
    }

    private List<WorkflowHistory> findAllByOrderId(String orderId) {
        var prefix = orderId + "#";
        var found = new ArrayList<WorkflowHistory>();
        historyRepository.findById(orderId).ifPresent(found::add);
        for (var h : historyRepository.findAll()) {
            if (h.workflowId().startsWith(prefix)) {
                found.add(h);
            }
        }
        return found;
    }

    private Map<String, Object> renderInstance(WorkflowHistory history) {
        var state = history.state();
        var view = new LinkedHashMap<String, Object>();
        view.put("workflowId", history.workflowId());
        view.put("workflowVersion", state.currentWorkflowVersion());
        view.put("workflowStatus", state.workflowStatus().name());
        view.put("stepsInHistory", state.workflowStepNames());
        view.put("payload", state.payload());
        view.put("addressValidationVersion", state.version("address-validation"));
        view.put("hasAddressValidationMarker", state.hasVersion("address-validation"));
        return view;
    }

    public record PlaceOrderRequest(String orderId, String customerId, int amount) {}
    public record PayRequest(int amount) {}
}
