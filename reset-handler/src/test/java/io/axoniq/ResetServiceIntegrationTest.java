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

    @ClassRule
    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
                    .withExposedService("axonserver", 8024, Wait.forListeningPort())
                    .withExposedService("axonserver", 8124, Wait.forListeningPort())
                    .waitingFor("axonserver", Wait.forLogMessage(".*Started AxonServer in .*",1));


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
