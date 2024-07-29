package io.axoniq.multitenancy.domain;

import io.axoniq.multitenancy.api.AddFundsCommand;
import io.axoniq.multitenancy.api.FundsAddedEvent;
import io.axoniq.multitenancy.api.IssueCardCommand;
import io.axoniq.multitenancy.api.CardIssuedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@ProcessingGroup("giftcard")
@Aggregate
class GiftCard {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String id;
    private int remainingValue;

    public GiftCard() {
        logger.debug("Empty constructor invoked");
    }

    @CommandHandler
    GiftCard(IssueCardCommand command) throws InterruptedException {
        logger.debug("handling {}", command);
        Integer amount = command.amount();
        if (amount <= 0) {
            throw new IllegalArgumentException("amount <= 0");
        }

        //simulate some work time
        Thread.sleep(amount);
        apply(new CardIssuedEvent(command.id(), amount));
    }

    @CommandHandler
    void add(AddFundsCommand command) {
        apply(new FundsAddedEvent(command.id(), command.amount()));
    }

    @EventSourcingHandler
    void on(FundsAddedEvent event) {
        remainingValue += event.amount();
        logger.info("remaining amount is {} for account {}", remainingValue, id);
    }

    @EventSourcingHandler
    void on(CardIssuedEvent event) {
        logger.debug("applying {}", event);
        id = event.id();
        remainingValue = event.amount();
        logger.debug("new remaining value: {}", remainingValue);
    }
}
