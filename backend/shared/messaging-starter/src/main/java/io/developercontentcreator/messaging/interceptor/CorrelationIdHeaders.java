package io.developercontentcreator.messaging.interceptor;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.apache.kafka.common.header.Header;

public final class CorrelationIdHeaders {

  public static final String HEADER_NAME = "X-Correlation-ID";
  public static final int MAX_LENGTH = 128;

  private static final Pattern VALIDATION_PATTERN = Pattern.compile("^[a-zA-Z0-9._:-]+$");

  private CorrelationIdHeaders() {}

  public static boolean isValid(String correlationId) {
    return correlationId != null
        && !correlationId.isBlank()
        && correlationId.length() <= MAX_LENGTH
        && VALIDATION_PATTERN.matcher(correlationId).matches();
  }

  public static String read(Header header) {
    if (header == null || header.value() == null) {
      return null;
    }

    String value = new String(header.value(), StandardCharsets.UTF_8);

    return isValid(value) ? value : null;
  }
}
