package io.axoniq.dev.samples.query;

import io.axoniq.dev.samples.api.GetMyEntityByIdQuery;
import io.axoniq.dev.samples.api.MyEntityCreatedEvent;
import org.axonframework.eventhandling.EventHandler;
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

    private final MyEntityRepository repository;

    public MyEntityProjection(QueryUpdateEmitter emitter,
                              MyEntityRepository repository) {
        this.emitter = emitter;
        this.repository = repository;
    }

    @QueryHandler
    public Optional<MyEntity> on(GetMyEntityByIdQuery query) {
        return repository.findById(query.getEntityId());
    }

    @EventHandler
    public void on(MyEntityCreatedEvent event) {
        MyEntity entity = new MyEntity(event.getEntityId());
        repository.save(entity);
        emitter.emit(GetMyEntityByIdQuery.class,
                     getMyEntityByIdQuery -> event.getEntityId().equals(getMyEntityByIdQuery.getEntityId()),
                     entity);
    }
}
