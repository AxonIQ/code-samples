package io.axoniq.demo.orderfulfillment.controller;

import io.axoniq.demo.orderfulfillment.history.JdbcWorkflowHistoryRepository;
import io.axoniq.demo.orderfulfillment.history.WorkflowHistoryUpdates;
import io.axoniq.framework.workflow.dsl.api.WorkflowStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowHistoryController {

    private final JdbcWorkflowHistoryRepository history;
    private final WorkflowHistoryUpdates updates;

    public WorkflowHistoryController(JdbcWorkflowHistoryRepository history, WorkflowHistoryUpdates updates) {
        this.history = history;
        this.updates = updates;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return updates.subscribe();
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "") String search,
                                    @RequestParam(required = false) WorkflowStatus status,
                                    @RequestParam(defaultValue = "50") int limit,
                                    @RequestParam(defaultValue = "0") int offset) {
        if (limit < 1 || limit > 200 || offset < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use limit 1–200 and offset >= 0");
        }
        return history.list(search, status == null ? "" : status.name(), limit, offset);
    }

    @GetMapping("/{workflowId}")
    public Object detail(@PathVariable String workflowId) {
        return history.detail(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
    }

    @GetMapping("/{workflowId}/events")
    public List<Map<String, Object>> events(@PathVariable String workflowId,
                                           @RequestParam(defaultValue = "0") long after,
                                           @RequestParam(defaultValue = "200") int limit) {
        if (after < 0 || limit < 1 || limit > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use limit 1–500 and after >= 0");
        }
        detail(workflowId);
        return history.timeline(workflowId, after, limit);
    }
}
