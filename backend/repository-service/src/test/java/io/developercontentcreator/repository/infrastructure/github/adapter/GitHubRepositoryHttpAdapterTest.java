package io.developercontentcreator.repository.infrastructure.github.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.developercontentcreator.github.client.GitHubInstallationTokenClient;
import io.developercontentcreator.github.config.GitHubAppProperties;
import io.developercontentcreator.github.exception.GitHubApiException;
import io.developercontentcreator.github.model.GitHubInstallationToken;
import io.developercontentcreator.repository.domain.model.RepositoryVisibility;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

class GitHubRepositoryHttpAdapterTest {

  private static final Long INSTALLATION_ID = 123L;
  private static final Long REPOSITORY_ID = 456L;
  private static final String API_URL = "https://api.github.com";
  private static final String TOKEN = "ghs-test-token";

  private GitHubInstallationTokenClient tokenClient;
  private GitHubAppProperties properties;
  private MockRestServiceServer server;
  private GitHubRepositoryHttpAdapter adapter;

  @BeforeEach
  void setUp() {
    tokenClient = org.mockito.Mockito.mock(GitHubInstallationTokenClient.class);
    properties = org.mockito.Mockito.mock(GitHubAppProperties.class);

    when(properties.getApiUrl()).thenReturn(URI.create(API_URL));
    when(tokenClient.createForInstallation(INSTALLATION_ID))
        .thenReturn(
            new GitHubInstallationToken(
                TOKEN, Instant.now().plusSeconds(3600), Map.of("contents", "read"), "all"));

    RestClient.Builder builder = RestClient.builder().baseUrl(API_URL);
    server = MockRestServiceServer.bindTo(builder).build();
    adapter = new GitHubRepositoryHttpAdapter(builder.build(), tokenClient);
  }

  @Test
  void shouldLoadPublicRepository() {
    expectRepositoryRequest()
        .andRespond(
            withSuccess(
                """
                {
                  "id": 456,
                  "name": "developer-content-creator",
                  "full_name": "acme/developer-content-creator",
                  "html_url": "https://github.com/acme/developer-content-creator",
                  "default_branch": "main",
                  "visibility": "public"
                }
                """,
                MediaType.APPLICATION_JSON));

    var result = adapter.findByInstallationIdAndRepositoryId(INSTALLATION_ID, REPOSITORY_ID);

    assertThat(result).isPresent();
    assertThat(result.get().githubRepositoryId()).isEqualTo(REPOSITORY_ID);
    assertThat(result.get().name()).isEqualTo("developer-content-creator");
    assertThat(result.get().fullName()).isEqualTo("acme/developer-content-creator");
    assertThat(result.get().htmlUrl())
        .isEqualTo(URI.create("https://github.com/acme/developer-content-creator"));
    assertThat(result.get().defaultBranch()).isEqualTo("main");
    assertThat(result.get().visibility()).isEqualTo(RepositoryVisibility.PUBLIC);

    server.verify();
  }

  @ParameterizedTest
  @CsvSource({"private, PRIVATE", "internal, PRIVATE"})
  void shouldMapNonPublicVisibility(
      String githubVisibility, RepositoryVisibility expectedVisibility) {

    expectRepositoryRequest()
        .andRespond(
            withSuccess(
                """
                {
                  "id": 456,
                  "name": "private-repository",
                  "full_name": "acme/private-repository",
                  "html_url": "https://github.com/acme/private-repository",
                  "default_branch": "main",
                  "visibility": "%s"
                }
                """
                    .formatted(githubVisibility),
                MediaType.APPLICATION_JSON));

    var result = adapter.findByInstallationIdAndRepositoryId(INSTALLATION_ID, REPOSITORY_ID);

    assertThat(result).isPresent();
    assertThat(result.get().visibility()).isEqualTo(expectedVisibility);

    server.verify();
  }

  @Test
  void shouldReturnEmptyWhenRepositoryIsNotAccessible() {
    expectRepositoryRequest().andRespond(withStatus(HttpStatus.NOT_FOUND));

    var result = adapter.findByInstallationIdAndRepositoryId(INSTALLATION_ID, REPOSITORY_ID);

    assertThat(result).isEmpty();

    server.verify();
  }

  @ParameterizedTest
  @CsvSource({"401", "403", "429", "500"})
  void shouldPropagateGitHubHttpErrors(int statusCode) {
    expectRepositoryRequest().andRespond(withStatus(HttpStatus.valueOf(statusCode)));

    assertThatThrownBy(
            () -> adapter.findByInstallationIdAndRepositoryId(INSTALLATION_ID, REPOSITORY_ID))
        .isInstanceOf(GitHubApiException.class)
        .satisfies(
            exception ->
                assertThat(((GitHubApiException) exception).getStatusCode()).isEqualTo(statusCode));

    server.verify();
  }

  @Test
  void shouldConvertNetworkFailureIntoGitHubApiException() throws IOException {
    expectRepositoryRequest().andRespond(withException(new IOException("Connection refused")));

    assertThatThrownBy(
            () -> adapter.findByInstallationIdAndRepositoryId(INSTALLATION_ID, REPOSITORY_ID))
        .isInstanceOf(GitHubApiException.class)
        .satisfies(
            exception -> assertThat(((GitHubApiException) exception).getStatusCode()).isZero());

    server.verify();
  }

  @Test
  void shouldRejectEmptyResponse() {
    expectRepositoryRequest().andRespond(withSuccess("", MediaType.APPLICATION_JSON));

    assertThatThrownBy(
            () -> adapter.findByInstallationIdAndRepositoryId(INSTALLATION_ID, REPOSITORY_ID))
        .isInstanceOf(GitHubApiException.class)
        .hasMessageContaining("empty repository response");

    server.verify();
  }

  @Test
  void shouldRejectUnsupportedVisibility() {
    expectRepositoryRequest()
        .andRespond(
            withSuccess(
                """
                {
                  "id": 456,
                  "name": "repository",
                  "full_name": "acme/repository",
                  "html_url": "https://github.com/acme/repository",
                  "default_branch": "main",
                  "visibility": "restricted"
                }
                """,
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(
            () -> adapter.findByInstallationIdAndRepositoryId(INSTALLATION_ID, REPOSITORY_ID))
        .isInstanceOf(GitHubApiException.class)
        .hasMessageContaining("Unsupported GitHub repository visibility");

    server.verify();
  }

  @Test
  void shouldRejectMissingVisibility() {
    expectRepositoryRequest()
        .andRespond(
            withSuccess(
                """
                {
                  "id": 456,
                  "name": "repository",
                  "full_name": "acme/repository",
                  "html_url": "https://github.com/acme/repository",
                  "default_branch": "main"
                }
                """,
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(
            () -> adapter.findByInstallationIdAndRepositoryId(INSTALLATION_ID, REPOSITORY_ID))
        .isInstanceOf(GitHubApiException.class)
        .hasMessageContaining("visibility is missing");

    server.verify();
  }

  @Test
  void shouldRejectInvalidInstallationId() {
    assertThatThrownBy(() -> adapter.findByInstallationIdAndRepositoryId(0L, REPOSITORY_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("installation ID");

    verify(tokenClient, never()).createForInstallation(0L);
  }

  @Test
  void shouldRejectInvalidRepositoryId() {
    assertThatThrownBy(() -> adapter.findByInstallationIdAndRepositoryId(INSTALLATION_ID, 0L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("repository ID");

    verify(tokenClient, never()).createForInstallation(INSTALLATION_ID);
  }

  private ResponseActions expectRepositoryRequest() {
    return server
        .expect(requestTo(API_URL + "/repositories/" + REPOSITORY_ID))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer " + TOKEN))
        .andExpect(header("Accept", "application/vnd.github+json"))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"));
  }
}
