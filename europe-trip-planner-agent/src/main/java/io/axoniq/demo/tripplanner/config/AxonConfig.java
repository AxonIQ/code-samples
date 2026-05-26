package io.axoniq.demo.tripplanner.config;

import org.axonframework.messaging.eventhandling.processing.streaming.token.store.TokenStore;
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.inmemory.InMemoryTokenStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Axon Framework infrastructure beans.
 * <p>
 * The {@link ChatStateProjection} runs as a pooled-streaming event processor; Axon requires a
 * {@code TokenStore} bean for it. The demo uses {@link InMemoryTokenStore} — fine for the demo
 * (replays from event store on every restart) but use a JPA / JDBC token store in production
 * so the projection only catches up on new events.
 */
@Configuration
public class AxonConfig {

    @Bean
    public TokenStore tokenStore() {
        return new InMemoryTokenStore();
    }
}
