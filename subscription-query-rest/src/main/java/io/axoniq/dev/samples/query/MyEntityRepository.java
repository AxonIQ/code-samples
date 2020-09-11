package io.axoniq.dev.samples.query;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Sara Pellegrini
 */
@Component
public class MyEntityRepository {

    private final Map<String, MyEntity> myEntities = new ConcurrentHashMap<>();

    public Optional<MyEntity> findById(String id) {
        return Optional.ofNullable(myEntities.get(id));
    }

    public void save(MyEntity entity) {
        myEntities.put(entity.getId(), entity);
    }
}
