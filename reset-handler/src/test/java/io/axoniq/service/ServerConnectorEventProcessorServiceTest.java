package io.axoniq.service;

import io.axoniq.axonserver.connector.ResultStream;
import io.axoniq.axonserver.connector.admin.AdminChannel;
import io.axoniq.axonserver.grpc.admin.EventProcessor;
import io.axoniq.axonserver.grpc.admin.EventProcessorIdentifier;
import io.axoniq.axonserver.grpc.admin.EventProcessorInstance;
import io.axoniq.axonserver.grpc.admin.Result;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ServerConnectorEventProcessorServiceTest{

    private final String PROCESSOR_NAME = "someProcessorName";
    private final String STORE_ID = "someTokenStoreIdentifier";

    private ServerConnectorEventProcessorService eventProcessorService;

    @Test
    void pauseDoesNotWaitIfSuccess() {
        AdminChannel adminChannel = Mockito.mock(AdminChannel.class);
        when(adminChannel.pauseEventProcessor(PROCESSOR_NAME, STORE_ID))
                .thenReturn(CompletableFuture.completedFuture(Result.SUCCESS));

        eventProcessorService = new ServerConnectorEventProcessorService(null,adminChannel);

        Mono<Void> result = eventProcessorService.pause(PROCESSOR_NAME, STORE_ID);
        StepVerifier.create(result)
                .expectNext()
                .verifyComplete();

        verify(adminChannel, never()).eventProcessors();
    }

    @Test
    void waitDoesNotRetryIfReady() {
        AdminChannel adminChannel = Mockito.mock(AdminChannel.class);
        when(adminChannel.eventProcessors())
                .thenReturn(new DummyResultStream(processors_all_stopped()));
        eventProcessorService = new ServerConnectorEventProcessorService(null,adminChannel);

        Mono<Result> result = eventProcessorService.awaitForStatus(PROCESSOR_NAME,STORE_ID,false);
        StepVerifier.create(result)
                .expectNext(Result.SUCCESS)
                .verifyComplete();

        verify(adminChannel, times(1)).eventProcessors();
    }

    @Test
    void waitDoesRetryUntilReady() {
        AdminChannel adminChannel = Mockito.mock(AdminChannel.class);
        when(adminChannel.eventProcessors())
                .thenReturn(new DummyResultStream(processors_all_running()))
                .thenReturn(new DummyResultStream(processors_some_running()))
                .thenReturn(new DummyResultStream(processors_all_stopped()));

        eventProcessorService = new ServerConnectorEventProcessorService(null,adminChannel);

        Mono<Result> result = eventProcessorService.awaitForStatus(PROCESSOR_NAME,STORE_ID,false);
        StepVerifier.create(result)
                .expectNext(Result.SUCCESS)
                .verifyComplete();

        verify(adminChannel, times(3)).eventProcessors();
    }

    @Test
    void waitDoesErrorAfterRetriesExhausted() {
        AdminChannel adminChannel = Mockito.mock(AdminChannel.class);
        when(adminChannel.eventProcessors())
                .thenReturn(new DummyResultStream(processors_all_running()));

        eventProcessorService = new ServerConnectorEventProcessorService(null,adminChannel);

        Mono<Result> result = eventProcessorService.awaitForStatus(PROCESSOR_NAME,STORE_ID,false);
        StepVerifier.create(result)
                .expectError()
                .verify();

        verify(adminChannel, times(4)).eventProcessors();
    }


    private List<EventProcessor> processors_all_stopped() {
        EventProcessorInstance epi0 = EventProcessorInstance.newBuilder().setIsRunning(false).build();
        EventProcessorInstance epi1 = EventProcessorInstance.newBuilder().setIsRunning(false).build();
        EventProcessor ep0 = getEventProcessorRelevant(epi0, epi1);
        EventProcessor ep1 = getEventProcessorIrrelevant();
        return Arrays.asList(ep0, ep1);
    }



    private List<EventProcessor> processors_some_running() {
        EventProcessorInstance epi0 = EventProcessorInstance.newBuilder().setIsRunning(false).build();
        EventProcessorInstance epi1 = EventProcessorInstance.newBuilder().setIsRunning(true).build();
        EventProcessor ep0 = getEventProcessorRelevant(epi0, epi1);
        EventProcessor ep1 = getEventProcessorIrrelevant();
        return Arrays.asList(ep0, ep1);
    }

    private List<EventProcessor> processors_all_running() {
        EventProcessorInstance epi0 = EventProcessorInstance.newBuilder().setIsRunning(true).build();
        EventProcessorInstance epi1 = EventProcessorInstance.newBuilder().setIsRunning(true).build();
        EventProcessor ep0 = getEventProcessorRelevant(epi0, epi1);
        EventProcessor ep1 = getEventProcessorIrrelevant();
        return Arrays.asList(ep0, ep1);
    }

    @NotNull
    private EventProcessor getEventProcessorRelevant(EventProcessorInstance epi0, EventProcessorInstance epi1) {
        return EventProcessor.newBuilder().setIdentifier(
                EventProcessorIdentifier.newBuilder()
                        .setProcessorName(PROCESSOR_NAME)
                        .setTokenStoreIdentifier(STORE_ID)
                        .build()
        ).addAllClientInstance(Arrays.asList(
                epi0, epi1
        )).build();
    }

    @NotNull
    private EventProcessor getEventProcessorIrrelevant() {
        EventProcessorInstance epi2 = EventProcessorInstance.newBuilder().setIsRunning(true).build();
        EventProcessorInstance epi3 = EventProcessorInstance.newBuilder().setIsRunning(false).build();
        return EventProcessor.newBuilder().setIdentifier(
                EventProcessorIdentifier.newBuilder()
                        .setProcessorName("whatever.dont.care")
                        .setTokenStoreIdentifier("some.other.id")
                        .build()
        ).addAllClientInstance(Arrays.asList(
                epi2, epi3
        )).build();
    }


    static class DummyResultStream implements ResultStream<EventProcessor> {

        private int index = 0;
        private boolean closed=false;

        public DummyResultStream(List<EventProcessor> procs) {
            this.procs = procs;
        }

        private final List<EventProcessor> procs;

        @Override
        public EventProcessor peek() {
            if(!closed && procs.size()>index){
                return procs.get(index);
            }
            else{
                closed=true;
                return null;
            }
        }

        @Override
        public EventProcessor nextIfAvailable() {
            if(!closed && procs.size()>index){
                EventProcessor ep = procs.get(index);
                index+=1;
                return ep;
            }
            else{
                closed=true;
                return null;
            }
        }

        @Override
        public EventProcessor nextIfAvailable(long timeout, TimeUnit unit) {
            return nextIfAvailable();
        }

        @Override
        public EventProcessor next() {
            return nextIfAvailable();
        }

        @Override
        public void onAvailable(Runnable callback) {
            callback.run();
        }

        @Override
        public void close() {
            this.closed=true;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public Optional<Throwable> getError() {
            return Optional.empty();
        }
    }
}
