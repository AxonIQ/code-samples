package io.axoniq.server;

import org.axonframework.common.configuration.Configuration;
import org.axonframework.messaging.eventhandling.processing.streaming.StreamingEventProcessor;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("server")
public class ServerEventProcessorRestController {

    private final EventProcessorService eventProcessorService;
    private final Configuration configuration;


    public ServerEventProcessorRestController(EventProcessorService eventProcessorService,
                                              Configuration configuration) {
        this.eventProcessorService = eventProcessorService;
        this.configuration = configuration;
    }

    @GetMapping("start/{processorName}")
    public Mono<Void> start(@PathVariable String processorName) {
        return eventProcessorService.start(processorName);
    }

    @GetMapping("pause/{processorName}")
    public Mono<Void> pause(@PathVariable String processorName) {
        return eventProcessorService.pause(processorName);
    }

    @GetMapping("reset/{processorName}")
    public Mono<Void> reset(@PathVariable String processorName) {
        Assert.hasLength(processorName, "Processor Name is mandatory and can't be empty!");
        // AF5 removed the (Tracking)EventProcessor-by-processing-group lookup API. Every StreamingEventProcessor
        // (nowadays always a PooledStreamingEventProcessor) is available as a named component instead, keyed by
        // its processing group / processor name.
        StreamingEventProcessor eventProcessor = configuration.getComponents(StreamingEventProcessor.class)
                                                              .get(processorName);
        if (eventProcessor == null) {
            throw new IllegalArgumentException("Unknown processor: " + processorName);
        }

        // resetTokens() now returns a CompletableFuture<Void> instead of blocking, so we bridge it into the
        // reactive chain with Mono.fromFuture instead of Mono.fromRunnable.
        // This sample has no data worth preserving, so we simply reset all tokens to the start of the stream
        // (the "reset to latest"/full-replay strategy) rather than migrating a stored token's (now mandatory)
        // mask column programmatically.
        return eventProcessorService.pause(processorName)
                                    .then(eventProcessorService.awaitTermination(processorName))
                                    .then(Mono.fromFuture(eventProcessor::resetTokens))
                                    .then(eventProcessorService.start(processorName));
    }
}
