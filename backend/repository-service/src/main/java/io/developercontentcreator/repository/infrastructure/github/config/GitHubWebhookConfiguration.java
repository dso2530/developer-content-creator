package io.developercontentcreator.repository.infrastructure.github.config;

import io.developercontentcreator.repository.infrastructure.github.webhook.GitHubWebhookSignatureValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GitHubWebhookProperties.class)
public class GitHubWebhookConfiguration {

  @Bean
  @ConditionalOnProperty(
      prefix = "developer-content-creator.github.webhook",
      name = "enabled",
      havingValue = "true")
  GitHubWebhookSignatureValidator gitHubWebhookSignatureValidator(
      GitHubWebhookProperties properties) {

    return new GitHubWebhookSignatureValidator(properties.getSecret());
  }
}
