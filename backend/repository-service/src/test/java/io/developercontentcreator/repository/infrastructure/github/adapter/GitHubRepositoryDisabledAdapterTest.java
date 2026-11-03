package io.developercontentcreator.repository.infrastructure.github.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.developercontentcreator.repository.infrastructure.github.exception.GitHubIntegrationDisabledException;
import org.junit.jupiter.api.Test;

class GitHubRepositoryDisabledAdapterTest {

  private final GitHubRepositoryDisabledAdapter adapter = new GitHubRepositoryDisabledAdapter();

  @Test
  void shouldThrowExceptionWhenGitHubIntegrationIsDisabled() {
    assertThatThrownBy(() -> adapter.findByInstallationIdAndRepositoryId(123L, 456L))
        .isInstanceOf(GitHubIntegrationDisabledException.class)
        .hasMessageContaining("developer-content-creator.github.app.enabled");
  }
}
