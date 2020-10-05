package io.axoniq.dev.samples.query;

import io.axoniq.dev.samples.api.GetMyEntityByCorrelationIdQuery;
import io.axoniq.dev.samples.api.MyEntityCreatedEvent;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Sara Pellegrini
 * @author Stefan Dragisic
 */
@Component
class MyEntityProjection {

    private final QueryUpdateEmitter emitter;

    public MyEntityProjection(QueryUpdateEmitter emitter) {
        this.emitter = emitter;
    }

    @QueryHandler
    /**We are creating virtual initial result, doesn't need to return anything, but also do not return null*/
    public Optional<Void> on(GetMyEntityByCorrelationIdQuery query) {
        return Optional.empty();
    }

    @EventHandler
    public void on(MyEntityCreatedEvent event, @MetaDataValue("correlationId") String correlationId) {
        MyEntity entity = new MyEntity(event.getEntityId());

        // { save your entity in your repository here }

        /** Inject correlationId from Event Metadata, which is basically command id that produced this event.
         Emit and update to all observers that are interested in this correlationId */
        emitter.emit(GetMyEntityByCorrelationIdQuery.class,
                     query -> query.getCorrelationId().equals(correlationId),
                     entity);
    }
}
