package io.axoniq.dev.samples.serializationavro.rest;

import io.axoniq.dev.samples.serializationavro.api.IssueCardCommand;
import io.axoniq.dev.samples.serializationavro.api.RedeemCardCommand;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/giftcard")
public class GiftCardController {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;

    public GiftCardController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<String>> issueNewGiftCard(@RequestBody IssueCardDto request) {
        IssueCardCommand command = new IssueCardCommand(UUID.randomUUID().toString(), request.amount());
        return commandGateway.send(command)
                             .thenApply(it -> ResponseEntity.ok(String.valueOf(it)))
                             .exceptionally(e -> {
                                 logException(e);
                                 return ResponseEntity.badRequest().body(e.getCause().getMessage());
                             });
    }


    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<String>> redeem(@PathVariable String id, @RequestBody RedeemCardDto dto) {
        return commandGateway.send(new RedeemCardCommand(id, dto.amount()))
                             .thenApply(it -> ResponseEntity.ok(""))
                             .exceptionally(e -> {
                                 logException(e);
                                 return ResponseEntity.badRequest().body(e.getCause().getMessage());
                             });
    }

    private void logException(Throwable throwable) {
        logger.info("exception is " + throwable.getClass().getSimpleName()
                            + " and cause is " + throwable.getCause().getClass().getSimpleName());
        logger.error(throwable.getMessage());
    }
}
