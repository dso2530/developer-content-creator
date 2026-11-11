package io.developercontentcreator.repository.domain.model;

public final class GitHubInstallation {

  private final Long installationId;
  private final GitHubAccount account;
  private final GitHubInstallationStatus status;

  public GitHubInstallation(Long installationId, GitHubAccount account) {
    this(installationId, account, GitHubInstallationStatus.ACTIVE);
  }

  public GitHubInstallation(
      Long installationId, GitHubAccount account, GitHubInstallationStatus status) {

    if (installationId == null || installationId <= 0) {
      throw new IllegalArgumentException("GitHub installation ID must be positive");
    }

    if (account == null) {
      throw new IllegalArgumentException("GitHub account must not be null");
    }

    if (status == null) {
      throw new IllegalArgumentException("GitHub installation status must not be null");
    }

    this.installationId = installationId;
    this.account = account;
    this.status = status;
  }

  public Long installationId() {
    return installationId;
  }

  public GitHubAccount account() {
    return account;
  }

  public GitHubInstallationStatus status() {
    return status;
  }

  public boolean isActive() {
    return status == GitHubInstallationStatus.ACTIVE;
  }
}
