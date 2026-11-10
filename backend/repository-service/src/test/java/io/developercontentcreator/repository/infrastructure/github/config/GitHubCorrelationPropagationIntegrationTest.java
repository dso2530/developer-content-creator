package io.developercontentcreator.repository.infrastructure.github.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.dso2530.starters.core.correlation.CorrelationContext;
import com.dso2530.starters.web.autoconfigure.WebAutoConfiguration;
import io.developercontentcreator.github.auth.GitHubAppJwtProvider;
import io.developercontentcreator.github.client.GitHubInstallationTokenClientImpl;
import io.developercontentcreator.github.config.GitHubAppProperties;
import io.developercontentcreator.github.exception.GitHubApiException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GitHubCorrelationPropagationIntegrationTest {

  private static final String API_URL = "https://api.github.com";

  private static final String CORRELATION_ID = "test-correlation-123";

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(RestClientAutoConfiguration.class, WebAutoConfiguration.class))
          .withUserConfiguration(TestConfiguration.class)
          .withBean(
              GitHubAppProperties.class,
              () -> {
                GitHubAppProperties properties = mock(GitHubAppProperties.class);
                when(properties.getApiUrl()).thenReturn(URI.create(API_URL));
                return properties;
              })
          .withBean(
              GitHubAppJwtProvider.class,
              () -> {
                GitHubAppJwtProvider jwtProvider = mock(GitHubAppJwtProvider.class);
                when(jwtProvider.generateToken()).thenReturn("test-jwt");
                return jwtProvider;
              });

  @Test
  void shouldPropagateCorrelationIdToGitHubInstallationTokenRequest() {

    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();

          GitHubTestFixture fixture = context.getBean(GitHubTestFixture.class);

          fixture
              .mockServer()
              .expect(requestTo(API_URL + "/app/installations/123456/access_tokens"))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header("X-Correlation-ID", CORRELATION_ID))
              .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

          try (CorrelationContext.Scope ignored = CorrelationContext.open(CORRELATION_ID)) {

            assertThatThrownBy(() -> fixture.client().createForInstallation(123456L))
                .isInstanceOf(GitHubApiException.class);
          }

          fixture.mockServer().verify();
        });
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {

    @Bean
    GitHubTestFixture gitHubTestFixture(
        RestClient.Builder restClientBuilder,
        GitHubAppProperties properties,
        GitHubAppJwtProvider jwtProvider) {

      MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

      GitHubInstallationTokenClientImpl client =
          new GitHubInstallationTokenClientImpl(restClientBuilder, properties, jwtProvider);

      return new GitHubTestFixture(client, mockServer);
    }
  }

  record GitHubTestFixture(
      GitHubInstallationTokenClientImpl client, MockRestServiceServer mockServer) {}
}
