package io.axoniq.multitenancy.web;

import io.axoniq.multitenancy.api.AddCmd;
import io.axoniq.multitenancy.api.FindAllGiftCardQry;
import io.axoniq.multitenancy.api.FindGiftCardQry;
import io.axoniq.multitenancy.api.GiftCardRecord;
import io.axoniq.multitenancy.api.IssueCmd;
import io.axoniq.multitenancy.api.IssuedEvt;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.GenericQueryMessage;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository REST Controller for handling 'commands' only
 * <p>
 * Sometimes you may want to write a custom handler for a specific resource. To take advantage of Spring Data REST’s
 * settings, message converters, exception handling, and more, we use the @RepositoryRestController annotation instead
 * of a standard Spring MVC @Controller or @RestController
 */
@org.springframework.web.bind.annotation.RestController
public class RestController {


    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final EventBus eventBus;
    private final QueryBus queryBus;
    private final Configuration configuration;

    public RestController(CommandGateway commandGateway, QueryGateway queryGateway, EventBus eventBus,
                          QueryBus queryBus, Configuration configuration) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
        this.eventBus = eventBus;
        this.queryBus = queryBus;
        this.configuration = configuration;

        this.commandGateway.registerDispatchInterceptor(new HeaderTenantInterceptor<>());
        this.queryGateway.registerDispatchInterceptor(new HeaderTenantInterceptor<>());
    }

    @GetMapping(value = "/send/query")
    public void sendQuery(@RequestParam int count, @RequestParam int delay)
            throws InterruptedException {
        for (int i = 0; i < count; i++) {
            log.info("Sending {}/{} queries delayed by {}", i + 1, count, delay);

            QueryMessage<FindAllGiftCardQry, List<GiftCardRecord>> query = new GenericQueryMessage<>(new FindAllGiftCardQry(),
                                                                                                       ResponseTypes.multipleInstancesOf(
                                                                                                               GiftCardRecord.class));

            queryGateway.query(query, ResponseTypes.multipleInstancesOf(GiftCardRecord.class));
            Thread.sleep(delay);
        }
    }

    @GetMapping(value = "/send/query/subscription", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<String> subscriptionQuery(@RequestParam int count, @RequestParam int delay, @RequestParam int worktime)
            throws InterruptedException {

        final String giftCardId = UUID.randomUUID().toString();

        QueryMessage<FindGiftCardQry, Optional<GiftCardRecord>> query = new GenericQueryMessage<>(new FindGiftCardQry(giftCardId),
                                                                                        ResponseTypes.optionalInstanceOf(
                                                                                                GiftCardRecord.class));

        SubscriptionQueryResult<Optional<GiftCardRecord>, GiftCardRecord>queryResult = queryGateway.subscriptionQuery(
                query,
                ResponseTypes.optionalInstanceOf(GiftCardRecord.class),
                ResponseTypes.instanceOf(GiftCardRecord.class));

        return sendAndReturnUpdate(new IssueCmd(giftCardId, worktime), queryResult)
                .map(GiftCardRecord::getId);
    }

    public <U> Mono<U> sendAndReturnUpdate(Object command, SubscriptionQueryResult<?, U> result) {
        /* The trick here is to subscribe to initial results first, even it does not return any result
         Subscribing to initialResult creates a buffer for updates, even that we didn't subscribe for updates yet
         they will wait for us in buffer, after this we can safely send command, and then subscribe to updates */
        return Mono.when(result.initialResult())
                   .then(Mono.fromCompletionStage(() -> commandGateway.send(command)))
                   .thenMany(result.updates())
                   .timeout(Duration.ofSeconds(180))
                   .next()
                   .doFinally(unused -> result.cancel());
        /* dont forget to close subscription query on the end and add a timeout */
    }

    @GetMapping(value = "/send/events")
    public void sendEvents(@RequestParam int count, @RequestParam int delay) throws InterruptedException {
        final String giftCardId = UUID.randomUUID().toString();
        commandGateway.sendAndWait(new IssueCmd(giftCardId, 1));

        for (int i = 0; i < count; i++) {
            log.info("Sending {}/{} events delayed by {}", i + 1, count, delay);
            eventBus.publish(GenericEventMessage.asEventMessage(new IssuedEvt(giftCardId, i)));
            Thread.sleep(delay);
        }
    }

    @GetMapping(value = "/send/commands/new-aggregate")
    public void sendCommands(@RequestParam int count, @RequestParam int delay, @RequestParam int worktime)
            throws InterruptedException {
        for (int i = 0; i < count; i++) {
            log.info("Sending {}/{} commands delayed by {}, with work time: {}", i + 1, count, delay, worktime);

            commandGateway.send(new IssueCmd(UUID.randomUUID().toString(), worktime));
            Thread.sleep(delay);
        }
    }

    @GetMapping(value = "/send/commands/aggregate")
    public void sendAggregateCommands(@RequestParam int count) {
        log.info("Starting!");

        final String giftCardId = UUID.randomUUID().toString();

        commandGateway.sendAndWait(new IssueCmd(giftCardId, 1));


        for (int i = 0; i < count; i++) {
            commandGateway.sendAndWait(new AddCmd(giftCardId, 1));

            log.info("Done!");
        }
    }
}




















