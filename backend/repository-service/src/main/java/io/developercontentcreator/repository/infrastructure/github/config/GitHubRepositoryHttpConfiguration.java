package io.developercontentcreator.repository.infrastructure.github.config;

import io.developercontentcreator.github.client.GitHubInstallationTokenClient;
import io.developercontentcreator.github.config.GitHubAppProperties;
import io.developercontentcreator.repository.application.port.out.LoadGitHubRepositoryPort;
import io.developercontentcreator.repository.infrastructure.github.adapter.GitHubRepositoryDisabledAdapter;
import io.developercontentcreator.repository.infrastructure.github.adapter.GitHubRepositoryHttpAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class GitHubRepositoryHttpConfiguration {

  @Bean
  @ConditionalOnProperty(
      prefix = "developer-content-creator.github.app",
      name = "enabled",
      havingValue = "true")
  GitHubRepositoryHttpAdapter gitHubRepositoryHttpAdapter(
      RestClient.Builder restClientBuilder,
      GitHubAppProperties properties,
      GitHubInstallationTokenClient tokenClient) {

    RestClient restClient = restClientBuilder.baseUrl(properties.getApiUrl().toString()).build();

    return new GitHubRepositoryHttpAdapter(restClient, tokenClient);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "developer-content-creator.github.app",
      name = "enabled",
      havingValue = "false",
      matchIfMissing = true)
  LoadGitHubRepositoryPort gitHubRepositoryDisabledAdapter() {

    return new GitHubRepositoryDisabledAdapter();
  }
}
