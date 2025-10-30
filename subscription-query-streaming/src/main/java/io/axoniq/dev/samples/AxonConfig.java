package io.axoniq.dev.samples;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.interceptors.LoggingInterceptor;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Basic Axon configuration adding the {@link LoggingInterceptor} to the {@link CommandGateway}, {@link QueryGateway} and {@link EventGateway}.
 * This provides easy logging of all the messages being published, ensuring that portion of the application works.
 *
 * @author Steven van Beelen
 */
@Configuration
public class AxonConfig {

    @Bean
    public LoggingInterceptor<Message<?>> loggingInterceptor() {
        return new LoggingInterceptor<>();
    }

}
