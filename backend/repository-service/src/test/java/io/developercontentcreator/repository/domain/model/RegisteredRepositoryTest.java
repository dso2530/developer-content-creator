package io.developercontentcreator.repository.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisteredRepositoryTest {

  private static final UUID ID = UUID.randomUUID();
  private static final Long GITHUB_REPOSITORY_ID = 123456L;
  private static final Long INSTALLATION_ID = 987654L;
  private static final String NAME = "developer-content-creator";
  private static final String FULL_NAME = "octocat/developer-content-creator";
  private static final URI HTML_URL =
      URI.create("https://github.com/octocat/developer-content-creator");
  private static final String DEFAULT_BRANCH = "main";

  @Test
  void shouldCreateRegisteredRepository() {
    RegisteredRepository repository = createRepository();

    assertThat(repository.id()).isEqualTo(ID);
    assertThat(repository.githubRepositoryId()).isEqualTo(GITHUB_REPOSITORY_ID);
    assertThat(repository.installationId()).isEqualTo(INSTALLATION_ID);
    assertThat(repository.name()).isEqualTo(NAME);
    assertThat(repository.fullName()).isEqualTo(FULL_NAME);
    assertThat(repository.htmlUrl()).isEqualTo(HTML_URL);
    assertThat(repository.defaultBranch()).contains(DEFAULT_BRANCH);
    assertThat(repository.visibility()).isEqualTo(RepositoryVisibility.PRIVATE);
  }

  @Test
  void shouldAllowRepositoryWithoutDefaultBranch() {
    RegisteredRepository repository =
        new RegisteredRepository(
            ID,
            GITHUB_REPOSITORY_ID,
            INSTALLATION_ID,
            NAME,
            FULL_NAME,
            HTML_URL,
            null,
            RepositoryVisibility.PUBLIC);

    assertThat(repository.defaultBranch()).isEmpty();
  }

  @Test
  void shouldRejectNullInternalId() {
    assertThatThrownBy(
            () ->
                new RegisteredRepository(
                    null,
                    GITHUB_REPOSITORY_ID,
                    INSTALLATION_ID,
                    NAME,
                    FULL_NAME,
                    HTML_URL,
                    DEFAULT_BRANCH,
                    RepositoryVisibility.PRIVATE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNonPositiveGitHubRepositoryId() {
    assertThatThrownBy(
            () ->
                new RegisteredRepository(
                    ID,
                    0L,
                    INSTALLATION_ID,
                    NAME,
                    FULL_NAME,
                    HTML_URL,
                    DEFAULT_BRANCH,
                    RepositoryVisibility.PRIVATE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNonPositiveInstallationId() {
    assertThatThrownBy(
            () ->
                new RegisteredRepository(
                    ID,
                    GITHUB_REPOSITORY_ID,
                    0L,
                    NAME,
                    FULL_NAME,
                    HTML_URL,
                    DEFAULT_BRANCH,
                    RepositoryVisibility.PRIVATE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(
            () ->
                new RegisteredRepository(
                    ID,
                    GITHUB_REPOSITORY_ID,
                    INSTALLATION_ID,
                    " ",
                    FULL_NAME,
                    HTML_URL,
                    DEFAULT_BRANCH,
                    RepositoryVisibility.PRIVATE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankFullName() {
    assertThatThrownBy(
            () ->
                new RegisteredRepository(
                    ID,
                    GITHUB_REPOSITORY_ID,
                    INSTALLATION_ID,
                    NAME,
                    " ",
                    HTML_URL,
                    DEFAULT_BRANCH,
                    RepositoryVisibility.PRIVATE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullHtmlUrl() {
    assertThatThrownBy(
            () ->
                new RegisteredRepository(
                    ID,
                    GITHUB_REPOSITORY_ID,
                    INSTALLATION_ID,
                    NAME,
                    FULL_NAME,
                    null,
                    DEFAULT_BRANCH,
                    RepositoryVisibility.PRIVATE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectRelativeHtmlUrl() {
    assertThatThrownBy(
            () ->
                new RegisteredRepository(
                    ID,
                    GITHUB_REPOSITORY_ID,
                    INSTALLATION_ID,
                    NAME,
                    FULL_NAME,
                    URI.create("/octocat/repository"),
                    DEFAULT_BRANCH,
                    RepositoryVisibility.PRIVATE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullVisibility() {
    assertThatThrownBy(
            () ->
                new RegisteredRepository(
                    ID,
                    GITHUB_REPOSITORY_ID,
                    INSTALLATION_ID,
                    NAME,
                    FULL_NAME,
                    HTML_URL,
                    DEFAULT_BRANCH,
                    null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private RegisteredRepository createRepository() {
    return new RegisteredRepository(
        ID,
        GITHUB_REPOSITORY_ID,
        INSTALLATION_ID,
        NAME,
        FULL_NAME,
        HTML_URL,
        DEFAULT_BRANCH,
        RepositoryVisibility.PRIVATE);
  }
}
