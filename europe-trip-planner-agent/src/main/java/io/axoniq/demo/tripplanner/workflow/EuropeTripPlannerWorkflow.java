package io.axoniq.demo.tripplanner.workflow;

import com.google.adk.models.BaseLlm;
import io.axoniq.demo.tripplanner.api.StartTripPlanning;
import io.axoniq.demo.tripplanner.api.TripPlanReady;
import io.axoniq.workflow.dsl.agent.AgentDefinition;
import io.axoniq.workflow.dsl.agent.AgenticWorkflowContext;
import io.axoniq.workflow.dsl.agent.adk.AdkHostAgent;
import io.axoniq.workflow.runtime.api.annotation.Workflow;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.eventhandling.EventSink;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

import static io.axoniq.workflow.runtime.execution.DefaultEventNameCustomizer.Builder.defaults;

/**
 * Durable, replayable agentic conversation:
 *
 * <ol>
 *     <li>HTTP {@code POST /chats} publishes a {@link StartTripPlanning} event;</li>
 *     <li>this workflow starts (one instance per chat id);</li>
 *     <li>it spawns a Gemini-backed agent via {@link AdkHostAgent};</li>
 *     <li>Gemini asks {@code askQuestion} tool calls — the workflow publishes {@code AskQuestion}
 *         events for each;</li>
 *     <li>the REST controller forwards those questions to the operator (human in the loop);</li>
 *     <li>the operator POSTs an answer → a {@code HumanAnswered} event;</li>
 *     <li>the workflow durably resumes and the agent eventually returns an itinerary;</li>
 *     <li>workflow publishes {@link TripPlanReady} and completes.</li>
 * </ol>
 *
 * Crash anywhere in this chain and the journal replays from the last completed step. Gemini is
 * not re-invoked for completed turns; the human is not asked questions twice.
 */
@Component
public class EuropeTripPlannerWorkflow {

    private static final Logger logger = LoggerFactory.getLogger(EuropeTripPlannerWorkflow.class);

    private final Supplier<BaseLlm> llmSupplier;

    public EuropeTripPlannerWorkflow(Supplier<BaseLlm> llmSupplier) {
        this.llmSupplier = llmSupplier;
    }

    @Workflow(
            idProperty = "id",
            startOnEventClass = StartTripPlanning.class,
            workflowName = "EuropeTripPlannerWorkflow"
    )
    public void execute(AgenticWorkflowContext ctx) {
        var payload = ctx.workflowPayload();
        var tripId = (String) payload.get("id");
        var brief = (String) payload.get("brief");
        logger.info("Trip planning started for {}: {}", tripId, brief);

        // The agent definition: a system prompt, a host adapter, and (implicitly) the built-in
        // askQuestion tool — auto-registered by AgentDefinition. The adapter wires everything
        // through to ADK internally; no LlmAgent, no bridge tool to build by hand.
        var definition = AgentDefinition.builder()
                                        .systemPrompt("""
                                                You are a concise European travel planner. To plan a great
                                                trip, use the `askQuestion` tool 3 times to ask the user, one
                                                question per call:
                                                   1. Which cities or regions in Europe interest them?
                                                   2. How many days they have for the whole trip?
                                                   3. Their daily budget.
                                                After receiving the three answers, produce a short day-by-day
                                                itinerary as your final reply — no more tool calls.
                                                """)
                                        .hostAgent(AdkHostAgent.of(llmSupplier.get()))
                                        .maxIterations(8)
                                        .build();

        var agentRun = ctx.runAgent("ResearchAgent1", definition, brief);
        agentRun.await();

        var itinerary = agentRun.resultAs(Map.class)
                                .map(m -> (String) m.get("text"))
                                .orElse("");
        logger.info("Trip {} planning complete. Itinerary:\n{}", tripId, itinerary);

        // Publish the durable "I'm done, here's the itinerary" event.
        var eventSink = ctx.processingContext().component(EventSink.class);
        var resolver = ctx.processingContext().component(MessageTypeResolver.class);
        var converter = ctx.processingContext().component(EventConverter.class);
        ctx.execute(
                "publishItinerary",
                Map.of(),
                (pc, p) -> {
                    var event = new TripPlanReady(tripId, itinerary);
                    var msg = new GenericEventMessage(resolver.resolveOrThrow(TripPlanReady.class), event);
                    if (converter != null) {
                        msg = (GenericEventMessage) msg.withConverter(converter);
                    }
                    eventSink.publish(null, msg);
                    return Map.of();
                },
                Duration.ofSeconds(10),
                defaults()
        ).await();
    }
}
