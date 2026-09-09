package io.axoniq.dev.samples.serializationavro.command;

import io.axoniq.dev.samples.serializationavro.api.*;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

@EventSourced
class GiftCard {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String giftCardId;
    private int remainingValue;

    @EntityCreator
    public GiftCard() {
        // Required by Axon
        logger.debug("Empty constructor invoked");
    }

    @CommandHandler
    public static void handle(IssueCardCommand command, EventAppender eventAppender) {
        logger.debug("handling {}", command);
        if (command.getAmount() <= 0) {
            throw new NegativeOrZeroAmount(command.getAmount(), "amount <= 0");
        }
        eventAppender.append(new CardIssuedEvent(command.getId(), command.getAmount()));
    }

    @CommandHandler
    public void handle(RedeemCardCommand command, EventAppender eventAppender) {
        logger.debug("handling {}", command);
        if (command.getAmount() <= 0) {
            throw new NegativeOrZeroAmount(command.getAmount(), "amount <= 0");
        }
        if (command.getAmount() > remainingValue) {
            throw new InsufficientFunds("amount > remaining value");
        }
        eventAppender.append(new CardRedeemedEvent(giftCardId, command.getAmount()));
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
