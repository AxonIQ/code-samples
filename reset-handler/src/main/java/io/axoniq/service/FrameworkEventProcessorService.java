package io.axoniq.service;

import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service
public class FrameworkEventProcessorService implements EventProcessorService {

    private final Configuration configuration;

    public FrameworkEventProcessorService(Configuration configuration) {
        this.configuration = configuration;
    }


    @Override
    public Mono<Void> pause(String processorName) {
        return Mono.fromFuture(
                configuration.eventProcessingConfiguration()
                        .eventProcessorByProcessingGroup(processorName,StreamingEventProcessor.class)
                        .map(EventProcessor::shutdownAsync)
                        .orElse(CompletableFuture.completedFuture(null))
        );
    }

    @Override
    public Mono<Void> start(String processorName) {
        configuration.eventProcessingConfiguration()
                .eventProcessorByProcessingGroup(processorName,StreamingEventProcessor.class)
                .ifPresent(EventProcessor::start);
        return Mono.empty();
    }

    @Override
    public Mono<Void> reset(String processorName) {
        configuration.eventProcessingConfiguration()
                .eventProcessorByProcessingGroup(processorName,
                        StreamingEventProcessor.class)
                .ifPresent(streamingEventProcessor -> {
                    if (streamingEventProcessor.supportsReset()) {
                        streamingEventProcessor.shutDown();
                        streamingEventProcessor.resetTokens();
                        streamingEventProcessor.start();
                    }
                });
        return Mono.empty();
    }

}
