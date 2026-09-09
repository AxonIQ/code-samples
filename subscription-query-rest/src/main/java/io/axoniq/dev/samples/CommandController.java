package io.axoniq.dev.samples;

import io.axoniq.dev.samples.api.CreateMyEntityCommand;
import io.axoniq.dev.samples.api.GetMyEntityByCorrelationIdQuery;
import io.axoniq.dev.samples.query.MyEntity;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.queryhandling.gateway.QueryGateway;
import org.reactivestreams.Publisher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class CommandController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public CommandController(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    @PostMapping("/entities/{id}")
    public Mono<String> myApi(@PathVariable("id") String entityId) {

        /* We are wrapping the command into a GenericCommandMessage, so we can get its identifier (correlation id) */
        CommandMessage command = new GenericCommandMessage(new MessageType(CreateMyEntityCommand.class),
                                                            new CreateMyEntityCommand(entityId));

        /* With the command identifier we can now subscribe for updates that this command produced */
        GetMyEntityByCorrelationIdQuery query = new GetMyEntityByCorrelationIdQuery(command.identifier());

        /* Axon Framework 5 merges the "virtual" initial result and the updates into a single Publisher,
         so there is only one responseType now instead of separate initial/update types */
        Publisher<MyEntity> updates = queryGateway.subscriptionQuery(query, MyEntity.class);

        return sendAndReturnUpdate(command, updates)
                .map(MyEntity::id);
    }

    public Mono<MyEntity> sendAndReturnUpdate(Object command, Publisher<MyEntity> updates) {
        /* The trick here is to subscribe to the subscription query's Publisher first: subscribing is what sends
         the query and opens the buffer for updates. We hook the command dispatch into doOnSubscribe so it only
         fires once that buffer is open, guaranteeing we cannot miss the update it produces. Cancelling (closing)
         the subscription query is handled automatically once next() receives its element or the timeout fires. */
        return Flux.from(updates)
                .doOnSubscribe(subscription -> commandGateway.send(command))
                .timeout(Duration.ofSeconds(5))
                .next();
    }
}


