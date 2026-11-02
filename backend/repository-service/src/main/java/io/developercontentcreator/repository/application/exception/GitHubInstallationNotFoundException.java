package io.developercontentcreator.repository.application.exception;

public class GitHubInstallationNotFoundException extends RuntimeException {

  public GitHubInstallationNotFoundException(Long installationId) {
    super("GitHub installation not found or not authorized: " + installationId);
  }
}
