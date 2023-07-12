package io.axoniq.dev.samples.command.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Objects;
import java.util.UUID;

@Entity
public class EmailJpaEntity {

    @Id
    public String emailAddress;

    public UUID accountId;

    public EmailJpaEntity() {
    }

    public EmailJpaEntity(String emailAddress, UUID accountId) {
        this.accountId = accountId;
        this.emailAddress = emailAddress;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmailJpaEntity that = (EmailJpaEntity) o;
        return Objects.equals(emailAddress, that.emailAddress) &&
                Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailAddress, accountId);
    }
}
