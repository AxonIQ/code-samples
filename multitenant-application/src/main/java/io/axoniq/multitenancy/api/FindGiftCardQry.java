package io.axoniq.multitenancy.api;

import java.io.Serializable;

public class FindGiftCardQry implements Serializable {
    private String id;

    public FindGiftCardQry() {

    }
    public FindGiftCardQry(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

