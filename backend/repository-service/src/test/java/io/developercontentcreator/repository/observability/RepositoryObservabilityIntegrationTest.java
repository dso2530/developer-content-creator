package io.developercontentcreator.repository.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@SpringBootTest(
    classes = RepositoryObservabilityIntegrationTest.TestApplication.class,
    properties = {
      "spring.application.name=repository-observability-test",
      "spring.main.web-application-type=none",
      "management.tracing.sampling.probability=1.0",
      "management.tracing.export.otlp.enabled=false",
      "management.otlp.metrics.export.enabled=false",
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
          + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
      "platform.observability.logging.masking.enabled=true",
      "logging.structured.format.console="
          + "com.dso2530.starters.observability.logging.SensitiveDataStructuredLogFormatter"
    })
@ExtendWith(OutputCaptureExtension.class)
class RepositoryObservabilityIntegrationTest {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RepositoryObservabilityIntegrationTest.class);

  @Autowired private Tracer tracer;

  @Test
  void shouldIncludeTraceIdAndSpanIdInStructuredLogs(CapturedOutput output) {
    Span span = tracer.nextSpan().name("repository-observability-test").start();

    try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
      LOGGER.info("Testing distributed trace logging");

      assertThat(tracer.currentSpan()).isNotNull();

      assertThat(output.getOut())
          .contains("\"traceId\":\"" + span.context().traceId() + "\"")
          .contains("\"spanId\":\"" + span.context().spanId() + "\"")
          .contains("\"message\":\"Testing distributed trace logging\"");
    } finally {
      span.end();
    }
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestApplication {}
}
