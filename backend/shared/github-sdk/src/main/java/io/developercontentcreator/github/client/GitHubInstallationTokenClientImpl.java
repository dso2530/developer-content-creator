package io.developercontentcreator.github.client;

import io.developercontentcreator.github.auth.GitHubAppJwtProvider;
import io.developercontentcreator.github.config.GitHubAppProperties;
import io.developercontentcreator.github.dto.GitHubInstallationTokenResponse;
import io.developercontentcreator.github.exception.GitHubApiException;
import io.developercontentcreator.github.model.GitHubInstallationToken;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class GitHubInstallationTokenClientImpl implements GitHubInstallationTokenClient {

  private static final String API_VERSION = "2022-11-28";

  private final RestClient restClient;
  private final GitHubAppJwtProvider jwtProvider;

  public GitHubInstallationTokenClientImpl(
      RestClient.Builder restClientBuilder,
      GitHubAppProperties properties,
      GitHubAppJwtProvider jwtProvider) {

    this.restClient =
        restClientBuilder
            .baseUrl(properties.getApiUrl().toString())
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("X-GitHub-Api-Version", API_VERSION)
            .build();

    this.jwtProvider = jwtProvider;
  }

  @Override
  public GitHubInstallationToken createForInstallation(Long installationId) {

    if (installationId == null || installationId <= 0) {
      throw new IllegalArgumentException("GitHub installation ID must be positive.");
    }

    String jwt = jwtProvider.generateToken();

    try {
      GitHubInstallationTokenResponse response =
          restClient
              .post()
              .uri("/app/installations/{installationId}/access_tokens", installationId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
              .retrieve()
              .body(GitHubInstallationTokenResponse.class);

      if (response == null
          || response.token() == null
          || response.token().isBlank()
          || response.expiresAt() == null) {
        throw new GitHubApiException(
            "GitHub returned an invalid installation token response.", 502);
      }

      return new GitHubInstallationToken(
          response.token(),
          response.expiresAt(),
          response.permissions(),
          response.repositorySelection());

    } catch (RestClientResponseException exception) {
      throw new GitHubApiException(
          "GitHub API rejected the installation token request.",
          exception.getStatusCode().value(),
          exception);

    } catch (ResourceAccessException exception) {
      throw new GitHubApiException("Unable to communicate with GitHub API.", 0, exception);
    } catch (RestClientException exception) {
      throw new GitHubApiException("GitHub API returned an unreadable response.", 502, exception);
    }
  }
}
