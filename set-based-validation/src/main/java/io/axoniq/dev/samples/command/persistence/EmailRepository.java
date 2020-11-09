package io.axoniq.dev.samples.command.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<EmailJpaEntity, String> {
    Optional<EmailJpaEntity> findEmailJpaEntityByAccountId(UUID accountId);
}