package io.axoniq.multitenancy.api;

import java.io.Serializable;

public class FindGiftCardQry implements Serializable {
    private String id;

    public FindGiftCardQry(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}

