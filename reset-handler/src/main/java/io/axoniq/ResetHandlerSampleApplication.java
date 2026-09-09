package io.axoniq;

import org.axonframework.messaging.eventhandling.processing.streaming.token.store.TokenStore;
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.inmemory.InMemoryTokenStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ResetHandlerSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResetHandlerSampleApplication.class);
    }

    /**
     * By default, the InMemoryTokenStore is not autowired since it's not 'production ready'. So, we need to explicitly
     * declare it ourselves.
     *
     * @return One InMemoryTokenStore.ó
     */
    @Bean
    TokenStore tokenStore() {
        return new InMemoryTokenStore();
    }
}
