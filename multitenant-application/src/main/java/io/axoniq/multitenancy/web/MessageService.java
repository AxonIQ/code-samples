package io.axoniq.multitenancy.web;

import io.axoniq.multitenancy.api.AddCmd;
import io.axoniq.multitenancy.api.FindAllGiftCardQry;
import io.axoniq.multitenancy.api.FindGiftCardQry;
import io.axoniq.multitenancy.api.GiftCardRecord;
import io.axoniq.multitenancy.api.IssueCmd;
import io.axoniq.multitenancy.api.IssuedEvt;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.extensions.multitenancy.components.TenantProvider;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.GenericQueryMessage;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.axonframework.extensions.multitenancy.autoconfig.TenantConfiguration.TENANT_CORRELATION_KEY;


@Service
public class MessageService {


    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final EventBus eventBus;

    public MessageService(CommandGateway commandGateway, QueryGateway queryGateway, EventBus eventBus) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
        this.eventBus = eventBus;
    }


    public void sendQueries(String tenantName) {
        for (int i = 0; i < 50; i++) {
            log.info("Sending {}/{} queries delayed by {}", i + 1, 50, 50);

            QueryMessage<FindAllGiftCardQry, List<GiftCardRecord>> query = new GenericQueryMessage<>(new FindAllGiftCardQry(),
                                                                                                       ResponseTypes.multipleInstancesOf(
                                                                                                               GiftCardRecord.class))
                    .withMetaData(Collections.singletonMap(TENANT_CORRELATION_KEY, tenantName));

            queryGateway.query(query, ResponseTypes.multipleInstancesOf(GiftCardRecord.class));
        }
    }


    public Mono<String> subscriptionQuery(String tenantName) {

        final String giftCardId = UUID.randomUUID().toString();

        QueryMessage<FindGiftCardQry, Optional<GiftCardRecord>> query = new GenericQueryMessage<>(new FindGiftCardQry(giftCardId),
                                                                                        ResponseTypes.optionalInstanceOf(
                                                                                                GiftCardRecord.class))
                .withMetaData(Collections.singletonMap(TENANT_CORRELATION_KEY, tenantName));

        SubscriptionQueryResult<Optional<GiftCardRecord>, GiftCardRecord>queryResult = queryGateway.subscriptionQuery(
                query,
                ResponseTypes.optionalInstanceOf(GiftCardRecord.class),
                ResponseTypes.instanceOf(GiftCardRecord.class));

        return sendAndReturnUpdate(new IssueCmd(giftCardId, 50), queryResult)
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


    public void sendEvents(String tenantName) {
        final String giftCardId = UUID.randomUUID().toString();

        GenericCommandMessage<IssueCmd> issueCmd = new GenericCommandMessage<>(new IssueCmd(giftCardId, 1))
                .withMetaData(Collections.singletonMap(TENANT_CORRELATION_KEY, tenantName));
        commandGateway.sendAndWait(issueCmd);

        for (int i = 0; i < 50; i++) {
            log.info("Sending {}/{} events delayed by {}", i + 1, 50, 50);
            EventMessage<Object> eventMessage = GenericEventMessage.asEventMessage(new IssuedEvt(giftCardId, i))
                    .withMetaData(Collections.singletonMap(TENANT_CORRELATION_KEY, tenantName));
            eventBus.publish(eventMessage);
        }
    }

    public void sendCommands(String tenantName) {
        log.info("Starting!");

        final String giftCardId = UUID.randomUUID().toString();
        GenericCommandMessage<IssueCmd> issueCmd = new GenericCommandMessage<>(new IssueCmd(giftCardId, 1))
                .withMetaData(Collections.singletonMap(TENANT_CORRELATION_KEY, tenantName));

        commandGateway.sendAndWait(issueCmd);


        for (int i = 0; i < 50; i++) {
            GenericCommandMessage<AddCmd> addCmd = new GenericCommandMessage<>(new AddCmd(giftCardId, 1))
                    .withMetaData(Collections.singletonMap(TENANT_CORRELATION_KEY, tenantName));
            commandGateway.sendAndWait(addCmd);

            log.info("Done!");
        }
    }
}




















