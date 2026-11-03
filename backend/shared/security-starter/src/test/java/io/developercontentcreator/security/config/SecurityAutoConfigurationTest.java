package io.developercontentcreator.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.security.authentication.CurrentUserProvider;
import io.developercontentcreator.security.authentication.SpringCurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;

class SecurityAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class));

  @Test
  void shouldCreateDefaultCurrentUserProvider() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(SecurityProperties.class);
          assertThat(context).hasSingleBean(CurrentUserProvider.class);
          assertThat(context.getBean(CurrentUserProvider.class))
              .isInstanceOf(SpringCurrentUserProvider.class);
        });
  }

  @Test
  void shouldDisableSecurityStarterWhenPropertyIsFalse() {
    contextRunner
        .withPropertyValues("developer-content-creator.security.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(SecurityProperties.class);
              assertThat(context).doesNotHaveBean(CurrentUserProvider.class);
            });
  }

  @Test
  void shouldBackOffWhenCustomCurrentUserProviderExists() {
    contextRunner
        .withUserConfiguration(CustomProviderConfiguration.class)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(CurrentUserProvider.class);
              assertThat(context.getBean(CurrentUserProvider.class))
                  .isSameAs(context.getBean(CustomCurrentUserProvider.class));
            });
  }

  @Test
  void shouldNotCreateSecurityFilterChain() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(SecurityFilterChain.class);
        });
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomProviderConfiguration {

    @Bean
    CustomCurrentUserProvider customCurrentUserProvider() {
      return new CustomCurrentUserProvider();
    }
  }

  static class CustomCurrentUserProvider implements CurrentUserProvider {

    @Override
    public io.developercontentcreator.security.authentication.AuthenticatedUser getCurrentUser() {
      throw new UnsupportedOperationException("Test implementation");
    }
  }
}
