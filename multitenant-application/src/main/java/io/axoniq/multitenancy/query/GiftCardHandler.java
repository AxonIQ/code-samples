package io.axoniq.multitenancy.query;

import io.axoniq.multitenancy.api.FindAllGiftCardQry;
import io.axoniq.multitenancy.api.FindGiftCardQry;
import io.axoniq.multitenancy.api.GiftCardRecord;
import io.axoniq.multitenancy.api.IssuedEvt;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.MetaData;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Component
class GiftCardHandler {

    private final GiftCardJpaRepository giftCardJpaRepository;

    public GiftCardHandler(GiftCardJpaRepository giftCardJpaRepository) {
        this.giftCardJpaRepository = giftCardJpaRepository;
    }

    @EventHandler
    public void on(IssuedEvt event, QueryUpdateEmitter queryUpdateEmitter) {
        /*
         * Update our read model by inserting the new card.
         */
        giftCardJpaRepository.save(new GiftCardEntity(event.getId(), event.getAmount(), event.getAmount()));

        /* Send it to subscription queries of type FindGiftCardQry, but only if the card id matches. */
        queryUpdateEmitter.emit(FindGiftCardQry.class,
                                findGiftCardQry -> Objects.equals(event.getId(), findGiftCardQry.getId()),
                                new GiftCardRecord(event.getId(), event.getAmount(), event.getAmount(), "payload")
        );

    }

    //-------------------------------------------------------------


    @QueryHandler
    public List<GiftCardRecord> handle(FindAllGiftCardQry query, MetaData metaData) {
        System.out.println("@" + metaData+ " - " + "FindGiftCardQry: " + query);

        return Collections.emptyList();
    }

    @QueryHandler
    public Optional<GiftCardRecord> handle(FindGiftCardQry query, MetaData metaData) {
        System.out.println("@" + metaData+ " - " + "FindGiftCardQry: " + query);

        return Optional.empty();
    }

}
