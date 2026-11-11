package io.developercontentcreator.repository.application.port.out;

public interface DeactivateGitHubInstallationPort {

  void deactivate(Long installationId);
}
