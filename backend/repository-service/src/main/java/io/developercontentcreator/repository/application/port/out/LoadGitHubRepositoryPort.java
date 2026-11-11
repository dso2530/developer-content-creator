package io.developercontentcreator.repository.application.port.out;

import io.developercontentcreator.repository.domain.model.GitHubRepositorySnapshot;
import java.util.Optional;

public interface LoadGitHubRepositoryPort {

  Optional<GitHubRepositorySnapshot> findByInstallationIdAndRepositoryId(
      Long installationId, Long githubRepositoryId);
}
