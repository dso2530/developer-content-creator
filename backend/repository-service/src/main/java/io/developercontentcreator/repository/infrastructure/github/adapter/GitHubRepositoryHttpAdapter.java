package io.developercontentcreator.repository.infrastructure.github.adapter;

import io.developercontentcreator.github.client.GitHubInstallationTokenClient;
import io.developercontentcreator.github.exception.GitHubApiException;
import io.developercontentcreator.github.model.GitHubInstallationToken;
import io.developercontentcreator.repository.application.port.out.LoadGitHubRepositoryPort;
import io.developercontentcreator.repository.domain.model.GitHubRepositorySnapshot;
import io.developercontentcreator.repository.domain.model.RepositoryVisibility;
import io.developercontentcreator.repository.infrastructure.github.dto.GitHubRepositoryResponse;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class GitHubRepositoryHttpAdapter implements LoadGitHubRepositoryPort {

  private static final String GITHUB_API_VERSION = "2022-11-28";

  private final RestClient restClient;
  private final GitHubInstallationTokenClient tokenClient;

  public GitHubRepositoryHttpAdapter(
      RestClient restClient, GitHubInstallationTokenClient tokenClient) {

    this.restClient = restClient;
    this.tokenClient = tokenClient;
  }

  @Override
  public Optional<GitHubRepositorySnapshot> findByInstallationIdAndRepositoryId(
      Long installationId, Long githubRepositoryId) {

    if (installationId == null || installationId <= 0) {
      throw new IllegalArgumentException("GitHub installation ID must be positive");
    }

    if (githubRepositoryId == null || githubRepositoryId <= 0) {
      throw new IllegalArgumentException("GitHub repository ID must be positive");
    }

    GitHubInstallationToken installationToken = tokenClient.createForInstallation(installationId);

    try {
      GitHubRepositoryResponse response =
          restClient
              .get()
              .uri("/repositories/{repositoryId}", githubRepositoryId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken.getToken())
              .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
              .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
              .retrieve()
              .body(GitHubRepositoryResponse.class);

      if (response == null) {
        throw new GitHubApiException("GitHub returned an empty repository response", 502);
      }

      return Optional.of(toSnapshot(response));

    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 404) {
        return Optional.empty();
      }

      throw new GitHubApiException(
          "GitHub repository API returned HTTP " + exception.getStatusCode().value(),
          exception.getStatusCode().value(),
          exception);

    } catch (ResourceAccessException exception) {
      throw new GitHubApiException("Unable to reach GitHub repository API", 0, exception);

    } catch (RestClientException exception) {
      throw new GitHubApiException(
          "Unable to process GitHub repository API response", 502, exception);
    }
  }

  private GitHubRepositorySnapshot toSnapshot(GitHubRepositoryResponse response) {
    if (response.visibility() == null) {
      throw new GitHubApiException("GitHub repository visibility is missing", 502);
    }

    RepositoryVisibility visibility =
        switch (response.visibility()) {
          case "public" -> RepositoryVisibility.PUBLIC;
          case "private", "internal" -> RepositoryVisibility.PRIVATE;
          default ->
              throw new GitHubApiException(
                  "Unsupported GitHub repository visibility: " + response.visibility(), 502);
        };

    return new GitHubRepositorySnapshot(
        response.id(),
        response.name(),
        response.fullName(),
        response.htmlUrl(),
        response.defaultBranch(),
        visibility);
  }
}
