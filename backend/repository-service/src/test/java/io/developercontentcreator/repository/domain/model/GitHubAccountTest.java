package io.developercontentcreator.repository.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GitHubAccountTest {

  @Test
  void shouldCreateUserAccount() {
    GitHubAccount account = new GitHubAccount(123456L, "octocat", GitHubAccountType.USER);

    assertThat(account.githubId()).isEqualTo(123456L);
    assertThat(account.login()).isEqualTo("octocat");
    assertThat(account.type()).isEqualTo(GitHubAccountType.USER);
  }

  @Test
  void shouldCreateOrganizationAccount() {
    GitHubAccount account = new GitHubAccount(987654L, "github", GitHubAccountType.ORGANIZATION);

    assertThat(account.githubId()).isEqualTo(987654L);
    assertThat(account.login()).isEqualTo("github");
    assertThat(account.type()).isEqualTo(GitHubAccountType.ORGANIZATION);
  }

  @Test
  void shouldRejectNullGitHubId() {
    assertThatThrownBy(() -> new GitHubAccount(null, "octocat", GitHubAccountType.USER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNonPositiveGitHubId() {
    assertThatThrownBy(() -> new GitHubAccount(0L, "octocat", GitHubAccountType.USER))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new GitHubAccount(-1L, "octocat", GitHubAccountType.USER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullLogin() {
    assertThatThrownBy(() -> new GitHubAccount(123456L, null, GitHubAccountType.USER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankLogin() {
    assertThatThrownBy(() -> new GitHubAccount(123456L, "  ", GitHubAccountType.USER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullAccountType() {
    assertThatThrownBy(() -> new GitHubAccount(123456L, "octocat", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
