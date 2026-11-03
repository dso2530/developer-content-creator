package io.developercontentcreator.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class SecurityPropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(SecurityPropertiesTest.PropertiesConfiguration.class);

  @Test
  void shouldExposeDefaultSecurityProperties() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(SecurityProperties.class);

          SecurityProperties properties = context.getBean(SecurityProperties.class);

          assertThat(properties.isEnabled()).isTrue();
          assertThat(properties.getUserIdClaim()).isEqualTo("dcc_user_id");
          assertThat(properties.getUsernameClaim()).isEqualTo("preferred_username");
          assertThat(properties.getAuthoritiesClaim()).isEqualTo("roles");
        });
  }

  @Test
  void shouldBindCustomJwtClaims() {
    contextRunner
        .withPropertyValues(
            "developer-content-creator.security.user-id-claim=internal_user_id",
            "developer-content-creator.security.username-claim=email",
            "developer-content-creator.security.authorities-claim=permissions")
        .run(
            context -> {
              assertThat(context).hasNotFailed();

              SecurityProperties properties = context.getBean(SecurityProperties.class);

              assertThat(properties.getUserIdClaim()).isEqualTo("internal_user_id");
              assertThat(properties.getUsernameClaim()).isEqualTo("email");
              assertThat(properties.getAuthoritiesClaim()).isEqualTo("permissions");
            });
  }

  @Test
  void shouldBindSecurityEnabledProperty() {
    contextRunner
        .withPropertyValues("developer-content-creator.security.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();

              SecurityProperties properties = context.getBean(SecurityProperties.class);

              assertThat(properties.isEnabled()).isFalse();
            });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(SecurityProperties.class)
  static class PropertiesConfiguration {}
}
