package io.developercontentcreator.logging.auto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.developercontentcreator.logging.context.CorrelationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class LoggingRestClientAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  LoggingPropertiesAutoConfiguration.class,
                  LoggingRestClientAutoConfiguration.class));

  @AfterEach
  void tearDown() {
    CorrelationContext.clear();
  }

  @Test
  void shouldRegisterRestClientCustomizerByDefault() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(RestClientCustomizer.class);
        });
  }

  @Test
  void shouldNotRegisterCustomizerWhenDisabled() {
    contextRunner
        .withPropertyValues("developer-content-creator.logging.correlation.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(RestClientCustomizer.class);
            });
  }

  @Test
  void shouldPropagateCorrelationIdThroughRestClient() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();

          RestClientCustomizer customizer = context.getBean(RestClientCustomizer.class);

          RestClient.Builder builder = RestClient.builder();

          customizer.customize(builder);

          MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

          server
              .expect(requestTo("https://example.test/api/repositories"))
              .andExpect(header("X-Correlation-ID", "correlation-http-123"))
              .andRespond(withSuccess());

          CorrelationContext.setCorrelationId("correlation-http-123");

          RestClient restClient = builder.build();

          restClient
              .get()
              .uri("https://example.test/api/repositories")
              .retrieve()
              .toBodilessEntity();

          server.verify();
        });
  }
}
