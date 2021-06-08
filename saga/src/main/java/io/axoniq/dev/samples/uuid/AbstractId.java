package io.axoniq.dev.samples.uuid;

import java.util.UUID;

abstract class AbstractId {

    UUID id;
    public AbstractId() {
        this.id = UUID.randomUUID();
    }
}
