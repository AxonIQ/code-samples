package io.axoniq.demo.orderfulfillment.history;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Notifications invalidate browser caches; the database remains the source of truth. */
@Component
public class WorkflowHistoryUpdates {
    private final Set<SseEmitter> clients = ConcurrentHashMap.newKeySet();
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService sender = Executors.newSingleThreadScheduledExecutor(task -> {
        var thread = new Thread(task, "workflow-history-stream");
        thread.setDaemon(true);
        return thread;
    });

    public WorkflowHistoryUpdates() {
        sender.scheduleWithFixedDelay(this::flush, 100, 100, TimeUnit.MILLISECONDS);
        sender.scheduleWithFixedDelay(() -> clients.forEach(client -> send(client, "heartbeat", clock())),
                                      15, 15, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe() {
        var client = new SseEmitter(0L);
        clients.add(client);
        client.onCompletion(() -> clients.remove(client));
        client.onTimeout(() -> clients.remove(client));
        client.onError(error -> clients.remove(client));
        send(client, "ready", clock());
        return client;
    }

    /** Called after commit. Coalescing avoids blocking the projector on slow browsers. */
    public void publish(String workflowId) {
        pending.add(workflowId);
    }

    private void flush() {
        for (String id : pending) {
            if (pending.remove(id)) {
                clients.forEach(client -> send(client, "workflow", Map.of("workflowId", id)));
            }
        }
    }

    private Map<String, Long> clock() {
        return Map.of("serverTime", System.currentTimeMillis());
    }

    private void send(SseEmitter client, String event, Object data) {
        try {
            client.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException error) {
            clients.remove(client);
            client.complete();
        }
    }

    @PreDestroy
    void close() {
        sender.shutdownNow();
        clients.forEach(SseEmitter::complete);
        clients.clear();
    }
}
