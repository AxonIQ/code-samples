package io.axoniq.service;

import io.axoniq.axonserver.connector.ResultStreamPublisher;
import io.axoniq.axonserver.connector.admin.AdminChannel;
import io.axoniq.axonserver.grpc.admin.Result;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * @author Sara Pellegrini
 * @since 1.1
 */
@Service
public class ServerConnectorEventProcessorService implements EventProcessorService {

    private final Configuration configuration;
    private final AdminChannel adminChannel;

    public ServerConnectorEventProcessorService(Configuration configuration,
                                                AdminChannel adminChannel) {
        this.configuration = configuration;
        this.adminChannel = adminChannel;
    }

    @Override
    public Mono<Void> pause(String processorName) {
        return pause(processorName, tokenStoreId(processorName));
    }

    @Override
    public Mono<Void> start(String processorName) {
        return start(processorName, tokenStoreId(processorName));
    }

    @Override
    public Mono<Void> reset(String processorName) {
        StreamingEventProcessor eventProcessor = eventProcessor(processorName);
        String tokenStoreIdentifier = tokenStoreId(processorName);

        return pause(processorName, tokenStoreIdentifier)
                .then(resetTokens(eventProcessor))
                .then(start(processorName, tokenStoreIdentifier));
    }

    /*
     * Resets the token of an event processor. The event processor needs to be stopped for this to work
     */
    private Mono<Void> resetTokens(StreamingEventProcessor eventProcessor) {
        return Mono.fromRunnable(eventProcessor::resetTokens);
    }

    /*
     * Starts event processors and ensures that either the axon server waits for them to have started or we wait locally
     * for them to reach the desired state (pre 4.6)
     */
    protected Mono<Void> start(String eventProcessorName, String tokenStoreIdentifier) {
        return Mono.fromFuture(() -> adminChannel.startEventProcessor(eventProcessorName, tokenStoreIdentifier))
                   .filter(Result.SUCCESS::equals)
                   .switchIfEmpty(awaitForStatus(eventProcessorName, tokenStoreIdentifier, true))
                   .then();
    }

    /*
     * Stops event processors and ensures that either the axon server waits for them to have stopped or we wait locally
     * for them to reach the desired state (pre 4.6)
     */
    protected Mono<Void> pause(String eventProcessorName, String tokenStoreIdentifier) {
        return Mono.fromFuture(() -> adminChannel.pauseEventProcessor(eventProcessorName, tokenStoreIdentifier))
                   .filter(Result.SUCCESS::equals)
                   .switchIfEmpty(awaitForStatus(eventProcessorName, tokenStoreIdentifier, false))
                   .then();
    }

    /*
        Older versions of the Axon Framework return execution ACK immediately, without waiting for EventProcessors to
        start or stop. Newer versions return execution ACK after the EventProcessor has been actually started or stopped.
        For older clients, we use this method to poll the status of the event processors, to ensure they have started
        or stopped. In case you are using a client >= 4.6, this is no longer needed.
     */
    protected Mono<Result> awaitForStatus(String eventProcessorName, String tokenStoreIdentifier, boolean running) {
        return Flux.from(new ResultStreamPublisher<>(adminChannel::eventProcessors))
                   .filter(eventProcessor -> eventProcessor.getIdentifier().getProcessorName()
                                                           .equals(eventProcessorName))
                   .filter(eventProcessor -> eventProcessor.getIdentifier().getTokenStoreIdentifier()
                                                           .equals(tokenStoreIdentifier))
                   .flatMap(eventProcessor -> Flux.fromIterable(eventProcessor.getClientInstanceList()))
                   .map(clientInstance -> clientInstance.getIsRunning() == running)
                   .reduce(Boolean::logicalAnd)
                   .filter(result -> result.equals(true))
                   .switchIfEmpty(Mono.error(new RuntimeException("")))
                   .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                   .thenReturn(Result.SUCCESS);
    }


    private String tokenStoreId(String processorName) {
        StreamingEventProcessor eventProcessor = eventProcessor(processorName);
        return eventProcessor.getTokenStoreIdentifier();
    }

    private StreamingEventProcessor eventProcessor(String processorName) {
        return configuration.eventProcessingConfiguration()
                            .eventProcessorByProcessingGroup(
                                    processorName,
                                    StreamingEventProcessor.class)
                            .orElseThrow(IllegalArgumentException::new);
    }
}