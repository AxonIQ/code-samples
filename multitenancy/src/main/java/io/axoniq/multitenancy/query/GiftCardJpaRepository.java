package io.axoniq.multitenancy.query;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GiftCardJpaRepository extends CrudRepository<GiftCardEntity, String> {

}