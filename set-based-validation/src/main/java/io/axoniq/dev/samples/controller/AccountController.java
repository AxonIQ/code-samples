package io.axoniq.dev.samples.controller;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.axoniq.dev.samples.api.AccountRequestData;
import io.axoniq.dev.samples.api.ChangeEmailAddressCommand;
import io.axoniq.dev.samples.api.CreateAccountCommand;
import io.axoniq.dev.samples.api.RequestEmailChangeCommand;

@RestController
public class AccountController {
    private final CommandGateway commandGateway;

    public AccountController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping(path = "/accounts")
    public CompletableFuture<Void> register(@RequestBody AccountRequestData accountRequestData) {
        UUID accountId = UUID.randomUUID();
        return commandGateway.send(new CreateAccountCommand(accountId, accountRequestData.getEmailAddress()));
    }

    @PutMapping(path = "/accounts/{accountId}/requestUpdateEmailAddress")
    public CompletableFuture<Void> requestUpdateEmailAddress(@RequestBody AccountRequestData accountRequestData,
                                                             @PathVariable("accountId") UUID accountId) {
        return commandGateway.send(new RequestEmailChangeCommand(accountId, accountRequestData.getEmailAddress()));
    }

    @PutMapping(path = "/accounts/{accountId}/updateEmailAddress")
    public CompletableFuture<Void> updateEmailAddress(@RequestBody AccountRequestData accountRequestData,
                                                      @PathVariable("accountId") UUID accountId) {
        return commandGateway.send(new ChangeEmailAddressCommand(accountId, accountRequestData.getEmailAddress()));
    }

}
