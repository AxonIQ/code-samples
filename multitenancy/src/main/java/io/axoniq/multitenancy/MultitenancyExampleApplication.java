package io.axoniq.multitenancy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class MultitenancyExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultitenancyExampleApplication.class, args);
    }
}
