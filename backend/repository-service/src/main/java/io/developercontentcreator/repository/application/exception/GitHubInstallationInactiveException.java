package io.developercontentcreator.repository.application.exception;

public class GitHubInstallationInactiveException extends RuntimeException {

  public GitHubInstallationInactiveException(Long installationId) {
    super("GitHub installation " + installationId + " is inactive");
  }
}
