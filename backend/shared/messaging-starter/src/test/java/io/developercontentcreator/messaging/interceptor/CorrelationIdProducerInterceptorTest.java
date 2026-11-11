package io.developercontentcreator.messaging.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.logging.context.CorrelationContext;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CorrelationIdProducerInterceptorTest {

  private final CorrelationIdProducerInterceptor<String, String> interceptor =
      new CorrelationIdProducerInterceptor<>();

  @AfterEach
  void tearDown() {
    CorrelationContext.clear();
  }

  @Test
  void shouldAddCorrelationIdFromMdcToKafkaRecord() {
    CorrelationContext.setCorrelationId("correlation-123");

    ProducerRecord<String, String> record = createRecord();

    var result = interceptor.onSend(record);

    assertThat(result.headers().lastHeader(CorrelationIdHeaders.HEADER_NAME)).isNotNull();

    assertThat(
            new String(
                result.headers().lastHeader(CorrelationIdHeaders.HEADER_NAME).value(),
                StandardCharsets.UTF_8))
        .isEqualTo("correlation-123");
  }

  @Test
  void shouldNotAddHeaderWhenMdcIsEmpty() {
    CorrelationContext.clear();

    ProducerRecord<String, String> record = createRecord();

    interceptor.onSend(record);

    assertThat(record.headers().lastHeader(CorrelationIdHeaders.HEADER_NAME)).isNull();
  }

  @Test
  void shouldNotAddHeaderWhenMdcContainsInvalidCorrelationId() {
    CorrelationContext.setCorrelationId("invalid/value");

    ProducerRecord<String, String> record = createRecord();

    interceptor.onSend(record);

    assertThat(record.headers().lastHeader(CorrelationIdHeaders.HEADER_NAME)).isNull();
  }

  @Test
  void shouldNotOverwriteExistingCorrelationIdHeader() {
    CorrelationContext.setCorrelationId("mdc-correlation");

    ProducerRecord<String, String> record = createRecord();

    record
        .headers()
        .add(
            new RecordHeader(
                CorrelationIdHeaders.HEADER_NAME,
                "explicit-correlation".getBytes(StandardCharsets.UTF_8)));

    interceptor.onSend(record);

    assertThat(
            new String(
                record.headers().lastHeader(CorrelationIdHeaders.HEADER_NAME).value(),
                StandardCharsets.UTF_8))
        .isEqualTo("explicit-correlation");
  }

  @Test
  void shouldSupportKafkaInterceptorLifecycleMethods() {
    interceptor.configure(Map.of());
    interceptor.onAcknowledgement(null, null);
    interceptor.close();
  }

  private ProducerRecord<String, String> createRecord() {
    return new ProducerRecord<>("repository-analyzed", "key", "value");
  }
}
