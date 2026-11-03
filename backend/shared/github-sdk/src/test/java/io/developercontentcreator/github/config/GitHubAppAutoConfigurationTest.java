package io.developercontentcreator.github.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.github.auth.GitHubAppJwtProvider;
import io.developercontentcreator.github.auth.GitHubPrivateKeyLoader;
import io.developercontentcreator.github.client.GitHubInstallationTokenClient;
import java.net.URI;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

class GitHubAppAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(GitHubAppAutoConfiguration.class))
          .withBean(RestClient.Builder.class, RestClient::builder);

  @Test
  void shouldAllowCustomGitHubAppJwtProvider() {
    contextRunner
        .withUserConfiguration(
            TestPrivateKeyLoaderConfiguration.class, TestJwtProviderConfiguration.class)
        .withPropertyValues(
            "developer-content-creator.github.app.enabled=true",
            "developer-content-creator.github.app.app-id=123456",
            "developer-content-creator.github.app.private-key-path=/etc/dcc/github-app.pem")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(GitHubAppJwtProvider.class);
              assertThat(context.getBean(GitHubAppJwtProvider.class))
                  .isSameAs(context.getBean("customGitHubAppJwtProvider"));
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class TestJwtProviderConfiguration {

    @Bean
    GitHubAppJwtProvider customGitHubAppJwtProvider(
        GitHubAppProperties properties, GitHubPrivateKeyLoader privateKeyLoader) {

      return new GitHubAppJwtProvider(properties, privateKeyLoader);
    }
  }

  @Test
  void shouldNotRegisterGitHubBeansWhenGitHubAppIsDisabled() {
    contextRunner
        .withPropertyValues("developer-content-creator.github.app.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(GitHubAppProperties.class);
              assertThat(context).doesNotHaveBean(GitHubPrivateKeyLoader.class);
              assertThat(context).doesNotHaveBean(GitHubAppJwtProvider.class);
              assertThat(context).doesNotHaveBean(GitHubInstallationTokenClient.class);
            });
  }

  @Test
  void shouldNotRegisterGitHubBeansWhenEnabledPropertyIsMissing() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(GitHubAppProperties.class);
          assertThat(context).doesNotHaveBean(GitHubPrivateKeyLoader.class);
          assertThat(context).doesNotHaveBean(GitHubAppJwtProvider.class);
          assertThat(context).doesNotHaveBean(GitHubInstallationTokenClient.class);
        });
  }

  @Test
  void shouldRegisterPropertiesWithValidConfiguration() {
    contextRunner
        .withUserConfiguration(TestPrivateKeyLoaderConfiguration.class)
        .withPropertyValues(
            "developer-content-creator.github.app.enabled=true",
            "developer-content-creator.github.app.app-id=123456",
            "developer-content-creator.github.app.private-key-path=/etc/dcc/github-app.pem",
            "developer-content-creator.github.app.jwt-validity=9m",
            "developer-content-creator.github.app.api-url=https://api.github.com")
        .run(
            context -> {
              assertThat(context).hasNotFailed();

              GitHubAppProperties properties = context.getBean(GitHubAppProperties.class);

              assertThat(properties.getAppId()).isEqualTo(123456L);
              assertThat(properties.getPrivateKeyPath())
                  .isEqualTo(Path.of("/etc/dcc/github-app.pem"));
              assertThat(properties.getJwtValidity()).isEqualTo(Duration.ofMinutes(9));
              assertThat(properties.getApiUrl()).isEqualTo(URI.create("https://api.github.com"));
            });
  }

  @Test
  void shouldRejectNonPositiveAppId() {
    contextRunner
        .withUserConfiguration(TestPrivateKeyLoaderConfiguration.class)
        .withPropertyValues(
            "developer-content-creator.github.app.enabled=true",
            "developer-content-creator.github.app.app-id=0",
            "developer-content-creator.github.app.private-key-path=/etc/dcc/github-app.pem")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseInstanceOf(IllegalStateException.class);
            });
  }

  @Test
  void shouldRejectRelativePrivateKeyPath() {
    contextRunner
        .withUserConfiguration(TestPrivateKeyLoaderConfiguration.class)
        .withPropertyValues(
            "developer-content-creator.github.app.enabled=true",
            "developer-content-creator.github.app.app-id=123456",
            "developer-content-creator.github.app.private-key-path=secrets/github-app.pem")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseInstanceOf(IllegalStateException.class);
            });
  }

  @Test
  void shouldRejectJwtValidityGreaterThanTenMinutes() {
    contextRunner
        .withUserConfiguration(TestPrivateKeyLoaderConfiguration.class)
        .withPropertyValues(
            "developer-content-creator.github.app.enabled=true",
            "developer-content-creator.github.app.app-id=123456",
            "developer-content-creator.github.app.private-key-path=/etc/dcc/github-app.pem",
            "developer-content-creator.github.app.jwt-validity=11m")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseInstanceOf(IllegalStateException.class);
            });
  }

  @Test
  void shouldRejectNonHttpsApiUrl() {
    contextRunner
        .withUserConfiguration(TestPrivateKeyLoaderConfiguration.class)
        .withPropertyValues(
            "developer-content-creator.github.app.enabled=true",
            "developer-content-creator.github.app.app-id=123456",
            "developer-content-creator.github.app.private-key-path=/etc/dcc/github-app.pem",
            "developer-content-creator.github.app.api-url=http://api.github.com")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseInstanceOf(IllegalStateException.class);
            });
  }

  @Test
  void shouldRegisterGitHubAuthenticationBeansWhenEnabled() {
    contextRunner
        .withUserConfiguration(TestPrivateKeyLoaderConfiguration.class)
        .withPropertyValues(
            "developer-content-creator.github.app.enabled=true",
            "developer-content-creator.github.app.app-id=123456",
            "developer-content-creator.github.app.private-key-path=/etc/dcc/github-app.pem")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(GitHubPrivateKeyLoader.class);
              assertThat(context).hasSingleBean(GitHubAppJwtProvider.class);
              assertThat(context).hasSingleBean(GitHubInstallationTokenClient.class);
            });
  }

  @Test
  void shouldAllowCustomGitHubInstallationTokenClient() {
    contextRunner
        .withUserConfiguration(
            TestPrivateKeyLoaderConfiguration.class, TestInstallationTokenClientConfiguration.class)
        .withPropertyValues(
            "developer-content-creator.github.app.enabled=true",
            "developer-content-creator.github.app.app-id=123456",
            "developer-content-creator.github.app.private-key-path=/etc/dcc/github-app.pem")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(GitHubInstallationTokenClient.class);
              assertThat(context.getBean(GitHubInstallationTokenClient.class))
                  .isSameAs(context.getBean("customGitHubInstallationTokenClient"));
            });
  }

  @Test
  void shouldAllowCustomGitHubPrivateKeyLoader() {
    contextRunner
        .withUserConfiguration(TestPrivateKeyLoaderConfiguration.class)
        .withPropertyValues(
            "developer-content-creator.github.app.enabled=true",
            "developer-content-creator.github.app.app-id=123456",
            "developer-content-creator.github.app.private-key-path=/etc/dcc/github-app.pem")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(GitHubPrivateKeyLoader.class);
              assertThat(context.getBean(GitHubPrivateKeyLoader.class))
                  .isInstanceOf(TestGitHubPrivateKeyLoader.class);
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class TestPrivateKeyLoaderConfiguration {

    @Bean
    GitHubPrivateKeyLoader testGitHubPrivateKeyLoader() {
      return new TestGitHubPrivateKeyLoader();
    }
  }

  static class TestGitHubPrivateKeyLoader extends GitHubPrivateKeyLoader {

    @Override
    public PrivateKey load(Path privateKeyPath) {
      try {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair().getPrivate();
      } catch (GeneralSecurityException exception) {
        throw new IllegalStateException(exception);
      }
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class TestInstallationTokenClientConfiguration {

    @Bean
    GitHubInstallationTokenClient customGitHubInstallationTokenClient() {
      return installationId -> {
        throw new UnsupportedOperationException("Test implementation");
      };
    }
  }
}
