package io.axoniq.distributedexceptions.command;

import io.axoniq.distributedexceptions.api.IssueCardCommand;
import io.axoniq.distributedexceptions.api.CardIssuedEvent;
import io.axoniq.distributedexceptions.api.RedeemCardCommand;
import io.axoniq.distributedexceptions.api.CardRedeemedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.lang.invoke.MethodHandles;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Profile("command")
class GiftCard {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String giftCardId;
    private int remainingValue;

    @CommandHandler
    public GiftCard(IssueCardCommand command) {
        logger.debug("handling {}", command);
        if (command.amount() <= 0) {
            throw new NegativeOrZeroAmount(command.amount(), "amount <= 0");
        }
        apply(new CardIssuedEvent(command.id(), command.amount()));
    }

    @CommandHandler
    public void handle(RedeemCardCommand command) {
        logger.debug("handling {}", command);
        if (command.amount() <= 0) {
            throw new NegativeOrZeroAmount(command.amount(), "amount <= 0");
        }
        if (command.amount() > remainingValue) {
            throw new InsufficientFunds("amount > remaining value");
        }
        apply(new CardRedeemedEvent(giftCardId, command.amount()));
    }

    @EventSourcingHandler
    public void on(CardIssuedEvent event) {
        logger.debug("applying {}", event);
        giftCardId = event.id();
        remainingValue = event.amount();
        logger.debug("new remaining value: {}", remainingValue);
    }

    @EventSourcingHandler
    public void on(CardRedeemedEvent event) {
        logger.debug("applying {}", event);
        remainingValue -= event.amount();
        logger.debug("new remaining value: {}", remainingValue);
    }

    public GiftCard() {
        // Required by Axon
        logger.debug("Empty constructor invoked");
    }
}
