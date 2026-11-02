package io.developercontentcreator.repository.domain.model;

import java.net.URI;

public record GitHubRepositorySnapshot(
    Long githubRepositoryId,
    String name,
    String fullName,
    URI htmlUrl,
    String defaultBranch,
    RepositoryVisibility visibility) {

  public GitHubRepositorySnapshot {
    if (githubRepositoryId == null || githubRepositoryId <= 0) {
      throw new IllegalArgumentException("GitHub repository ID must be positive");
    }

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Repository name must not be blank");
    }

    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("Repository full name must not be blank");
    }

    if (htmlUrl == null || !htmlUrl.isAbsolute()) {
      throw new IllegalArgumentException("Repository URL must be absolute");
    }

    if (defaultBranch != null && defaultBranch.isBlank()) {
      throw new IllegalArgumentException("Default branch must not be blank");
    }

    if (visibility == null) {
      throw new IllegalArgumentException("Repository visibility must not be null");
    }
  }
}
