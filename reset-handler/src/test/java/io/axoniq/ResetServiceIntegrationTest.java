package io.axoniq;

import io.axoniq.MyFakeProjection;
import io.axoniq.service.FrameworkEventProcessorService;
import io.axoniq.service.RestEventProcessorService;
import io.axoniq.service.ServerConnectorEventProcessorService;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
public class ResetServiceIntegrationTest {

    @Autowired
    RestEventProcessorService restEventProcessorService;
    @Autowired
    FrameworkEventProcessorService frameworkEventProcessorService;
    @Autowired
    ServerConnectorEventProcessorService serverConnectorEventProcessorService;
    @Autowired
    EventGateway eventGateway;
    @MockBean
    private MyFakeProjection projection;

    private static final String EVENT_PROCESSOR_NAME="io.axoniq";

    private static final int HTTP_PORT = 8024;
    private static final int GRPC_PORT = 8124;

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
    @BeforeEach
    void prepare(){
        createEvents();
        Mockito.clearInvocations(projection);
    }

    @Test
    void verifyResetEventProcessorByFramework(){
        frameworkEventProcessorService.reset(EVENT_PROCESSOR_NAME).block();
        waitForAS();
        verify(projection, atLeast(10)).on(any());
    }

    @Test
    void verifyResetEventProcessorByRest(){
        restEventProcessorService.reset(EVENT_PROCESSOR_NAME).block();
        waitForAS();
        verify(projection, atLeast(10)).on(any());
    }

    @Test
    void verifyResetEventProcessorByServer(){
        serverConnectorEventProcessorService.reset(EVENT_PROCESSOR_NAME).block();
        waitForAS();
        verify(projection, atLeast(10)).on(any());
    }


    void createEvents(){
        for(int i = 0 ; i < 10; i++){
            eventGateway.publish(new Object());
        }
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
