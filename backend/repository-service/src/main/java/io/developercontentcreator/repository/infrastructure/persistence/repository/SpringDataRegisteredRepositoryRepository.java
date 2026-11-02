package io.developercontentcreator.repository.infrastructure.persistence.repository;

import io.developercontentcreator.repository.infrastructure.persistence.entity.RegisteredRepositoryEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRegisteredRepositoryRepository
    extends JpaRepository<RegisteredRepositoryEntity, UUID> {

  boolean existsByInstallationIdAndGithubRepositoryId(Long installationId, Long githubRepositoryId);
}
