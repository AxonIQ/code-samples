package io.axoniq.dev.samples.rest;

import static io.axoniq.dev.samples.rest.AxonServerContainer.startAxonServer;
import static io.axoniq.dev.samples.rest.TestUtils.waitForCommandHandlers;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;

import io.axoniq.dev.samples.Application;
import reactor.core.publisher.Flux;

/**
 * @author Lucas Campos
 */

class CommandControllerIntegrationTest {

    @ParameterizedTest(name = "Testing against axonserver:{0}.")
    @ValueSource(strings = { "4.3.8", "4.4.5", "latest" })
    void test(String axonServerVersion) {
        // needed infra for tests
        GenericContainer<?> axonServer = startAxonServer(axonServerVersion);
        ConfigurableApplicationContext context = startApplication(axonServer);
        waitForCommandHandlers(axonServer, 2);
        // webclient setup
        String clientPort = context.getEnvironment().getProperty("local.server.port");
        WebClient webClient = WebClient.create("http://localhost:" + clientPort);

        String id = UUID.randomUUID().toString();
        // create the aggregate
        createEntity(webClient, id);
        // fire 5 more events (plus the create above) to it producing 1 snapshot based on our config
        Flux.range(1, 5)
            .doOnNext(i -> renameEntity(webClient, id, String.format("Name-%s", i)))
            .subscribe();

        EventStore eventStore = context.getBean(EventStore.class);
        DomainEventStream events = eventStore.readEvents(id);

        // we should have 1 snapshot (io.axoniq.dev.samples.command.MyEntityAggregate) plus 1 event (io.axoniq.dev.samples.api.MyEntityRenamedEvent)
        AtomicInteger index = new AtomicInteger();
        Map<Integer, String> indexAndEvent = Stream.of(new Object[][]{
                { 1, "io.axoniq.dev.samples.command.MyEntityAggregate" },
                { 2, "io.axoniq.dev.samples.api.MyEntityRenamedEvent" },
        }).collect(Collectors.toMap(data -> (Integer) data[0], data -> (String) data[1]));

        events.forEachRemaining(eventMessage -> {
            assertEquals(id, eventMessage.getAggregateIdentifier());
            assertEquals(indexAndEvent.get(index.incrementAndGet()), eventMessage.getPayloadType().getName());
        });
        assertEquals(2, index.get(), "We should have 2 events");

        axonServer.stop();
        context.close();
    }

    private void createEntity(WebClient webClient, String id) {
        webClient.post()
                 .uri(uriBuilder -> uriBuilder
                         .path("/entity/{id}")
                         .queryParam("name", "Initial Name")
                         .build(id))
                 .retrieve()
                 .bodyToMono(Void.class)
                 .block();
    }

    private void renameEntity(WebClient webClient, String id, String name) {
        webClient.patch()
                 .uri(uriBuilder -> uriBuilder
                         .path("/entity/{id}")
                         .queryParam("name", name)
                         .build(id))
                 .retrieve()
                 .bodyToMono(Void.class)
                 .block();
    }

    private static ConfigurableApplicationContext startApplication(GenericContainer<?> axonServer) {
        int axonServerPort = axonServer.getMappedPort(8124);
        String host = axonServer.getContainerIpAddress();
        String axonServerAddress = host + ":" + axonServerPort;
        String[] args = {
                "--axon.axonserver.servers=" + axonServerAddress,
                "--server.port=0"
        };
        return SpringApplication.run(Application.class, args);
    }

}


