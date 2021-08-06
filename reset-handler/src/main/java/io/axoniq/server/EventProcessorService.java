package io.axoniq.server;

import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class EventProcessorService {

    private final WebClient webClient;

    private final Supplier<String> contextSupplier;
    private final Supplier<String> componentSupplier;
    private final Supplier<String> tokenStoreIdSupplier;

    public EventProcessorService(@Value("${axon.axonserver.context}") String context,
                                 @Value("${axon.axonserver.component-name}") String component,
                                 TokenStore tokenStore) {
        this.webClient = WebClient.create("http://localhost:8024");
        this.contextSupplier = () -> context;
        this.componentSupplier = () -> component;
        this.tokenStoreIdSupplier = () -> tokenStore.retrieveStorageIdentifier().get();
    }

    /**
     * Requires Axon Server to pause every known instance of the given Processor Name.
     *
     * @param processorName Name of the processor to be paused.
     * @return Returns a Mono that completes when the request has been accepted by Axon Server.
     */
    public Mono<Void> pause(String processorName) {
        return webClient.patch()
                        .uri("/v1/components/{component}/processors/{processor}/pause?context={context}&tokenStoreIdentifier={tokenStoreId}",
                             componentSupplier.get(),
                             processorName,
                             contextSupplier.get(),
                             tokenStoreIdSupplier.get())
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toBodilessEntity()
                        .then();
    }

    /**
     * Requires Axon Server to start every known instance of the given Processor Name.
     *
     * @param processorName Name of the processor to be started.
     * @return Returns a Mono that completes when the request has been accepted by Axon Server.
     */
    public Mono<Void> start(String processorName) {
        return webClient.patch()
                        .uri("/v1/components/{component}/processors/{processor}/start?context={context}&tokenStoreIdentifier={tokenStoreId}",
                             componentSupplier.get(),
                             processorName,
                             contextSupplier.get(),
                             tokenStoreIdSupplier.get())
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toBodilessEntity()
                        .then();
    }

    /**
     * Check if the given Processor Name has already terminated. It will retry 10 times, one every second and fail if it
     * is not terminated yet. To check if it is terminated, the method looks into the activeThreads number.
     *
     * @param processorName Name of the processor to wait for termination.
     * @return Returns a Mono that completes when processor is terminated.
     */
    public Mono<Void> awaitTermination(String processorName) {
        return webClient.get()
                        .uri("/v1/components/{component}/processors?context={context}",
                             componentSupplier.get(),
                             contextSupplier.get())
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        })
                        .flatMapIterable(Function.identity())
                        .filter(processor -> processorName.equals(processor.get("name")))
                        .filter(processor -> (Integer) processor.get("activeThreads") == 0)
                        .switchIfEmpty(Mono.error(new RuntimeException()))
                        .retryWhen(Retry.fixedDelay(10, Duration.of(1000, ChronoUnit.MILLIS)))
                        .then();
    }
}
