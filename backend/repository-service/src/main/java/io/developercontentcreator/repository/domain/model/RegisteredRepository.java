package io.developercontentcreator.repository.domain.model;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public final class RegisteredRepository {

  private final UUID id;
  private final Long githubRepositoryId;
  private final Long installationId;
  private final String name;
  private final String fullName;
  private final URI htmlUrl;
  private final String defaultBranch;
  private final RepositoryVisibility visibility;

  public RegisteredRepository(
      UUID id,
      Long githubRepositoryId,
      Long installationId,
      String name,
      String fullName,
      URI htmlUrl,
      String defaultBranch,
      RepositoryVisibility visibility) {

    if (id == null) {
      throw new IllegalArgumentException("Repository internal ID must not be null");
    }

    if (githubRepositoryId == null || githubRepositoryId <= 0) {
      throw new IllegalArgumentException("GitHub repository ID must be positive");
    }

    if (installationId == null || installationId <= 0) {
      throw new IllegalArgumentException("GitHub installation ID must be positive");
    }

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Repository name must not be blank");
    }

    if (fullName == null || fullName.isBlank()) {
      throw new IllegalArgumentException("Repository full name must not be blank");
    }

    if (htmlUrl == null || !htmlUrl.isAbsolute()) {
      throw new IllegalArgumentException("Repository HTML URL must be absolute");
    }

    if (defaultBranch != null && defaultBranch.isBlank()) {
      throw new IllegalArgumentException("Default branch must not be blank");
    }

    if (visibility == null) {
      throw new IllegalArgumentException("Repository visibility must not be null");
    }

    this.id = id;
    this.githubRepositoryId = githubRepositoryId;
    this.installationId = installationId;
    this.name = name;
    this.fullName = fullName;
    this.htmlUrl = htmlUrl;
    this.defaultBranch = defaultBranch;
    this.visibility = visibility;
  }

  public UUID id() {
    return id;
  }

  public Long githubRepositoryId() {
    return githubRepositoryId;
  }

  public Long installationId() {
    return installationId;
  }

  public String name() {
    return name;
  }

  public String fullName() {
    return fullName;
  }

  public URI htmlUrl() {
    return htmlUrl;
  }

  public Optional<String> defaultBranch() {
    return Optional.ofNullable(defaultBranch);
  }

  public RepositoryVisibility visibility() {
    return visibility;
  }
}
