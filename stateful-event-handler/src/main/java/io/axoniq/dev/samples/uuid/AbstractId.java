package io.axoniq.dev.samples.uuid;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
abstract class AbstractId implements Serializable {

    UUID id;
    public AbstractId() {
        this.id = UUID.randomUUID();
    }
}
