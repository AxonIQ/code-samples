package io.axoniq.dev.samples.upcaster;

import com.google.common.collect.ImmutableList;
import org.axonframework.serialization.upcasting.event.EventUpcasterChain;

public class EventUpcasterChainFactory {

    public EventUpcasterChain buildEventUpcasterChain() {
        return new EventUpcasterChain(ImmutableList.of(new FlightDelayedEventUpcaster()));
    }
}
