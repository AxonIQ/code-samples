package io.axoniq.distributedexceptions.command;

import org.axonframework.commandhandling.CommandBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("command")
@Configuration
public class AxonCommandConfiguration {

    @Autowired
    void commandBus(CommandBus commandBus, ExceptionWrappingHandlerInterceptor exceptionWrappingHandlerInterceptor) {
        //noinspection resource
        commandBus.registerHandlerInterceptor(exceptionWrappingHandlerInterceptor);
    }
}
