package io.axoniq.dev.samples;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sara Pellegrini
 */

public class CommandControllerTest {

    private ConfigurableApplicationContext context;


    @Test
    @ParameterizedTest
    @ValueSource(strings = {"latest", "4.4.1", "4.3.7"})
    void test(String axonServerVersion) {
        GenericContainer<?> axonServer =
                new GenericContainer<>("axoniq/axonserver:" + axonServerVersion)
                        .withExposedPorts(8124, 8024)
                        .waitingFor(Wait.forHttp("/actuator/health").forPort(8024));
        axonServer.start();
        int axonServerPort = axonServer.getMappedPort(8124);
        String host = axonServer.getContainerIpAddress();
        String axonServerAddress = host + ":" + axonServerPort;
        String[] args = {
                "--axon.axonserver.servers=" + axonServerAddress,
                "--server.port=0"
        };
        context = SpringApplication.run(Application.class, args);

        String clientPort = context.getEnvironment().getProperty("local.server.port");
        WebClient webClient = WebClient.create("http://localhost:" + clientPort);

        Boolean result = Flux.range(1, 100)
                             .doOnNext(i -> System.out.println("request # " + i))
                             .flatMap(i -> sendRequest(webClient), 10)
                             .reduce((prev, current) -> prev && current)
                             .block();
        assertNotNull(result);
        assertTrue(result);
        axonServer.stop();
        context.close();
    }

    private Mono<Boolean> sendRequest(WebClient webClient) {
        String id = UUID.randomUUID().toString();
        return webClient.post()
                        .uri("/entities/" + id)
                        .exchange()
                        .flatMap(clientResponse -> clientResponse.toEntity(String.class))
                        .map(HttpEntity::getBody)
                        .map(id::equals);
    }
}
