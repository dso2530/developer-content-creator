package io.developercontentcreator.repository.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GitHubInstallationTest {

  @Test
  void shouldCreateInstallation() {
    GitHubAccount account = new GitHubAccount(123456L, "octocat", GitHubAccountType.USER);

    GitHubInstallation installation = new GitHubInstallation(987654L, account);

    assertThat(installation.installationId()).isEqualTo(987654L);
    assertThat(installation.account()).isEqualTo(account);
  }

  @Test
  void shouldRejectNullInstallationId() {
    GitHubAccount account = new GitHubAccount(123456L, "octocat", GitHubAccountType.USER);

    assertThatThrownBy(() -> new GitHubInstallation(null, account))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNonPositiveInstallationId() {
    GitHubAccount account = new GitHubAccount(123456L, "octocat", GitHubAccountType.USER);

    assertThatThrownBy(() -> new GitHubInstallation(0L, account))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new GitHubInstallation(-1L, account))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullAccount() {
    assertThatThrownBy(() -> new GitHubInstallation(987654L, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
