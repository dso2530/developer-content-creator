package io.developercontentcreator.github.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class GitHubInstallationToken {

  private final String token;
  private final Instant expiresAt;
  private final Map<String, String> permissions;
  private final String repositorySelection;

  public GitHubInstallationToken(
      String token,
      Instant expiresAt,
      Map<String, String> permissions,
      String repositorySelection) {

    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("GitHub installation token must not be blank.");
    }

    this.token = token;
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null.");
    this.permissions = permissions == null ? Map.of() : Map.copyOf(permissions);
    this.repositorySelection = repositorySelection;
  }

  public String getToken() {
    return token;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Map<String, String> getPermissions() {
    return permissions;
  }

  public String getRepositorySelection() {
    return repositorySelection;
  }

  public boolean isUsable(Duration refreshMargin) {
    return isUsableAt(Instant.now(), refreshMargin);
  }

  public boolean isUsableAt(Instant now, Duration refreshMargin) {
    Objects.requireNonNull(now, "now must not be null.");
    Objects.requireNonNull(refreshMargin, "refreshMargin must not be null.");

    if (refreshMargin.isNegative()) {
      throw new IllegalArgumentException("Refresh margin must not be negative.");
    }

    return expiresAt.isAfter(now.plus(refreshMargin));
  }

  @Override
  public String toString() {
    return "GitHubInstallationToken["
        + "token=REDACTED"
        + ", expiresAt="
        + expiresAt
        + ", permissions="
        + permissions
        + ", repositorySelection="
        + repositorySelection
        + ']';
  }
}
