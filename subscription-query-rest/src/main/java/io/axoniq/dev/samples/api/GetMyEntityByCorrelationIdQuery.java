package io.axoniq.dev.samples.api;

/**
 * @author Sara Pellegrini
 * @author Stefan Dragisic
 */
public class GetMyEntityByCorrelationIdQuery {

    private final String correlationId;

    public GetMyEntityByCorrelationIdQuery(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
