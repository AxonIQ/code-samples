package io.axoniq.dev.samples.querymodel;

import io.axoniq.dev.samples.api.ModelQuery;
import io.axoniq.dev.samples.api.StreamUpdatedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
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
@ProcessingGroup("model-projector")
public class ModelProjector {

    private final QueryUpdateEmitter updateEmitter;
    private final List<String> updates;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ModelProjector(QueryUpdateEmitter updateEmitter) {
        this.updateEmitter = updateEmitter;
        this.updates = new ArrayList<>();
    }

    @EventHandler
    public void on(StreamUpdatedEvent event) {
        updates.add(event.getUpdate());
        updateEmitter.emit(ModelQuery.class, query -> true, event.getUpdate());
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public List<String> handle(ModelQuery query) {
        return updates;
    }
}
