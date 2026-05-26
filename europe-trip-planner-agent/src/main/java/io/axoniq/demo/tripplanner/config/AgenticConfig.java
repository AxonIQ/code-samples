package io.axoniq.demo.tripplanner.config;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.Gemini;
import io.axoniq.workflow.dsl.agent.AgenticWorkflowContextFactory;
import io.axoniq.workflow.runtime.api.execution.context.EventNameCustomizer;
import io.axoniq.workflow.runtime.api.execution.status.StepStatus;
import io.axoniq.workflow.runtime.api.execution.status.WorkflowStatus;
import io.axoniq.workflow.runtime.execution.DefaultEventNameCustomizer;
import org.axonframework.common.configuration.ConfigurationEnhancer;
import org.axonframework.messaging.core.QualifiedName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Spring wiring for the agentic DSL.
 * <p>
 * Exposes a {@link AgenticWorkflowContextFactory} bean — the workflow Spring Boot starter
 * detects this and routes any {@code @Workflow} method whose context parameter is
 * {@code AgenticWorkflowContext} through it. Also defines a {@code Supplier<BaseLlm>} so the
 * workflow can ask for a fresh {@link Gemini} client per agent run.
 */
@Configuration
public class AgenticConfig {

    @Bean
    public AgenticWorkflowContextFactory agenticWorkflowContextFactory() {
        return new AgenticWorkflowContextFactory();
    }

    /**
     * Wraps whatever {@link EventNameCustomizer} the engine registers so it sees step names with
     * the {@code _N} occurrence suffix stripped. The agentic DSL's {@code AdapterStepNamer}
     * produces unique step names ({@code Reason}, {@code Reason_2}, {@code answer_3}, …) so the
     * engine can keep one journal slot per invocation; at the event-store level we'd rather see
     * one stable event type per kind ({@code ReasonStarted}, {@code answerCompleted}, …).
     * Metadata still carries the full step name, so engine state evolution is unaffected.
     *
     * <p>A {@link ConfigurationEnhancer} that registers a {@code decorator} (not a replacement
     * component) sidesteps registration ordering: whichever customizer the engine actually
     * builds, we wrap it.</p>
     */
    @Bean
    public ConfigurationEnhancer agenticEventNameCustomizerEnhancer() {
        return registry -> registry.registerDecorator(
                EventNameCustomizer.class,
                0,
                (config, name, delegate) -> wrapStrippingSuffix(delegate));
    }

    private static EventNameCustomizer wrapStrippingSuffix(EventNameCustomizer delegate) {
        return new EventNameCustomizer() {
            @Override
            public QualifiedName getEventName(String stepName, Map<String, Object> params, StepStatus status) {
                return delegate.getEventName(stripOccurrenceSuffix(stepName), params, status);
            }

            @Override
            public QualifiedName getEventName(String stepName, Map<String, Object> params, WorkflowStatus status) {
                return delegate.getEventName(stripOccurrenceSuffix(stepName), params, status);
            }

            @Override
            public EventNameCustomizer forStepInheritance() {
                return wrapStrippingSuffix(delegate.forStepInheritance());
            }

            private String stripOccurrenceSuffix(String name) {
                return name.replaceFirst("_\\d+$", "");
            }
        };
    }

    @Bean
    public Supplier<BaseLlm> geminiLlmSupplier(
            @Value("${trip-planner.gemini.model:gemini-2.5-flash}") String model,
            @Value("${trip-planner.gemini.api-key:}") String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Missing GOOGLE_API_KEY — set the env var (or trip-planner.gemini.api-key) "
                            + "with a valid Google AI Studio API key before starting the app."
            );
        }
        return () -> new Gemini(model, apiKey);
    }
}
