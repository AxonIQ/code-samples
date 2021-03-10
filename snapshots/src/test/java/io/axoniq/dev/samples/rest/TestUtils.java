package io.axoniq.dev.samples.rest;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;

public class TestUtils {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String COMMAND_HANDLERS = "http://%s:%s/v1/components/application/commands?context=default";
    private static final RestTemplate restTemplate = new RestTemplate();

    private TestUtils(){
        // utility class
    }

    public static void waitForCommandHandlers(GenericContainer<?> axonServer, int expectedNumberOfCommandHandlers, int time, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long deadline = now + unit.toMillis(time);
        int currentNumberOfCommandHandlers = 0;
        String commandHandlersUrl = buildAxonServerURL(axonServer);

        logger.info("Looking for Command Handler on AS: {}", commandHandlersUrl);
        do {
            try {
                ResponseEntity<CommandHandler[]> response = restTemplate.getForEntity(commandHandlersUrl, CommandHandler[].class);
                currentNumberOfCommandHandlers = Objects.requireNonNull(response.getBody()).length;
                logger.info("Looking for {} Command Handlers, found {}.", expectedNumberOfCommandHandlers, currentNumberOfCommandHandlers);
                Arrays.stream(response.getBody()).forEach(ch -> logger.info(ch.toString()));
            } catch (Exception ex) {
                logger.error("Failed to get the list of Command Handlers from AS: {}", ex.getMessage(), ex);
            }
            if (now >= deadline) {
                throw new IllegalStateException("Expected number of command handlers not received.");
            }
            now = System.currentTimeMillis();
        }
        while (currentNumberOfCommandHandlers < expectedNumberOfCommandHandlers);
    }

    public static void waitForCommandHandlers(GenericContainer<?> axonServer, int expectedNumberOfCommandHandlers) {
        waitForCommandHandlers(axonServer, expectedNumberOfCommandHandlers, 5, TimeUnit.SECONDS);
    }

    private static String buildAxonServerURL(GenericContainer<?> axonServer) {
        String host = axonServer.getContainerIpAddress();
        int axonServerPort = axonServer.getMappedPort(8024);
        return String.format(COMMAND_HANDLERS, host, axonServerPort);
    }

    static class CommandHandler {
        public String name;

        @Override
        public String toString() {
            return "CommandHandler{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
