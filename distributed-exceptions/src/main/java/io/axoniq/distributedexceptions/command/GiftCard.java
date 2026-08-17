package io.axoniq.distributedexceptions.command;

import io.axoniq.distributedexceptions.api.IssueCardCommand;
import io.axoniq.distributedexceptions.api.CardIssuedEvent;
import io.axoniq.distributedexceptions.api.RedeemCardCommand;
import io.axoniq.distributedexceptions.api.CardRedeemedEvent;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.lang.invoke.MethodHandles;

@EventSourced(tagKey = "GiftCard")
@Profile("command")
class GiftCard {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String giftCardId;
    private int remainingValue;

    @CommandHandler
    public static void handle(IssueCardCommand command, EventAppender eventAppender) {
        logger.debug("handling {}", command);
        if (command.amount() <= 0) {
            throw new NegativeOrZeroAmount(command.amount(), "amount <= 0");
        }
        eventAppender.append(new CardIssuedEvent(command.id(), command.amount()));
    }

    @CommandHandler
    public void handle(RedeemCardCommand command, EventAppender eventAppender) {
        logger.debug("handling {}", command);
        if (command.amount() <= 0) {
            throw new NegativeOrZeroAmount(command.amount(), "amount <= 0");
        }
        if (command.amount() > remainingValue) {
            throw new InsufficientFunds("amount > remaining value");
        }
        eventAppender.append(new CardRedeemedEvent(giftCardId, command.amount()));
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

    @EntityCreator
    public GiftCard() {
        // Required by Axon
        logger.debug("Empty constructor invoked");
    }
}
