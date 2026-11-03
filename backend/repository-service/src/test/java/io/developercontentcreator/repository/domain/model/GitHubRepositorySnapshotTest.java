package io.developercontentcreator.repository.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class GitHubRepositorySnapshotTest {

  private static final Long REPOSITORY_ID = 123456L;
  private static final URI HTML_URL = URI.create("https://github.com/acme/project");

  @Test
  void shouldCreateSnapshotWithValidValues() {
    GitHubRepositorySnapshot snapshot =
        new GitHubRepositorySnapshot(
            REPOSITORY_ID,
            "project",
            "acme/project",
            HTML_URL,
            "main",
            RepositoryVisibility.PRIVATE);

    assertThat(snapshot.githubRepositoryId()).isEqualTo(REPOSITORY_ID);
    assertThat(snapshot.name()).isEqualTo("project");
    assertThat(snapshot.fullName()).isEqualTo("acme/project");
    assertThat(snapshot.htmlUrl()).isEqualTo(HTML_URL);
    assertThat(snapshot.defaultBranch()).isEqualTo("main");
    assertThat(snapshot.visibility()).isEqualTo(RepositoryVisibility.PRIVATE);
  }

  @Test
  void shouldAllowNullDefaultBranch() {
    GitHubRepositorySnapshot snapshot =
        new GitHubRepositorySnapshot(
            REPOSITORY_ID, "project", "acme/project", HTML_URL, null, RepositoryVisibility.PUBLIC);

    assertThat(snapshot.defaultBranch()).isNull();
  }

  @Test
  void shouldRejectNullRepositoryId() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    null, "project", "acme/project", HTML_URL, "main", RepositoryVisibility.PUBLIC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("GitHub repository ID must be positive");
  }

  @Test
  void shouldRejectNonPositiveRepositoryId() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    0L, "project", "acme/project", HTML_URL, "main", RepositoryVisibility.PUBLIC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("GitHub repository ID must be positive");
  }

  @Test
  void shouldRejectNullName() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    REPOSITORY_ID,
                    null,
                    "acme/project",
                    HTML_URL,
                    "main",
                    RepositoryVisibility.PUBLIC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Repository name must not be blank");
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    REPOSITORY_ID,
                    " ",
                    "acme/project",
                    HTML_URL,
                    "main",
                    RepositoryVisibility.PUBLIC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Repository name must not be blank");
  }

  @Test
  void shouldRejectNullFullName() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    REPOSITORY_ID, "project", null, HTML_URL, "main", RepositoryVisibility.PUBLIC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Repository full name must not be blank");
  }

  @Test
  void shouldRejectBlankFullName() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    REPOSITORY_ID, "project", " ", HTML_URL, "main", RepositoryVisibility.PUBLIC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Repository full name must not be blank");
  }

  @Test
  void shouldRejectNullHtmlUrl() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    REPOSITORY_ID,
                    "project",
                    "acme/project",
                    null,
                    "main",
                    RepositoryVisibility.PUBLIC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Repository URL must be absolute");
  }

  @Test
  void shouldRejectRelativeHtmlUrl() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    REPOSITORY_ID,
                    "project",
                    "acme/project",
                    URI.create("/acme/project"),
                    "main",
                    RepositoryVisibility.PUBLIC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Repository URL must be absolute");
  }

  @Test
  void shouldRejectBlankDefaultBranch() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    REPOSITORY_ID,
                    "project",
                    "acme/project",
                    HTML_URL,
                    " ",
                    RepositoryVisibility.PUBLIC))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default branch must not be blank");
  }

  @Test
  void shouldRejectNullVisibility() {
    assertThatThrownBy(
            () ->
                new GitHubRepositorySnapshot(
                    REPOSITORY_ID, "project", "acme/project", HTML_URL, "main", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Repository visibility must not be null");
  }
}
