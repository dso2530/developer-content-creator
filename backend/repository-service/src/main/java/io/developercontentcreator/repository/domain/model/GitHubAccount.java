package io.developercontentcreator.repository.domain.model;

public record GitHubAccount(Long githubId, String login, GitHubAccountType type) {

  public GitHubAccount {
    if (githubId == null || githubId <= 0) {
      throw new IllegalArgumentException("GitHub account ID must be positive");
    }

    if (login == null || login.isBlank()) {
      throw new IllegalArgumentException("GitHub account login must not be blank");
    }

    if (type == null) {
      throw new IllegalArgumentException("GitHub account type must not be null");
    }
  }
}
