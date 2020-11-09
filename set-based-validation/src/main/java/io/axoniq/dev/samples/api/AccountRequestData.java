package io.axoniq.dev.samples.api;

import java.util.Objects;

public class AccountRequestData {
    private String emailAddress;

    public AccountRequestData(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public AccountRequestData() {
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountRequestData that = (AccountRequestData) o;
        return Objects.equals(emailAddress, that.emailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailAddress);
    }
}
