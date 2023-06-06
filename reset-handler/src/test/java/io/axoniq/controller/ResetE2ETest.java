package io.axoniq.controller;

import io.axoniq.MyFakeProjection;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResetE2ETest {

    private static final int HTTP_PORT = 8024;
    private static final int GRPC_PORT = 8124;

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private MyFakeProjection projection;

    private static final String EVENT_PROCESSOR_NAME="io.axoniq";

    @ClassRule
    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
                    .withExposedService("axonserver", HTTP_PORT, Wait.forListeningPort())
                    .withExposedService("axonserver", GRPC_PORT, Wait.forListeningPort())
                    .waitingFor("axonserver", Wait.forLogMessage(".*Started AxonServer in .*",1));
    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {

        int grpcPort = environment.getServicePort("axonserver", GRPC_PORT);
        int httpPort = environment.getServicePort("axonserver", HTTP_PORT);

        registry.add("axon.axonserver.servers", () -> "localhost:"+grpcPort);
        registry.add("axon.axonserver.http-url", () -> "http://localhost:"+httpPort);

    }
    @Test
    void testEventsAreCreated(){
        prepareIntegrationTest();
        Mockito.clearInvocations(projection);

        this.webClient.get().uri("/event/")
                .exchange()
                .expectStatus()
                .isOk();
        waitForAS();
        verify(projection, times(1)).on(any());
    }


    @Test
    void testResetWorks(){
        prepareIntegrationTest();

        createEvents();

        verifyResetEventProcessorByMethod("framework");
        verifyResetEventProcessorByMethod("rest");
        verifyResetEventProcessorByMethod("server");

    }


    void createEvents(){
        Mockito.clearInvocations(projection);

        int amountOfCreatedEvetns = 10;
        // create a few events so we have something to reset
        for(int i = 0 ; i < amountOfCreatedEvetns; i++){
            this.webClient.get().uri("/event/")
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
        waitForAS();
        verify(projection, times(10)).on(any());
    }

    void verifyResetEventProcessorByMethod(String method){
        Mockito.clearInvocations(projection);
        String path = String.format("/%s/reset/%s", method, EVENT_PROCESSOR_NAME);

        this.webClient.get().uri(path)
                .exchange()
                .expectStatus()
                .isOk();

        // this might be asynchronous, so we have to wait a bit or hook something
        waitForAS();
        // there might be events of older tests in the context, therefore use at least instead of times
        verify(projection, atLeast(10)).on(any());

    }

    private void prepareIntegrationTest() {

        // TODO hook internal method?
        waitForAS();
    }

    private void waitForAS() {

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
