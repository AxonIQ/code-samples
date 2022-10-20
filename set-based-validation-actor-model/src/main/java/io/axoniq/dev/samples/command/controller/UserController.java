package io.axoniq.dev.samples.command.controller;

import io.axoniq.dev.samples.api.commands.RegisterUserWithUniqueEmailAddress;
import io.axoniq.dev.samples.api.commands.RemoveUserWithUniqueEmailAddress;
import io.axoniq.dev.samples.api.dto.RegisterUserDto;
import io.axoniq.dev.samples.api.dto.UpdateEmailFromUserDto;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;


    public UserController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping(path = "/users")
    public CompletableFuture<Void> register(@RequestBody RegisterUserDto registerUserDto) {
        UUID generatedUserId = UUID.randomUUID();
        logger.info("UUID [{}]", generatedUserId);
        return commandGateway.send(new RegisterUserWithUniqueEmailAddress(registerUserDto.getEmailAddress(),
                                                                          generatedUserId));
    }

    @PutMapping(path = "/users")
    public CompletableFuture<Void> changeEmailAddress(@RequestBody UpdateEmailFromUserDto updateEmailFromUserDto) {
        UUID generatedUserId = UUID.randomUUID();
        commandGateway.sendAndWait(new RegisterUserWithUniqueEmailAddress(updateEmailFromUserDto.getEmailAddress(),
                                                                          generatedUserId));
        return commandGateway.send(new RemoveUserWithUniqueEmailAddress(updateEmailFromUserDto.getFormerEmailAddress(),
                                                                        updateEmailFromUserDto.getUserId()));
    }
}
