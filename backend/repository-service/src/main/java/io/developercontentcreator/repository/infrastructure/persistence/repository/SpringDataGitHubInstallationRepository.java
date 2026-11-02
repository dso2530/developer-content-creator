package io.developercontentcreator.repository.infrastructure.persistence.repository;

import io.developercontentcreator.repository.infrastructure.persistence.entity.GitHubInstallationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataGitHubInstallationRepository
    extends JpaRepository<GitHubInstallationEntity, Long> {

  Optional<GitHubInstallationEntity> findByInstallationIdAndUserId(
      Long installationId, UUID userId);
}
