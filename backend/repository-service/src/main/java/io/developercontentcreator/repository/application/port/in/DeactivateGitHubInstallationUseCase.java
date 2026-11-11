package io.developercontentcreator.repository.application.port.in;

public interface DeactivateGitHubInstallationUseCase {

  void deactivate(Long installationId);
}
