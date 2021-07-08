package io.axoniq.dev.samples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Steven van Beelen
 */
@EnableScheduling
@SpringBootApplication
public class SubscriptionQueryUiStreamingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionQueryUiStreamingApplication.class, args);
    }
}
