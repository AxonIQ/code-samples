package io.axoniq.dev.samples.config;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.axoniq.dev.samples.interceptor.AccountCreationDispatchInterceptor;

@Configuration
public class AxonConfig {
    @Bean
    public CommandGateway commandGateway(CommandBus commandBus,
                                         AccountCreationDispatchInterceptor accountCreationDispatchInterceptor) {
        return DefaultCommandGateway.builder()
                                    .commandBus(commandBus)
                                    .dispatchInterceptors(accountCreationDispatchInterceptor)
                                    .build();
    }
}
