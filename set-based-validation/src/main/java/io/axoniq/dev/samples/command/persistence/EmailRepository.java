package io.axoniq.dev.samples.command.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailRepository extends JpaRepository<EmailJpaEntity, String> {

    Optional<EmailJpaEntity> findEmailJpaEntityByAccountId(UUID accountId);
}