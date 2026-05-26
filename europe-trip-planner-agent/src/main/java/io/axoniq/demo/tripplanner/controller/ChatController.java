package io.axoniq.demo.tripplanner.controller;

import io.axoniq.demo.tripplanner.api.StartTripPlanning;
import io.axoniq.demo.tripplanner.projection.ChatStateProjection;
import io.axoniq.demo.tripplanner.projection.ChatStateProjection.ChatRecord;
import io.axoniq.workflow.dsl.agent.HumanAnswered;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.eventhandling.EventSink;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simple REST surface over the planner workflow.
 *
 * <pre>
 * POST   /chats                              — start a new trip-planning chat
 *                                              { "brief": "weekend in Paris" } → {id, message}
 * GET    /chats                              — list every known chat (resumable + completed)
 * GET    /chats/{id}                         — get the current state of a chat
 * GET    /chats/{id}/question                — get the pending question if any
 * GET    /chats/{id}/transcript              — get the full Q&A history (so the UI can re-render
 *                                              past messages after a browser refresh)
 * POST   /chats/{id}/answer                  — answer the pending question
 *                                              { "text": "Paris and Rome" }
 * GET    /chats/{id}/itinerary               — get the final itinerary when ready
 * </pre>
 */
@RestController
@RequestMapping("/chats")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final EventSink eventSink;
    private final MessageTypeResolver typeResolver;
    private final EventConverter converter;
    private final ChatStateProjection state;

    public ChatController(EventSink eventSink,
                          MessageTypeResolver typeResolver,
                          @Autowired(required = false) EventConverter converter,
                          ChatStateProjection state) {
        this.eventSink = eventSink;
        this.typeResolver = typeResolver;
        this.converter = converter;
        this.state = state;
    }

    @PostMapping
    public Map<String, String> start(@RequestBody Map<String, String> body) {
        String brief = body.getOrDefault("brief", "I want a European city trip — please plan one.");
        String id = UUID.randomUUID().toString();
        publish(StartTripPlanning.class, new StartTripPlanning(id, brief));
        logger.info("Started trip-planner chat {}", id);
        return Map.of("id", id, "message", "Trip planning started. Poll /chats/" + id + "/question.");
    }

    @GetMapping
    public List<Map<String, Object>> listChats() {
        return state.all().stream().map(ChatController::summary).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> status(@PathVariable("id") String id) {
        ChatRecord rec = state.get(id);
        if (rec == null) {
            return Map.of("id", id, "status", "UNKNOWN");
        }
        return summary(rec);
    }

    @GetMapping("/{id}/transcript")
    public ResponseEntity<List<Map<String, String>>> transcript(@PathVariable("id") String id) {
        ChatRecord rec = state.get(id);
        if (rec == null) {
            return ResponseEntity.noContent().build();
        }
        var entries = rec.transcript().stream()
                         .map(e -> Map.of("role", e.role(), "text", e.text()))
                         .toList();
        return ResponseEntity.ok(entries);
    }

    private static Map<String, Object> summary(ChatRecord rec) {
        return Map.of(
                "id", rec.id(),
                "brief", rec.brief(),
                "status", rec.status(),
                "pendingQuestion", rec.pendingQuestion() == null ? "" : rec.pendingQuestion(),
                "itineraryReady", rec.itinerary() != null,
                "itinerary", rec.itinerary() == null ? "" : rec.itinerary()
        );
    }

    @GetMapping("/{id}/question")
    public ResponseEntity<Map<String, String>> question(@PathVariable("id") String id) {
        String q = state.pendingQuestion(id);
        if (q == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("question", q));
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<Void> answer(@PathVariable("id") String id,
                                       @RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        publish(HumanAnswered.class, new HumanAnswered(id, text));
        logger.info("Operator answered chat {}: '{}'", id, text);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/itinerary")
    public ResponseEntity<Map<String, String>> itinerary(@PathVariable("id") String id) {
        String itinerary = state.itinerary(id);
        if (itinerary == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("itinerary", itinerary));
    }

    private void publish(Class<?> eventType, Object payload) {
        MessageType type = typeResolver.resolveOrThrow(eventType);
        var msg = new GenericEventMessage(type, payload);
        if (converter != null) {
            msg = (GenericEventMessage) msg.withConverter(converter);
        }
        eventSink.publish(null, msg);
    }
}
