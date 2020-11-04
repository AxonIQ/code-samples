package io.axoniq.dev.samples.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<EmailJpaEntity, String> {
    EmailJpaEntity findEmailJpaEntityByAccountId(UUID accountId);
}