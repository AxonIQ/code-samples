package io.axoniq.dev.samples.ui;

import io.axoniq.dev.samples.api.ModelQuery;
import org.axonframework.messaging.queryhandling.QueryResponseMessage;
import org.axonframework.messaging.queryhandling.SubscriptionQueryUpdateMessage;
import org.axonframework.messaging.queryhandling.gateway.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;

/**
 * Simple controller providing a {@link Flux} of {@link ServerSentEvent}s. These {@code ServerSentEvents} contain simple
 * updates messages from th e {@link ModelQuery}.
 *
 * @author Steven van Beelen
 */
@RestController
@RequestMapping(path = "/app")
public class QueryController {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final QueryGateway queryGateway;

    public QueryController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @CrossOrigin(exposedHeaders = "Access-Control-Allow-Origin")
    @GetMapping(path = "/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> updates() {
        // Axon Framework 5's QueryGateway#subscriptionQuery combines the initial result and the updates into a
        // single Publisher of one responseType, and there is no more SubscriptionQueryResult to close explicitly:
        // the underlying subscription is closed automatically once the returned Flux is cancelled/disposed.
        // The ModelQuery's initial result is a List<String>, while every emitted update is a single String. To
        // keep that shape, we fall back to the mapper-based subscriptionQuery overload, which lets us
        // distinguish the initial result from an update via the message type, and flatten the initial
        // List<String> into individual elements ourselves (mirroring the old initialResult().flatMapMany(...)).
        Flux<ServerSentEvent<String>> sseStream =
                Flux.from(queryGateway.subscriptionQuery(new ModelQuery(), Object.class, QueryController::mapResponse))
                    .flatMap(response -> response instanceof List<?> initialResult
                            ? Flux.fromIterable(initialResult).cast(String.class)
                            : Flux.just((String) response))
                    .doOnError(throwable -> logger.warn("something failed"))
                    .map(update -> ServerSentEvent.<String>builder()
                                                  .event("update")
                                                  .data(update)
                                                  .build());

        // For Server Sent Events, the server doesn't get a close signal when the client closes the connection.
        // Hence, we are left with a hanging stream in that case.
        // A workaround is to implement heart beats, which will detect that no one is listening on the other side.
        // This closes the stream automatically.
        Flux<ServerSentEvent<String>> heartbeatStream = Flux.interval(Duration.ofSeconds(2))
                                                            .map(i -> ServerSentEvent.<String>builder()
                                                                                     .event("ping")
                                                                                     .build());
        return Flux.merge(sseStream, heartbeatStream);
    }

    private static Object mapResponse(QueryResponseMessage response) {
        return response instanceof SubscriptionQueryUpdateMessage
                ? response.payloadAs(String.class)
                : response.payloadAs(List.class);
    }
}
