package io.axoniq.dev.samples.api.dto;

import java.util.UUID;

public class UpdateEmailFromUserDto {

    private String emailAddress;
    private String formerEmailAddress;
    private UUID userId;

    public UpdateEmailFromUserDto(String emailAddress, String formerEmailAddress) {
        this.emailAddress = emailAddress;
        this.formerEmailAddress = formerEmailAddress;
    }

    public UpdateEmailFromUserDto() {
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getFormerEmailAddress() {
        return formerEmailAddress;
    }

    public void setFormerEmailAddress(String formerEmailAddress) {
        this.formerEmailAddress = formerEmailAddress;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
