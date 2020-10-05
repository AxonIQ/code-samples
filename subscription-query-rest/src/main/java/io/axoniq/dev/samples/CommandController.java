package io.axoniq.dev.samples;

import io.axoniq.dev.samples.api.CreateMyEntityCommand;
import io.axoniq.dev.samples.api.GetMyEntityByCorrelationIdQuery;
import io.axoniq.dev.samples.query.MyEntity;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * @author Sara Pellegrini
 * @author Stefan Dragisic
 */
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

        /** We are wrapping command into GenericCommandMessage, so we can get its identifier (correlation id) */
        CommandMessage<Object> command = GenericCommandMessage.asCommandMessage(new CreateMyEntityCommand(entityId));

        /** With command identifier we can now subscribe for updates that this command produced */
        GetMyEntityByCorrelationIdQuery query = new GetMyEntityByCorrelationIdQuery(command.getIdentifier());

        /** since we don't care about initial result, we mark it as Void.class */
        SubscriptionQueryResult<Void, MyEntity> response = queryGateway.subscriptionQuery(query,
                                                                                          Void.class,
                                                                                          MyEntity.class);
        return sendAndReturnUpdate(command, response)
                .map(MyEntity::getId);
    }

    public <U> Mono<U> sendAndReturnUpdate(Object command, SubscriptionQueryResult<?, U> result) {
        /** Trick here is to subscribe to initial results first, even it does not return any result
         Subscribing to initialResult creates a buffer for updates, even that we didn't subscribe for updates yet
         they will wait for us in buffer, after this we can safely send command, and then subscribe to updates */
        return Mono.when(result.initialResult())
                .then(Mono.fromCompletionStage(() -> commandGateway.send(command)))
                .thenMany(result.updates())
                .timeout(Duration.ofSeconds(5))
                .next()
                .doFinally(unused -> result.cancel());
        /** dont forget to close subscription query on the end and add a timeout */
    }
}


