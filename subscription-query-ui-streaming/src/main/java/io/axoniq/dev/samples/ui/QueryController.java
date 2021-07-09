package io.axoniq.dev.samples.ui;

import io.axoniq.dev.samples.api.ModelQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
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
        SubscriptionQueryResult<List<String>, String> result = queryGateway
                .subscriptionQuery(new ModelQuery(),
                                   ResponseTypes.multipleInstancesOf(String.class),
                                   ResponseTypes.instanceOf(String.class));

        Flux<ServerSentEvent<String>> sseStream = result.initialResult()
                                                        .flatMapMany(Flux::fromIterable)
                                                        .concatWith(result.updates())
                                                        .doOnError(throwable -> logger.warn("something failed"))
                                                        .map(update -> ServerSentEvent.<String>builder()
                                                                                      .event("update")
                                                                                      .data(update)
                                                                                      .build())
                                                        .doFinally(signal -> result.close());

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
}
