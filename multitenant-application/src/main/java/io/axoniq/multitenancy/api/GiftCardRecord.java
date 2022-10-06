package io.axoniq.multitenancy.api;

public class GiftCardRecord {

    private String id;
    private Integer initialValue;
    private Integer remainingValue;
    private String payload;

    public GiftCardRecord(String id, Integer initialValue, Integer remainingValue, String payload) {
        this.id = id;
        this.initialValue = initialValue;
        this.remainingValue = remainingValue;
        this.payload = payload;
    }

    public GiftCardRecord() {
    }

    public String getId() {
        return id;
    }

    public Integer getInitialValue() {
        return initialValue;
    }

    public Integer getRemainingValue() {
        return remainingValue;
    }

    public String getPayload() {
        return payload;
    }
}
