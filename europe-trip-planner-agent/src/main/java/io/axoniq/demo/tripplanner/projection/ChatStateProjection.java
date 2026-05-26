package io.axoniq.demo.tripplanner.projection;

import io.axoniq.demo.tripplanner.api.StartTripPlanning;
import io.axoniq.demo.tripplanner.api.TripPlanReady;
import io.axoniq.workflow.dsl.agent.AskQuestion;
import io.axoniq.workflow.dsl.agent.HumanAnswered;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event-sourced projection over the trip-planner chat domain.
 * <p>
 * Builds, from the AskQuestion / HumanAnswered / StartTripPlanning / TripPlanReady events, a
 * per-chat record holding:
 * <ul>
 *     <li>the chat id and brief,</li>
 *     <li>the current pending question (if the workflow is waiting on the operator),</li>
 *     <li>the final itinerary (once {@link TripPlanReady} fires),</li>
 *     <li>the full Q&amp;A transcript.</li>
 * </ul>
 *
 * <p>All state is rebuilt on JVM restart from the event store — no external store. Active chats
 * surface on the UI via {@link #all()} so a browser refresh can rediscover any session that the
 * workflow engine is still resuming.</p>
 */
@Component
public class ChatStateProjection {

    private final Map<String, ChatRecord> chats = new ConcurrentHashMap<>();

    @EventHandler
    public void on(StartTripPlanning event) {
        chats.computeIfAbsent(event.id(), id -> new ChatRecord(id, event.brief()));
    }

    @EventHandler
    public void on(AskQuestion event) {
        record(event.chatId()).recordQuestion(event.text());
    }

    @EventHandler
    public void on(HumanAnswered event) {
        record(event.chatId()).recordAnswer(event.text());
    }

    @EventHandler
    public void on(TripPlanReady event) {
        record(event.id()).recordItinerary(event.itinerary());
    }

    /** Snapshot of every known chat, oldest first by id. The UI uses this to list resumable chats. */
    public List<ChatRecord> all() {
        return chats.values().stream()
                    .sorted((a, b) -> a.id().compareTo(b.id()))
                    .toList();
    }

    public ChatRecord get(String id) {
        return chats.get(id);
    }

    public String pendingQuestion(String chatId) {
        ChatRecord rec = chats.get(chatId);
        return rec == null ? null : rec.pendingQuestion();
    }

    public String itinerary(String chatId) {
        ChatRecord rec = chats.get(chatId);
        return rec == null ? null : rec.itinerary();
    }

    private ChatRecord record(String chatId) {
        return chats.computeIfAbsent(chatId, id -> new ChatRecord(id, ""));
    }

    /**
     * Per-chat state. Mutated only from event-handler callbacks, which Axon serialises per
     * processor segment, so the internal collections don't need extra synchronisation.
     */
    public static final class ChatRecord {

        private final String id;
        private final String brief;
        private final List<TranscriptEntry> transcript = new ArrayList<>();
        private volatile String pendingQuestion;
        private volatile String itinerary;

        ChatRecord(String id, String brief) {
            this.id = id;
            this.brief = brief;
        }

        public String id() { return id; }

        public String brief() { return brief; }

        public String pendingQuestion() { return pendingQuestion; }

        public String itinerary() { return itinerary; }

        public Collection<TranscriptEntry> transcript() { return List.copyOf(transcript); }

        /** PENDING while no itinerary; READY once {@link TripPlanReady} has fired. */
        public String status() { return itinerary == null ? "PENDING" : "READY"; }

        void recordQuestion(String text) {
            pendingQuestion = text;
            transcript.add(new TranscriptEntry("agent", text));
        }

        void recordAnswer(String text) {
            pendingQuestion = null;
            transcript.add(new TranscriptEntry("human", text));
        }

        void recordItinerary(String text) {
            pendingQuestion = null;
            itinerary = text;
        }
    }

    public record TranscriptEntry(String role, String text) {
    }
}
