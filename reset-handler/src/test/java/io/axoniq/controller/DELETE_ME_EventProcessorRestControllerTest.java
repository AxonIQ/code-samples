package io.axoniq.controller;

import io.axoniq.service.EventProcessorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class DELETE_ME_EventProcessorRestControllerTest {

    @Mock
    private EventProcessorService eventProcessorService;

    @InjectMocks
    private EventProcessorRestController eventProcessorRestController;

    @Test
    void testControllerCallsServiceAndReturns(){
        when(eventProcessorService.reset("someProcessorName")).thenReturn(Mono.empty());
        Mono<Void> result = eventProcessorRestController.reset(eventProcessorService,"someProcessorName");
        verify(eventProcessorService, times(1)).reset("someProcessorName");
        StepVerifier.create(result).expectNext();

    }
}
