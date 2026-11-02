package io.developercontentcreator.repository.infrastructure.persistence.adapter;

import io.developercontentcreator.repository.application.port.out.LoadGitHubInstallationPort;
import io.developercontentcreator.repository.domain.model.GitHubInstallation;
import io.developercontentcreator.repository.infrastructure.persistence.repository.SpringDataGitHubInstallationRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class GitHubInstallationPersistenceAdapter implements LoadGitHubInstallationPort {

  private final SpringDataGitHubInstallationRepository repository;

  public GitHubInstallationPersistenceAdapter(SpringDataGitHubInstallationRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<GitHubInstallation> findByInstallationIdAndUserId(
      Long installationId, UUID userId) {

    return repository
        .findByInstallationIdAndUserId(installationId, userId)
        .map(entity -> entity.toDomain());
  }
}
