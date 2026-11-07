package io.developercontentcreator.logging.formatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SensitiveDataStructuredLogFormatterTest {

  private MockEnvironment environment;

  @BeforeEach
  void setUp() {
    environment = new MockEnvironment();
  }

  @Test
  void shouldFormatStandardLogEvent() {
    ILoggingEvent event = createEvent("Application started");

    String result = createFormatter().format(event);

    assertThat(result)
        .contains("\"@timestamp\"")
        .contains("\"@version\":\"1\"")
        .contains("\"message\":\"Application started\"")
        .contains("\"logger_name\":\"test.logger\"")
        .contains("\"thread_name\":\"main\"")
        .contains("\"level\":\"INFO\"")
        .contains("\"level_value\":20000");
  }

  @Test
  void shouldMaskPasswordInMessage() {
    ILoggingEvent event = createEvent("Authentication failed: password=super-secret");

    String result = createFormatter().format(event);

    assertThat(result).contains("[REDACTED]").doesNotContain("super-secret");
  }

  @Test
  void shouldMaskEmailInMessage() {
    ILoggingEvent event = createEvent("User email: john.doe@example.com");

    String result = createFormatter().format(event);

    assertThat(result).contains("[REDACTED]").doesNotContain("john.doe@example.com");
  }

  @Test
  void shouldMaskJwtInMessage() {
    String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature123456789";

    ILoggingEvent event = createEvent("Authorization token: " + jwt);

    String result = createFormatter().format(event);

    assertThat(result).contains("[REDACTED]").doesNotContain(jwt);
  }

  @Test
  void shouldPreserveMessageWhenMaskingIsDisabled() {
    environment.setProperty("developer-content-creator.logging.masking.enabled", "false");

    ILoggingEvent event = createEvent("password=super-secret");

    String result = createFormatter().format(event);

    assertThat(result).contains("password=super-secret");
  }

  @Test
  void shouldIncludeCorrelationIdFromMdc() {
    ILoggingEvent event = createEvent("Request processed");

    when(event.getMDCPropertyMap()).thenReturn(Map.of("correlationId", "corr-123456"));

    String result = createFormatter().format(event);

    assertThat(result).contains("\"correlationId\":\"corr-123456\"");
  }

  @Test
  void shouldOmitCorrelationIdWhenNotPresent() {
    ILoggingEvent event = createEvent("Request processed");

    when(event.getMDCPropertyMap()).thenReturn(Map.of());

    String result = createFormatter().format(event);

    assertThat(result).doesNotContain("\"correlationId\"");
  }

  @Test
  void shouldMaskSensitiveDataInStackTrace() {
    IllegalStateException exception =
        new IllegalStateException("Operation failed: password=super-secret");

    ILoggingEvent event = createEvent("Operation failed");

    when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(exception));

    String result = createFormatter().format(event);

    assertThat(result)
        .contains("\"stack_trace\"")
        .contains("[REDACTED]")
        .doesNotContain("super-secret");
  }

  @Test
  void shouldOmitStackTraceWhenNoExceptionIsPresent() {
    ILoggingEvent event = createEvent("Operation completed");

    when(event.getThrowableProxy()).thenReturn(null);

    String result = createFormatter().format(event);

    assertThat(result).doesNotContain("\"stack_trace\"");
  }

  @Test
  void shouldApplyCustomJsonMembersCustomizer() {
    ILoggingEvent event = createEvent("Application started");

    String result =
        new SensitiveDataStructuredLogFormatter(
                environment, members -> members.add("service", "repository-service"))
            .format(event);

    assertThat(result).contains("\"service\":\"repository-service\"");
  }

  @Test
  void shouldPreserveOrdinaryMessage() {
    ILoggingEvent event = createEvent("Repository analyzed successfully");

    String result = createFormatter().format(event);

    assertThat(result).contains("Repository analyzed successfully");
  }

  private SensitiveDataStructuredLogFormatter createFormatter() {
    return new SensitiveDataStructuredLogFormatter(environment, null);
  }

  private ILoggingEvent createEvent(String message) {
    ILoggingEvent event = mock(ILoggingEvent.class);

    when(event.getInstant()).thenReturn(Instant.parse("2026-11-07T10:00:00Z"));
    when(event.getFormattedMessage()).thenReturn(message);
    when(event.getLoggerName()).thenReturn("test.logger");
    when(event.getThreadName()).thenReturn("main");
    when(event.getLevel()).thenReturn(Level.INFO);
    when(event.getMDCPropertyMap()).thenReturn(Map.of());
    when(event.getThrowableProxy()).thenReturn(null);

    return event;
  }
}
