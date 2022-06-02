package io.axoniq.service;

import reactor.core.publisher.Mono;

public interface EventProcessorService {
    Mono<Void> pause(String processorName);

    Mono<Void> start(String processorName);

    Mono<Void> reset(String processorName);

//    Mono<Void> awaitTermination(String processorName);
}
