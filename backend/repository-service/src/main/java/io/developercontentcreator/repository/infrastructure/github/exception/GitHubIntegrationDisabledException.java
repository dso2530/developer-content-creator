package io.developercontentcreator.repository.infrastructure.github.exception;

public class GitHubIntegrationDisabledException extends IllegalStateException {

  public GitHubIntegrationDisabledException() {
    super(
        "GitHub integration is disabled. "
            + "Set developer-content-creator.github.app.enabled=true to enable it.");
  }
}
