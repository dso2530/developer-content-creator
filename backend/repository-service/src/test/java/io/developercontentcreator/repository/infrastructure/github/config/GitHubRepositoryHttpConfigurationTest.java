package io.developercontentcreator.repository.infrastructure.github.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.developercontentcreator.github.client.GitHubInstallationTokenClient;
import io.developercontentcreator.github.config.GitHubAppProperties;
import io.developercontentcreator.repository.application.port.out.LoadGitHubRepositoryPort;
import io.developercontentcreator.repository.infrastructure.github.adapter.GitHubRepositoryDisabledAdapter;
import io.developercontentcreator.repository.infrastructure.github.adapter.GitHubRepositoryHttpAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class GitHubRepositoryHttpConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(GitHubRepositoryHttpConfiguration.class);

  @Test
  void shouldRegisterHttpAdapterWhenGitHubIntegrationIsEnabled() {
    contextRunner
        .withPropertyValues("developer-content-creator.github.app.enabled=true")
        .withBean(RestClient.Builder.class, RestClient::builder)
        .withBean(
            GitHubAppProperties.class,
            () -> {
              var properties = mock(GitHubAppProperties.class);
              org.mockito.Mockito.when(properties.getApiUrl())
                  .thenReturn(java.net.URI.create("https://api.github.com"));
              return properties;
            })
        .withBean(
            GitHubInstallationTokenClient.class, () -> mock(GitHubInstallationTokenClient.class))
        .run(
            context -> {
              assertThat(context).hasSingleBean(LoadGitHubRepositoryPort.class);
              assertThat(context.getBean(LoadGitHubRepositoryPort.class))
                  .isInstanceOf(GitHubRepositoryHttpAdapter.class);
            });
  }

  @Test
  void shouldRegisterDisabledAdapterWhenGitHubIntegrationIsDisabled() {
    contextRunner
        .withPropertyValues("developer-content-creator.github.app.enabled=false")
        .run(
            context -> {
              assertThat(context).hasSingleBean(LoadGitHubRepositoryPort.class);
              assertThat(context.getBean(LoadGitHubRepositoryPort.class))
                  .isInstanceOf(GitHubRepositoryDisabledAdapter.class);
            });
  }

  @Test
  void shouldRegisterDisabledAdapterWhenPropertyIsMissing() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(LoadGitHubRepositoryPort.class);
          assertThat(context.getBean(LoadGitHubRepositoryPort.class))
              .isInstanceOf(GitHubRepositoryDisabledAdapter.class);
        });
  }
}
