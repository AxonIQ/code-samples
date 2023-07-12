package io.axoniq.multitenancy.web;

import io.axoniq.multitenancy.api.AddFundsCommand;
import io.axoniq.multitenancy.api.FindAllCardsQuery;
import io.axoniq.multitenancy.api.FindCardQuery;
import io.axoniq.multitenancy.api.GiftCardRecord;
import io.axoniq.multitenancy.api.IssueCardCommand;
import io.axoniq.multitenancy.api.CardIssuedEvent;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.GenericQueryMessage;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.axonframework.commandhandling.GenericCommandMessage.asCommandMessage;
import static org.axonframework.extensions.multitenancy.autoconfig.TenantConfiguration.TENANT_CORRELATION_KEY;

@Service
public class MessageService {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
            logger.info("Sending {}/{} queries delayed by {}", i + 1, 50, 50);

            QueryMessage<FindAllCardsQuery, List<GiftCardRecord>> query =
                    new GenericQueryMessage<>(
                            new FindAllCardsQuery(),
                            ResponseTypes.multipleInstancesOf(GiftCardRecord.class)
                    ).withMetaData(Collections.singletonMap(TENANT_CORRELATION_KEY, tenantName));

            queryGateway.query(query, ResponseTypes.multipleInstancesOf(GiftCardRecord.class));
        }
    }

    public Mono<String> subscriptionQuery(String tenantName) {
        final String giftCardId = UUID.randomUUID().toString();
        QueryMessage<FindCardQuery, Optional<GiftCardRecord>> query =
                new GenericQueryMessage<>(
                        new FindCardQuery(giftCardId),
                        ResponseTypes.optionalInstanceOf(GiftCardRecord.class)
                ).withMetaData(Collections.singletonMap(TENANT_CORRELATION_KEY, tenantName));

        SubscriptionQueryResult<Optional<GiftCardRecord>, GiftCardRecord> queryResult = queryGateway.subscriptionQuery(
                query,
                ResponseTypes.optionalInstanceOf(GiftCardRecord.class),
                ResponseTypes.instanceOf(GiftCardRecord.class));

        return sendAndReturnUpdate(new IssueCardCommand(giftCardId, 50), queryResult, tenantName)
                .map(GiftCardRecord::id);
    }

    public <U> Mono<U> sendAndReturnUpdate(Object command, SubscriptionQueryResult<?, U> result, String tenantName) {
        MetaData tenantMetaData = MetaData.with(TENANT_CORRELATION_KEY, tenantName);
        CommandMessage<Object> commandMessage = asCommandMessage(command).withMetaData(tenantMetaData);

        return Mono.when(result.initialResult())
                   .then(Mono.fromCompletionStage(() -> commandGateway.send(commandMessage)))
                   .thenMany(result.updates())
                   .timeout(Duration.ofSeconds(180))
                   .next()
                   .doFinally(unused -> result.cancel());
    }


    public void sendEvents(String tenantName) {
        final String giftCardId = UUID.randomUUID().toString();
        MetaData tenantMetaData = MetaData.with(TENANT_CORRELATION_KEY, tenantName);
        CommandMessage<IssueCardCommand> commandMessage =
                GenericCommandMessage.<IssueCardCommand>asCommandMessage(
                        new IssueCardCommand(giftCardId, 1)
                ).withMetaData(tenantMetaData);

        commandGateway.sendAndWait(commandMessage);

        for (int i = 0; i < 50; i++) {
            logger.info("Sending {}/{} events delayed by {}", i + 1, 50, 50);
            EventMessage<Object> eventMessage = GenericEventMessage.asEventMessage(new CardIssuedEvent(giftCardId, i))
                                                                   .withMetaData(tenantMetaData);
            eventBus.publish(eventMessage);
        }
    }

    public void sendCommands(String tenantName) {
        logger.info("Starting!");

        final String giftCardId = UUID.randomUUID().toString();
        MetaData tenantMetaData = MetaData.with(TENANT_CORRELATION_KEY, tenantName);

        CommandMessage<IssueCardCommand> issueCmd =
                GenericCommandMessage.<IssueCardCommand>asCommandMessage(
                        new IssueCardCommand(giftCardId, 1)
                ).withMetaData(tenantMetaData);
        commandGateway.sendAndWait(issueCmd);

        for (int i = 0; i < 50; i++) {
            CommandMessage<AddFundsCommand> addCmd = GenericCommandMessage.<AddFundsCommand>asCommandMessage(new AddFundsCommand(giftCardId, 1))
                                                                          .withMetaData(tenantMetaData);
            commandGateway.sendAndWait(addCmd);
        }

        logger.info("Done!");
    }
}




















