package io.axoniq;

import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ResetHandlerSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResetHandlerSampleApplication.class);
    }

    @Bean
    TokenStore tokenStore() {
        return new InMemoryTokenStore();
    }
}
