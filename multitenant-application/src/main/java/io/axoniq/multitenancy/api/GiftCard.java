package io.axoniq.multitenancy.api;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.messaging.MetaData;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
class GiftCard {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String id;
    private int remainingValue;

    public GiftCard() {
        log.debug("empty constructor invoked");
    }

    @CommandHandler
    GiftCard(IssueCmd cmd) throws InterruptedException {
        log.debug("handling {}", cmd);
        if (cmd.getAmount() <= 0) throw new IllegalArgumentException("amount <= 0");

        //simulate some work time
        Thread.sleep(cmd.getAmount());
        apply(new IssuedEvt(cmd.getId(), cmd.getAmount()));
    }

    @CommandHandler
    void add(AddCmd cmd) {
        apply(new AddedEvt(cmd.getId(), cmd.getAmount()));
    }

    @EventSourcingHandler
    void on(AddedEvt evt) {
        remainingValue += evt.getAmount();
        log.info("remaning amount is {} for account {}",remainingValue,id);
    }

    @EventSourcingHandler
    void on(IssuedEvt evt) {
        log.debug("applying {}", evt);
        id = evt.getId();
        remainingValue = evt.getAmount();
        log.debug("new remaining value: {}", remainingValue);
    }
}
