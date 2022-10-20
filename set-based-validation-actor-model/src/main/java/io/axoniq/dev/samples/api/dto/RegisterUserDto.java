package io.axoniq.dev.samples.api.dto;

public class RegisterUserDto {
    private String emailAddress;

    public RegisterUserDto(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public RegisterUserDto() {
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
