package io.axoniq.multitenancy.query;

import io.axoniq.multitenancy.api.CardIssuedEvent;
import io.axoniq.multitenancy.api.FindAllCardsQuery;
import io.axoniq.multitenancy.api.FindCardQuery;
import io.axoniq.multitenancy.api.FundsAddedEvent;
import io.axoniq.multitenancy.api.GiftCardRecord;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.MetaData;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@ProcessingGroup("giftcard")
@Component
class GiftCardHandler {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final GiftCardJpaRepository giftCardJpaRepository;

    public GiftCardHandler(GiftCardJpaRepository giftCardJpaRepository) {
        this.giftCardJpaRepository = giftCardJpaRepository;
    }

    @EventHandler
    public void on(CardIssuedEvent event, QueryUpdateEmitter queryUpdateEmitter) {
        /*
         * Update our read model by inserting the new card.
         */
        giftCardJpaRepository.save(new GiftCardEntity(event.id(), event.amount(), event.amount()));

        /* Send it to subscription queries of type FindGiftCardQry, but only if the card id matches. */
        queryUpdateEmitter.emit(FindCardQuery.class,
                                query -> Objects.equals(event.id(), query.id()),
                                new GiftCardRecord(event.id(), event.amount(), event.amount(), "payload")
        );
    }

    @EventHandler
    public void on(FundsAddedEvent event) {
        //CurrentUnitOfWork.get().rollback(new TransactionException("This is nasty transaction exception!"));
        giftCardJpaRepository.findById(event.id())
                             .ifPresent(entity -> entity.addFunds(event.amount()));
    }

    @QueryHandler
    public List<GiftCardRecord> handle(FindAllCardsQuery query, MetaData metaData) {
        logger.debug("@" + metaData + " - " + "FindAllCardsQuery: " + query);
        return StreamSupport.stream(giftCardJpaRepository.findAll().spliterator(), false)
                            .map(GiftCardHandler::mapToRecord)
                            .toList();
    }

    @QueryHandler
    public Optional<GiftCardRecord> handle(FindCardQuery query, MetaData metaData) {
        logger.debug("@" + metaData + " - " + "FindCardQuery: " + query);
        return giftCardJpaRepository.findById(query.id()).map(GiftCardHandler::mapToRecord);
    }

    private static GiftCardRecord mapToRecord(GiftCardEntity entity) {
        return new GiftCardRecord(entity.getId(),
                                  entity.getInitialValue(),
                                  entity.getRemainingValue(),
                                  "payload");
    }
}
