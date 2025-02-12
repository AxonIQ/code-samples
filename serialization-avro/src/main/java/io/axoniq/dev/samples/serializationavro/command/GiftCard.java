package io.axoniq.dev.samples.serializationavro.command;

import io.axoniq.dev.samples.serializationavro.api.CardIssuedEvent;
import io.axoniq.dev.samples.serializationavro.api.CardRedeemedEvent;
import io.axoniq.dev.samples.serializationavro.api.IssueCardCommand;
import io.axoniq.dev.samples.serializationavro.api.RedeemCardCommand;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
class GiftCard {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String giftCardId;
    private int remainingValue;

    public GiftCard() {
        // Required by Axon
        logger.debug("Empty constructor invoked");
    }

    @CommandHandler
    public GiftCard(IssueCardCommand command) {
        logger.debug("handling {}", command);
        if (command.getAmount() <= 0) {
            throw new NegativeOrZeroAmount(command.getAmount(), "amount <= 0");
        }
        apply(new CardIssuedEvent(command.getId(), command.getAmount()));
    }

    @CommandHandler
    public void handle(RedeemCardCommand command) {
        logger.debug("handling {}", command);
        if (command.getAmount() <= 0) {
            throw new NegativeOrZeroAmount(command.getAmount(), "amount <= 0");
        }
        if (command.getAmount() > remainingValue) {
            throw new InsufficientFunds("amount > remaining value");
        }
        apply(new CardRedeemedEvent(giftCardId, command.getAmount()));
    }

    @EventSourcingHandler
    public void on(CardIssuedEvent event) {
        logger.debug("applying {}", event);
        giftCardId = event.getId();
        remainingValue = event.getAmount();
        logger.debug("new remaining value: {}", remainingValue);
    }

    @EventSourcingHandler
    public void on(CardRedeemedEvent event) {
        logger.debug("applying {}", event);
        remainingValue -= event.getAmount();
        logger.debug("new remaining value: {}", remainingValue);
    }
}
