package io.developercontentcreator.github.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import io.developercontentcreator.github.auth.GitHubAppJwtProvider;
import io.developercontentcreator.github.config.GitHubAppProperties;
import io.developercontentcreator.github.exception.GitHubApiException;
import io.developercontentcreator.github.model.GitHubInstallationToken;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GitHubInstallationTokenClientImplTest {

  private static final String API_URL = "https://api.github.com";
  private static final String JWT = "test-github-app-jwt";

  private GitHubAppJwtProvider jwtProvider;
  private GitHubAppProperties properties;
  private RestClient.Builder restClientBuilder;
  private MockRestServiceServer mockServer;

  private GitHubInstallationTokenClientImpl client;

  @BeforeEach
  void setUp() {
    jwtProvider = mock(GitHubAppJwtProvider.class);
    when(jwtProvider.generateToken()).thenReturn(JWT);

    properties = new GitHubAppProperties();
    properties.setApiUrl(java.net.URI.create(API_URL));

    restClientBuilder = RestClient.builder();

    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    client = new GitHubInstallationTokenClientImpl(restClientBuilder, properties, jwtProvider);
  }

  @Test
  void shouldCreateInstallationToken() {

    String response =
        """
        {
          "token": "ghs-installation-token",
          "expires_at": "2026-11-02T21:00:00Z",
          "permissions": {
            "contents": "read",
            "metadata": "read"
          },
          "repository_selection": "all"
        }
        """;

    mockServer
        .expect(requestTo(API_URL + "/app/installations/123/access_tokens"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + JWT))
        .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
        .andExpect(content().string(""))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    GitHubInstallationToken token = client.createForInstallation(123L);

    assertEquals("ghs-installation-token", token.getToken());
    assertEquals(Instant.parse("2026-11-02T21:00:00Z"), token.getExpiresAt());
    assertEquals("read", token.getPermissions().get("contents"));
    assertEquals("read", token.getPermissions().get("metadata"));
    assertEquals("all", token.getRepositorySelection());

    verify(jwtProvider).generateToken();
    mockServer.verify();
  }

  @Test
  void shouldRejectInvalidInstallationId() {

    assertThrows(IllegalArgumentException.class, () -> client.createForInstallation(0L));

    assertThrows(IllegalArgumentException.class, () -> client.createForInstallation(-1L));

    assertThrows(IllegalArgumentException.class, () -> client.createForInstallation(null));

    verifyNoInteractions(jwtProvider);
  }

  @Test
  void shouldConvertUnauthorizedResponseToGitHubApiException() {

    mockServer
        .expect(requestTo(API_URL + "/app/installations/123/access_tokens"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    GitHubApiException exception =
        assertThrows(GitHubApiException.class, () -> client.createForInstallation(123L));

    assertEquals(401, exception.getStatusCode());
    assertFalse(exception.getMessage().contains(JWT));

    mockServer.verify();
  }

  @Test
  void shouldConvertForbiddenResponseToGitHubApiException() {

    mockServer
        .expect(requestTo(API_URL + "/app/installations/123/access_tokens"))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));

    GitHubApiException exception =
        assertThrows(GitHubApiException.class, () -> client.createForInstallation(123L));

    assertEquals(403, exception.getStatusCode());

    mockServer.verify();
  }

  @Test
  void shouldConvertServerErrorToGitHubApiException() {

    mockServer
        .expect(requestTo(API_URL + "/app/installations/123/access_tokens"))
        .andRespond(withServerError());

    GitHubApiException exception =
        assertThrows(GitHubApiException.class, () -> client.createForInstallation(123L));

    assertEquals(500, exception.getStatusCode());

    mockServer.verify();
  }

  @Test
  void shouldRejectEmptyGitHubResponse() {

    mockServer
        .expect(requestTo(API_URL + "/app/installations/123/access_tokens"))
        .andRespond(withSuccess());

    GitHubApiException exception =
        assertThrows(GitHubApiException.class, () -> client.createForInstallation(123L));

    assertEquals(502, exception.getStatusCode());

    mockServer.verify();
  }

  @Test
  void shouldRejectResponseWithoutToken() {

    String response =
        """
        {
          "expires_at": "2026-11-02T21:00:00Z",
          "permissions": {},
          "repository_selection": "all"
        }
        """;

    mockServer
        .expect(requestTo(API_URL + "/app/installations/123/access_tokens"))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    GitHubApiException exception =
        assertThrows(GitHubApiException.class, () -> client.createForInstallation(123L));

    assertEquals(502, exception.getStatusCode());

    mockServer.verify();
  }

  @Test
  void shouldRejectResponseWithoutExpiration() {

    String response =
        """
        {
          "token": "ghs-installation-token",
          "permissions": {},
          "repository_selection": "all"
        }
        """;

    mockServer
        .expect(requestTo(API_URL + "/app/installations/123/access_tokens"))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    GitHubApiException exception =
        assertThrows(GitHubApiException.class, () -> client.createForInstallation(123L));

    assertEquals(502, exception.getStatusCode());

    mockServer.verify();
  }

  @Test
  void shouldConvertNetworkErrorToGitHubApiException() {

    mockServer
        .expect(requestTo(API_URL + "/app/installations/123/access_tokens"))
        .andRespond(
            request -> {
              throw new IOException("Connection refused");
            });

    GitHubApiException exception =
        assertThrows(GitHubApiException.class, () -> client.createForInstallation(123L));

    assertEquals(0, exception.getStatusCode());
    assertEquals("Unable to communicate with GitHub API.", exception.getMessage());

    mockServer.verify();
  }

  @Test
  void shouldConvertMalformedJsonResponseToGitHubApiException() {

    mockServer
        .expect(requestTo(API_URL + "/app/installations/123/access_tokens"))
        .andRespond(withSuccess("{ invalid-json }", MediaType.APPLICATION_JSON));

    GitHubApiException exception =
        assertThrows(GitHubApiException.class, () -> client.createForInstallation(123L));

    assertEquals(502, exception.getStatusCode());

    mockServer.verify();
  }
}
