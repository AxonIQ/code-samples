package io.axoniq.controller;

import io.axoniq.config.StringToEventProcessorServiceConverter;
import io.axoniq.service.FrameworkEventProcessorService;
import io.axoniq.service.RestEventProcessorService;
import io.axoniq.service.ServerConnectorEventProcessorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = EventProcessorRestController.class)
@Import(StringToEventProcessorServiceConverter.class)
public class EventProcessorRestControllerTest {


    @Autowired
    private WebTestClient webClient;

    @MockBean
    private FrameworkEventProcessorService frameworkEventProcessorService;
    @MockBean
    private RestEventProcessorService restEventProcessorService;
    @MockBean
    private ServerConnectorEventProcessorService serverConnectorEventProcessorService;

    @InjectMocks
    private EventProcessorRestController eventProcessorRestController;

    @Test
    void testframeworkEventProcessorServiceIsCalled() throws Exception {
        String requestedProcessorName = "someProcessorName";

        when(frameworkEventProcessorService.reset(requestedProcessorName)).thenReturn(Mono.empty());

        this.webClient.get()
                .uri("/framework/reset/"+requestedProcessorName)
                .exchange()
                .expectStatus()
                .isOk();

        verify(frameworkEventProcessorService, times(1)).reset(requestedProcessorName);
        verify(restEventProcessorService, times(0)).reset(requestedProcessorName);
        verify(serverConnectorEventProcessorService, times(0)).reset(requestedProcessorName);

    }
    @Test
    void testRestEventProcessorServiceIsCalled() throws Exception {
        String requestedProcessorName = "someProcessorName";

        when(restEventProcessorService.reset(requestedProcessorName)).thenReturn(Mono.empty());

        this.webClient.get()
                .uri("/rest/reset/"+requestedProcessorName)
                .exchange()
                .expectStatus()
                .isOk();

        verify(frameworkEventProcessorService, times(0)).reset(requestedProcessorName);
        verify(restEventProcessorService, times(1)).reset(requestedProcessorName);
        verify(serverConnectorEventProcessorService, times(0)).reset(requestedProcessorName);
    }
    @Test
    void testServerConnectorEventProcessorServiceIsCalled() throws Exception {

        String requestedProcessorName = "someProcessorName";

        when(serverConnectorEventProcessorService.reset(requestedProcessorName)).thenReturn(Mono.empty());

        this.webClient.get()
                .uri("/server/reset/"+requestedProcessorName)
                .exchange()
                .expectStatus()
                .isOk();

        verify(frameworkEventProcessorService, times(0)).reset(requestedProcessorName);
        verify(restEventProcessorService, times(0)).reset(requestedProcessorName);
        verify(serverConnectorEventProcessorService, times(1)).reset(requestedProcessorName);
    }
}
