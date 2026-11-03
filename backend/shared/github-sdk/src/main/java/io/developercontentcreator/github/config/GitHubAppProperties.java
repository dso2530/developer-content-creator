package io.developercontentcreator.github.config;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "developer-content-creator.github.app")
public class GitHubAppProperties implements InitializingBean {

  private Long appId;
  private Path privateKeyPath;
  private URI apiUrl = URI.create("https://api.github.com");
  private Duration jwtValidity = Duration.ofMinutes(9);

  public Long getAppId() {
    return appId;
  }

  public void setAppId(Long appId) {
    this.appId = appId;
  }

  public Path getPrivateKeyPath() {
    return privateKeyPath;
  }

  public void setPrivateKeyPath(Path privateKeyPath) {
    this.privateKeyPath = privateKeyPath;
  }

  public URI getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(URI apiUrl) {
    this.apiUrl = apiUrl;
  }

  public Duration getJwtValidity() {
    return jwtValidity;
  }

  public void setJwtValidity(Duration jwtValidity) {
    this.jwtValidity = jwtValidity;
  }

  @Override
  public void afterPropertiesSet() {
    validate();
  }

  private void validate() {
    if (appId == null || appId <= 0) {
      throw new IllegalStateException("GitHub App configuration requires a positive app-id.");
    }

    if (privateKeyPath == null || !privateKeyPath.isAbsolute()) {
      throw new IllegalStateException(
          "GitHub App configuration requires an absolute private-key-path.");
    }

    if (jwtValidity == null
        || jwtValidity.isZero()
        || jwtValidity.isNegative()
        || jwtValidity.compareTo(Duration.ofMinutes(10)) > 0) {
      throw new IllegalStateException(
          "GitHub App jwt-validity must be positive and no greater than 10 minutes.");
    }

    if (apiUrl == null
        || !apiUrl.isAbsolute()
        || !"https".equalsIgnoreCase(apiUrl.getScheme())
        || apiUrl.getHost() == null
        || apiUrl.getHost().isBlank()) {
      throw new IllegalStateException("GitHub App api-url must be an absolute HTTPS URL.");
    }
  }
}
