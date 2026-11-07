package io.developercontentcreator.logging.auto;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.logging.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class LoggingAutoConfigurationTest {

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(LoggingAutoConfiguration.class))
          .withPropertyValues("spring.main.web-application-type=servlet");

  @Test
  void shouldRegisterCorrelationFilterByDefault() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(CorrelationIdFilter.class);
        });
  }

  @Test
  void shouldNotRegisterFilterWhenDisabled() {
    contextRunner
        .withPropertyValues("developer-content-creator.logging.correlation.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(CorrelationIdFilter.class);
            });
  }
}
