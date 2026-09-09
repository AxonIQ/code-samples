package io.axoniq.dev.samples.query;

import io.axoniq.dev.samples.api.GetMyEntityByCorrelationIdQuery;
import io.axoniq.dev.samples.api.MyEntityCreatedEvent;
import org.axonframework.messaging.core.annotation.MetadataValue;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.queryhandling.QueryUpdateEmitter;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class MyEntityProjection {

    @QueryHandler
    /* We are creating virtual initial result, doesn't need to return anything, but also do not return null */
    public Optional<Void> on(GetMyEntityByCorrelationIdQuery query) {
        return Optional.empty();
    }

    @EventHandler
    public void on(MyEntityCreatedEvent event, @MetadataValue("correlationId") String correlationId,
                   QueryUpdateEmitter emitter) {
        MyEntity entity = new MyEntity(event.entityId());

        /* save your entity in your repository here */

        /* Inject correlationId from Event Metadata, which is basically command id that produced this event.
         Emit and update to all observers that are interested in this correlationId */
        emitter.emit(GetMyEntityByCorrelationIdQuery.class,
                     query -> query.correlationId().equals(correlationId),
                     entity);
    }
}
