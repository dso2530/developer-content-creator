package io.developercontentcreator.logging.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoggingPropertiesTest {

  @Test
  void shouldExposeDefaultValues() {
    LoggingProperties properties = new LoggingProperties();

    assertTrue(properties.isEnabled());
    assertEquals("X-Correlation-ID", properties.getHeaderName());
    assertEquals(128, properties.getMaxLength());
  }

  @Test
  void shouldAllowChangingHeaderName() {
    LoggingProperties properties = new LoggingProperties();

    properties.setHeaderName("X-Request-ID");

    assertEquals("X-Request-ID", properties.getHeaderName());
  }

  @Test
  void shouldAllowChangingMaximumLength() {
    LoggingProperties properties = new LoggingProperties();

    properties.setMaxLength(256);

    assertEquals(256, properties.getMaxLength());
  }
}
