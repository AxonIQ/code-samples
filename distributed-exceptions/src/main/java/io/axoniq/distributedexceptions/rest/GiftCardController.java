package io.axoniq.distributedexceptions.rest;

import io.axoniq.distributedexceptions.api.GiftCardBusinessError;
import io.axoniq.distributedexceptions.api.IssueCardCommand;
import io.axoniq.distributedexceptions.api.RedeemCardCommand;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
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

@Profile("rest")
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
                                 String errorResponse = getErrorResponseMessage(e.getCause());
                                 return ResponseEntity.badRequest().body(errorResponse);
                             });
    }


    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<String>> redeem(@PathVariable String id, @RequestBody RedeemCardDto dto) {
        return commandGateway.send(new RedeemCardCommand(id, dto.amount()))
                             .thenApply(it -> ResponseEntity.ok(""))
                             .exceptionally(e -> {
                                 logException(e);
                                 String errorResponse = getErrorResponseMessage(e.getCause());
                                 // This is also the place where this error can be handled, the command retried etc
                                 // or a different logical flow can be attempted
                                 // or at least map to frontend friendly error codes / messages.
                                 return ResponseEntity.badRequest().body(errorResponse);
                             });
    }

    private void logException(Throwable throwable) {
        logger.info("exception is " + throwable.getClass().getSimpleName()
                            + " and cause is " + throwable.getCause().getClass().getSimpleName());
        logger.error(throwable.getMessage());
    }

    private String getErrorResponseMessage(Throwable throwable) {
        if (throwable instanceof CommandExecutionException cee) {
            return cee.getDetails()
                      .map((Object it) -> {
                          GiftCardBusinessError giftCardBusinessError = (GiftCardBusinessError) it;
                          logger.debug("Received BusinessError with data: " + giftCardBusinessError);
                          logger.error("Unable to create GiftCard due to validation constrains. Reason: "
                                               + giftCardBusinessError);
                          return giftCardBusinessError.getCode() + " : " + giftCardBusinessError.getName();
                      })
                      .orElseGet(() -> {
                          logger.error("Unable to create GiftCard due to " + throwable);
                          return cee.getMessage();
                      });
        } else {
            logger.error("Unable to create GiftCard due to unknown generic exception " + throwable);
            return throwable.getMessage();
        }
    }
}
