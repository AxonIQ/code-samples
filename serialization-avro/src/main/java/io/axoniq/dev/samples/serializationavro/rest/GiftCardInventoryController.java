package io.axoniq.dev.samples.serializationavro.rest;

import io.axoniq.dev.samples.serializationavro.api.Card;
import io.axoniq.dev.samples.serializationavro.api.CardList;
import io.axoniq.dev.samples.serializationavro.api.GetAllCardsQuery;
import io.axoniq.dev.samples.serializationavro.api.GetCardByIdQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/inventory")
public class GiftCardInventoryController {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final QueryGateway queryGateway;

    public GiftCardInventoryController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<List<CardDto>>> getAllGiftCards() {
        return queryGateway.query(new GetAllCardsQuery(), ResponseTypes.instanceOf(CardList.class))
                             .thenApply(it -> ResponseEntity.ok(
                                     it
                                     .getElements()
                                     .stream()
                                     .map( c -> new CardDto(c.getId(), c.getAmount())).toList()
                                )
                             )
                             .exceptionally(e -> {
                                 logException(e);
                                 return ResponseEntity.badRequest().build();
                             });
    }


    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<CardDto>> getById(@PathVariable String id) {
        return queryGateway.query(new GetCardByIdQuery(id), ResponseTypes.instanceOf(Card.class))
                             .thenApply(it -> ResponseEntity.ok(new CardDto(it.getId(), it.getAmount())))
                             .exceptionally(e -> {
                                 logException(e);
                                 return ResponseEntity.badRequest().build();
                             });
    }

    private void logException(Throwable throwable) {
        logger.info("exception is " + throwable.getClass().getSimpleName()
                            + " and cause is " + throwable.getCause().getClass().getSimpleName());
        logger.error(throwable.getMessage());
    }
}
