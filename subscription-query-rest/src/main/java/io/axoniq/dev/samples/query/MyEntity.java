package io.axoniq.dev.samples.query;

/**
 * @author Sara Pellegrini
 * @author Stefan Dragisic
 */
public class MyEntity {

    private final String id;

    public MyEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
