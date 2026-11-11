package io.developercontentcreator.messaging.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;

class CorrelationIdHeadersTest {

  @Test
  void shouldExposeHeaderNameAndMaximumLength() {
    assertThat(CorrelationIdHeaders.HEADER_NAME).isEqualTo("X-Correlation-ID");
    assertThat(CorrelationIdHeaders.MAX_LENGTH).isEqualTo(128);
  }

  @Test
  void shouldAcceptValidCorrelationId() {
    assertThat(CorrelationIdHeaders.isValid("abc-123.def:456_789")).isTrue();
  }

  @Test
  void shouldAcceptCorrelationIdAtMaximumLength() {
    assertThat(CorrelationIdHeaders.isValid("a".repeat(128))).isTrue();
  }

  @Test
  void shouldRejectNullCorrelationId() {
    assertThat(CorrelationIdHeaders.isValid(null)).isFalse();
  }

  @Test
  void shouldRejectBlankCorrelationId() {
    assertThat(CorrelationIdHeaders.isValid("   ")).isFalse();
  }

  @Test
  void shouldRejectCorrelationIdExceedingMaximumLength() {
    assertThat(CorrelationIdHeaders.isValid("a".repeat(129))).isFalse();
  }

  @Test
  void shouldRejectCorrelationIdContainingUnsupportedCharacters() {
    assertThat(CorrelationIdHeaders.isValid("abc/123")).isFalse();
    assertThat(CorrelationIdHeaders.isValid("abc 123")).isFalse();
  }

  @Test
  void shouldReadValidHeader() {
    var header =
        new RecordHeader(
            CorrelationIdHeaders.HEADER_NAME, "correlation-123".getBytes(StandardCharsets.UTF_8));

    assertThat(CorrelationIdHeaders.read(header)).isEqualTo("correlation-123");
  }

  @Test
  void shouldReturnNullWhenHeaderIsNull() {
    assertThat(CorrelationIdHeaders.read(null)).isNull();
  }

  @Test
  void shouldReturnNullWhenHeaderValueIsNull() {
    var header = new RecordHeader(CorrelationIdHeaders.HEADER_NAME, null);

    assertThat(CorrelationIdHeaders.read(header)).isNull();
  }

  @Test
  void shouldRejectInvalidHeaderValue() {
    var header =
        new RecordHeader(
            CorrelationIdHeaders.HEADER_NAME, "invalid/value".getBytes(StandardCharsets.UTF_8));

    assertThat(CorrelationIdHeaders.read(header)).isNull();
  }
}
