package io.axoniq.dev.samples.serializationavro.query;

import io.axoniq.dev.samples.serializationavro.api.Card;
import io.axoniq.dev.samples.serializationavro.api.CardIssuedEvent;
import io.axoniq.dev.samples.serializationavro.api.CardList;
import io.axoniq.dev.samples.serializationavro.api.CardRedeemedEvent;
import io.axoniq.dev.samples.serializationavro.api.GetAllCardsQuery;
import io.axoniq.dev.samples.serializationavro.api.GetCardByIdQuery;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GiftCardProjection {

    private final ConcurrentHashMap<String, Card> store = new ConcurrentHashMap<>();

    @EventHandler
    public void on(CardIssuedEvent event) {
        store.put(event.getId(), new Card(event.getId(), event.getAmount()));
    }

    @EventHandler
    public void on(CardRedeemedEvent event) {
        store.computeIfPresent(event.getId(), (key, card) -> new Card(key, card.getAmount() - event.getAmount()));
    }

    @QueryHandler
    public CardList getCards(GetAllCardsQuery query) {
        return new CardList(store.values().stream().toList());
    }

    @QueryHandler
    public Card getCardById(GetCardByIdQuery query) {
        return Optional.ofNullable(store.get(query.getId())).orElseThrow(() -> new IllegalArgumentException("No card with id " + query.getId()));
    }

}
