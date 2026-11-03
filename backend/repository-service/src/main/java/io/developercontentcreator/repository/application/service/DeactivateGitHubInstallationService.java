package io.developercontentcreator.repository.application.service;

import io.developercontentcreator.repository.application.port.in.DeactivateGitHubInstallationUseCase;
import io.developercontentcreator.repository.application.port.out.DeactivateGitHubInstallationPort;

public class DeactivateGitHubInstallationService implements DeactivateGitHubInstallationUseCase {

  private final DeactivateGitHubInstallationPort deactivatePort;

  public DeactivateGitHubInstallationService(DeactivateGitHubInstallationPort deactivatePort) {
    this.deactivatePort = deactivatePort;
  }

  @Override
  public void deactivate(Long installationId) {
    if (installationId == null || installationId <= 0) {
      throw new IllegalArgumentException("GitHub installation ID must be positive");
    }

    deactivatePort.deactivate(installationId);
  }
}
