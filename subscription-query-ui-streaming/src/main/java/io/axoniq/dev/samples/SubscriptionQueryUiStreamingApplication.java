package io.axoniq.dev.samples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sample application providing an endpoint in the {@link io.axoniq.dev.samples.ui.QueryController} to open a stream of
 * {@link org.springframework.http.codec.ServerSentEvent}s on.
 * <p>
 * This stream is fed by Axon's {@link org.axonframework.queryhandling.QueryGateway#subscriptionQuery(Object, Class,
 * Class)}. The Query Model returned by this subscription query is updated through scheduled "fake" events containing
 * {@link java.util.UUID} strings. The query model updates are made in the {@link io.axoniq.dev.samples.querymodel.ModelProjector},
 * whereas the events are published from the {@link io.axoniq.dev.samples.commandmodel.EventPublisher}.
 *
 * @author Steven van Beelen
 */
@EnableScheduling
@SpringBootApplication
public class SubscriptionQueryUiStreamingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionQueryUiStreamingApplication.class, args);
    }
}
