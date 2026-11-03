package io.developercontentcreator.repository.infrastructure.github.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "developer-content-creator.github.webhook")
public class GitHubWebhookProperties implements InitializingBean {

  private boolean enabled;
  private String secret;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  @Override
  public void afterPropertiesSet() {
    if (enabled && (secret == null || secret.isBlank())) {
      throw new IllegalStateException(
          "GitHub webhook configuration requires a non-blank secret when enabled.");
    }
  }
}
