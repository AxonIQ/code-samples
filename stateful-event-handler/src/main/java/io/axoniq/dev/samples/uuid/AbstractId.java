package io.axoniq.dev.samples.uuid;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Embeddable;

@Embeddable
abstract class AbstractId implements Serializable {

    UUID id;

    public AbstractId() {
        this.id = UUID.randomUUID();
    }
}
