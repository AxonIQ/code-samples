package io.axoniq.springcloudcommandrouting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts one instance of the application. Start several, each on its own port, to form a cluster.
 */
@SpringBootApplication
public class SpringCloudCommandRoutingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudCommandRoutingApplication.class, args);
    }
}
