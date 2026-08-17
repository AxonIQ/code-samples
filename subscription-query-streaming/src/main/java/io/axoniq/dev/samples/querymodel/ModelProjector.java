package io.axoniq.dev.samples.querymodel;

import io.axoniq.dev.samples.api.ModelQuery;
import io.axoniq.dev.samples.api.StreamUpdatedEvent;
import org.axonframework.messaging.core.annotation.Namespace;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.queryhandling.QueryUpdateEmitter;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic projector class of a {@link List} of strings model. Adds the contents of the {@link StreamUpdatedEvent} to a
 * list and emits updates of these same events to the {@link ModelQuery}.
 *
 * @author Steven van Beelen
 */
@Component
@Namespace("model-projector")
public class ModelProjector {

    private final List<String> updates;

    public ModelProjector() {
        this.updates = new ArrayList<>();
    }

    @EventHandler
    public void on(StreamUpdatedEvent event, QueryUpdateEmitter updateEmitter) {
        updates.add(event.update());
        updateEmitter.emit(ModelQuery.class, query -> true, event.update());
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public List<String> handle(ModelQuery query) {
        return updates;
    }
}
