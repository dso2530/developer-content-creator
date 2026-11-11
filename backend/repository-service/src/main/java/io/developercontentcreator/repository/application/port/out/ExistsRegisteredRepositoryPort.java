package io.developercontentcreator.repository.application.port.out;

public interface ExistsRegisteredRepositoryPort {

  boolean existsByInstallationIdAndGitHubRepositoryId(Long installationId, Long githubRepositoryId);
}
