package io.developercontentcreator.repository.domain.model;

public final class GitHubInstallation {

  private final Long installationId;
  private final GitHubAccount account;

  public GitHubInstallation(Long installationId, GitHubAccount account) {
    if (installationId == null || installationId <= 0) {
      throw new IllegalArgumentException("GitHub installation ID must be positive");
    }

    if (account == null) {
      throw new IllegalArgumentException("GitHub account must not be null");
    }

    this.installationId = installationId;
    this.account = account;
  }

  public Long installationId() {
    return installationId;
  }

  public GitHubAccount account() {
    return account;
  }
}
