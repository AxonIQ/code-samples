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
 */
@Component
class MyEntityProjection {

    private final QueryUpdateEmitter emitter;

    public MyEntityProjection(QueryUpdateEmitter emitter) {
        this.emitter = emitter;
    }

    @QueryHandler
    //TODO document
    public Optional<Void> on(GetMyEntityByCorrelationIdQuery query) {
        return Optional.empty();
    }

    @EventHandler
    public void on(MyEntityCreatedEvent event, @MetaDataValue("correlationId") String correlationId) {
        MyEntity entity = new MyEntity(event.getEntityId());
        // save your entity in your repository...
        // TODO add documentation
        emitter.emit(GetMyEntityByCorrelationIdQuery.class,
                     query -> query.getCorrelationId().equals(correlationId),
                     entity);
    }
}
