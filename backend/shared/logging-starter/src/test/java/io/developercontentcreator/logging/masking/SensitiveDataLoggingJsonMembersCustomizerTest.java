package io.developercontentcreator.logging.masking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonWriter;
import org.springframework.mock.env.MockEnvironment;

class SensitiveDataLoggingJsonMembersCustomizerTest {

  private static final String PREFIX = "developer-content-creator.logging.masking";

  @Test
  void shouldMaskSensitiveValuesInStructuredJson() {

    MockEnvironment environment =
        new MockEnvironment()
            .withProperty(PREFIX + ".enabled", "true")
            .withProperty(PREFIX + ".replacement", "[REDACTED]")
            .withProperty(PREFIX + ".mask-email", "true");

    var customizer = new SensitiveDataLoggingJsonMembersCustomizer(environment);

    JsonWriter<ILoggingEvent> writer =
        JsonWriter.of(
            members -> {
              members.add("message", ILoggingEvent::getFormattedMessage);
              members.add("password", ILoggingEvent::getFormattedMessage);

              customizer.customize(members);
            });

    ILoggingEvent event = mock(ILoggingEvent.class);

    when(event.getFormattedMessage()).thenReturn("Contact user@example.com");

    String json = writer.writeToString(event);

    assertTrue(json.contains("[REDACTED]"));
    assertFalse(json.contains("user@example.com"));
  }

  @Test
  void shouldMaskSensitiveStructuredFieldNames() {

    MockEnvironment environment = new MockEnvironment().withProperty(PREFIX + ".enabled", "true");

    var customizer = new SensitiveDataLoggingJsonMembersCustomizer(environment);

    JsonWriter<ILoggingEvent> writer =
        JsonWriter.of(
            members -> {
              members.add("password", ILoggingEvent::getFormattedMessage);
              customizer.customize(members);
            });

    ILoggingEvent event = mock(ILoggingEvent.class);

    when(event.getFormattedMessage()).thenReturn("my-secret-password");

    String json = writer.writeToString(event);

    assertFalse(json.contains("my-secret-password"));
    assertTrue(json.contains("[REDACTED]"));
  }

  @Test
  void shouldPreserveValuesWhenMaskingIsDisabled() {

    MockEnvironment environment = new MockEnvironment().withProperty(PREFIX + ".enabled", "false");

    var customizer = new SensitiveDataLoggingJsonMembersCustomizer(environment);

    JsonWriter<ILoggingEvent> writer =
        JsonWriter.of(
            members -> {
              members.add("message", ILoggingEvent::getFormattedMessage);
              customizer.customize(members);
            });

    ILoggingEvent event = mock(ILoggingEvent.class);

    when(event.getFormattedMessage()).thenReturn("Contact user@example.com");

    String json = writer.writeToString(event);

    assertTrue(json.contains("user@example.com"));
  }
}
