package io.developercontentcreator.repository.application.service;

import io.developercontentcreator.repository.application.exception.GitHubInstallationNotFoundException;
import io.developercontentcreator.repository.application.exception.GitHubRepositoryNotAccessibleException;
import io.developercontentcreator.repository.application.exception.RepositoryAlreadyRegisteredException;
import io.developercontentcreator.repository.application.port.in.RegisterRepositoryUseCase;
import io.developercontentcreator.repository.application.port.out.ExistsRegisteredRepositoryPort;
import io.developercontentcreator.repository.application.port.out.LoadGitHubInstallationPort;
import io.developercontentcreator.repository.application.port.out.LoadGitHubRepositoryPort;
import io.developercontentcreator.repository.application.port.out.SaveRegisteredRepositoryPort;
import io.developercontentcreator.repository.domain.model.GitHubInstallation;
import io.developercontentcreator.repository.domain.model.GitHubRepositorySnapshot;
import io.developercontentcreator.repository.domain.model.RegisteredRepository;
import java.util.UUID;

public class RegisterRepositoryService implements RegisterRepositoryUseCase {

  private final LoadGitHubInstallationPort installationPort;
  private final LoadGitHubRepositoryPort repositoryPort;
  private final ExistsRegisteredRepositoryPort existsPort;
  private final SaveRegisteredRepositoryPort savePort;

  public RegisterRepositoryService(
      LoadGitHubInstallationPort installationPort,
      LoadGitHubRepositoryPort repositoryPort,
      ExistsRegisteredRepositoryPort existsPort,
      SaveRegisteredRepositoryPort savePort) {

    this.installationPort = installationPort;
    this.repositoryPort = repositoryPort;
    this.existsPort = existsPort;
    this.savePort = savePort;
  }

  @Override
  public RegisteredRepository register(RegisterRepositoryCommand command) {

    GitHubInstallation installation =
        installationPort
            .findByInstallationIdAndUserId(command.installationId(), command.requestedBy())
            .orElseThrow(() -> new GitHubInstallationNotFoundException(command.installationId()));

    GitHubRepositorySnapshot repository =
        repositoryPort
            .findByInstallationIdAndRepositoryId(
                installation.installationId(), command.githubRepositoryId())
            .orElseThrow(
                () ->
                    new GitHubRepositoryNotAccessibleException(
                        command.installationId(), command.githubRepositoryId()));

    if (existsPort.existsByInstallationIdAndGitHubRepositoryId(
        installation.installationId(), repository.githubRepositoryId())) {
      throw new RepositoryAlreadyRegisteredException(
          installation.installationId(), repository.githubRepositoryId());
    }

    RegisteredRepository registeredRepository =
        new RegisteredRepository(
            UUID.randomUUID(),
            repository.githubRepositoryId(),
            installation.installationId(),
            repository.name(),
            repository.fullName(),
            repository.htmlUrl(),
            repository.defaultBranch(),
            repository.visibility());

    return savePort.save(registeredRepository);
  }
}
