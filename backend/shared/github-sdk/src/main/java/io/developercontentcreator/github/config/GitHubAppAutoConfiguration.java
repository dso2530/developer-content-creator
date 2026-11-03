package io.developercontentcreator.github.config;

import io.developercontentcreator.github.auth.GitHubAppJwtProvider;
import io.developercontentcreator.github.auth.GitHubPrivateKeyLoader;
import io.developercontentcreator.github.client.GitHubInstallationTokenClient;
import io.developercontentcreator.github.client.GitHubInstallationTokenClientImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "developer-content-creator.github.app",
    name = "enabled",
    havingValue = "true")
@EnableConfigurationProperties(GitHubAppProperties.class)
public class GitHubAppAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  GitHubPrivateKeyLoader gitHubPrivateKeyLoader() {
    return new GitHubPrivateKeyLoader();
  }

  @Bean
  @ConditionalOnMissingBean
  GitHubAppJwtProvider gitHubAppJwtProvider(
      GitHubAppProperties properties, GitHubPrivateKeyLoader privateKeyLoader) {
    return new GitHubAppJwtProvider(properties, privateKeyLoader);
  }

  @Bean
  @ConditionalOnMissingBean(GitHubInstallationTokenClient.class)
  GitHubInstallationTokenClient gitHubInstallationTokenClient(
      RestClient.Builder restClientBuilder,
      GitHubAppProperties properties,
      GitHubAppJwtProvider jwtProvider) {

    return new GitHubInstallationTokenClientImpl(restClientBuilder, properties, jwtProvider);
  }
}
