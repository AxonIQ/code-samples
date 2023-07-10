package io.axoniq.distributedexceptions;

import io.axoniq.distributedexceptions.command.ExceptionWrappingHandlerInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class DistributedExceptionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedExceptionsApplication.class, args);
    }

    @Bean
    @Profile("command")
    public ExceptionWrappingHandlerInterceptor exceptionWrappingHandlerInterceptor() {
        return new ExceptionWrappingHandlerInterceptor();
    }
}
