package io.axoniq.framework;

import org.axonframework.common.configuration.Configuration;
import org.axonframework.messaging.eventhandling.processing.streaming.StreamingEventProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("framework")
public class FrameworkEventProcessorRestController {

    private final Configuration configuration;

    public FrameworkEventProcessorRestController(Configuration configuration) {
        this.configuration = configuration;
    }

    @GetMapping("reset/{processorName}")
    public ResponseEntity<Void> reset(@PathVariable String processorName) {
        Assert.hasLength(processorName, "Processing Group is mandatory and can't be empty!");

        // AF5 removed the (Tracking)EventProcessor-by-processing-group lookup API. Every StreamingEventProcessor
        // (nowadays always a PooledStreamingEventProcessor, since TrackingEventProcessor has been removed) is
        // available as a named component instead, keyed by its processing group / processor name.
        StreamingEventProcessor streamingEventProcessor =
                configuration.getComponents(StreamingEventProcessor.class).get(processorName);
        if (streamingEventProcessor != null && streamingEventProcessor.supportsReset()) {
            // shutdown/resetTokens/start are asynchronous in AF5 (they return a CompletableFuture instead of
            // blocking void calls). We chain and join them here to keep this endpoint's original blocking
            // behavior: the response is only returned once the reset has fully completed.
            // This sample has no data worth preserving, so we simply reset all tokens to the start of the
            // stream (the "reset to latest"/full-replay strategy) rather than migrating a stored token's
            // (now mandatory) mask column programmatically.
            streamingEventProcessor.shutdown()
                                   .thenCompose(ignored -> streamingEventProcessor.resetTokens())
                                   .thenCompose(ignored -> streamingEventProcessor.start())
                                   .join();
        }

        return ResponseEntity.ok().build();
    }
}
