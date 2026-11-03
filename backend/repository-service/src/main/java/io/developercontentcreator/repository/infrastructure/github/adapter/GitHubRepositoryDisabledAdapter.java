package io.developercontentcreator.repository.infrastructure.github.adapter;

import io.developercontentcreator.repository.application.port.out.LoadGitHubRepositoryPort;
import io.developercontentcreator.repository.domain.model.GitHubRepositorySnapshot;
import io.developercontentcreator.repository.infrastructure.github.exception.GitHubIntegrationDisabledException;
import java.util.Optional;

public class GitHubRepositoryDisabledAdapter implements LoadGitHubRepositoryPort {

  @Override
  public Optional<GitHubRepositorySnapshot> findByInstallationIdAndRepositoryId(
      Long installationId, Long githubRepositoryId) {

    throw new GitHubIntegrationDisabledException();
  }
}
