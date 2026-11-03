package io.developercontentcreator.repository.infrastructure.persistence.repository;

import io.developercontentcreator.repository.domain.model.GitHubInstallationStatus;
import io.developercontentcreator.repository.infrastructure.persistence.entity.GitHubInstallationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataGitHubInstallationRepository
    extends JpaRepository<GitHubInstallationEntity, Long> {

  Optional<GitHubInstallationEntity> findByInstallationIdAndUserId(
      Long installationId, UUID userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
    update GitHubInstallationEntity installation
       set installation.status = :status
     where installation.installationId = :installationId
       and installation.status <> :status
    """)
  int updateStatus(
      @Param("installationId") Long installationId,
      @Param("status") GitHubInstallationStatus status);
}
