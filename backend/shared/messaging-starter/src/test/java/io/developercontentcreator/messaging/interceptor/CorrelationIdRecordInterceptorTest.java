package io.developercontentcreator.messaging.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.logging.context.CorrelationContext;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CorrelationIdRecordInterceptorTest {

  private final CorrelationIdRecordInterceptor interceptor = new CorrelationIdRecordInterceptor();

  @AfterEach
  void tearDown() {
    interceptor.clearThreadState(null);
    CorrelationContext.clear();
  }

  @Test
  void shouldSetMdcFromKafkaHeader() {
    var record = createRecord("correlation-123");

    var result = interceptor.intercept(record, null);

    assertThat(result).isSameAs(record);
    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("correlation-123");

    interceptor.afterRecord(record, null);

    assertThat(CorrelationContext.getCorrelationId()).isNull();
  }

  @Test
  void shouldRestorePreviousMdcAfterRecordProcessing() {
    CorrelationContext.setCorrelationId("previous-correlation");

    var record = createRecord("kafka-correlation");

    interceptor.intercept(record, null);

    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("kafka-correlation");

    interceptor.afterRecord(record, null);

    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("previous-correlation");
  }

  @Test
  void shouldClearMdcWhenKafkaHeaderIsMissing() {
    CorrelationContext.setCorrelationId("previous-correlation");

    var record = createRecord(null);

    interceptor.intercept(record, null);

    assertThat(CorrelationContext.getCorrelationId()).isNull();

    interceptor.afterRecord(record, null);

    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("previous-correlation");
  }

  @Test
  void shouldClearMdcWhenKafkaHeaderIsInvalid() {
    CorrelationContext.setCorrelationId("previous-correlation");

    var record = createRecord("invalid/value");

    interceptor.intercept(record, null);

    assertThat(CorrelationContext.getCorrelationId()).isNull();

    interceptor.afterRecord(record, null);

    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("previous-correlation");
  }

  @Test
  void shouldRestorePreviousMdcWhenThreadStateIsCleared() {
    CorrelationContext.setCorrelationId("previous-correlation");

    var record = createRecord("kafka-correlation");

    interceptor.intercept(record, null);

    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("kafka-correlation");

    interceptor.clearThreadState(null);

    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("previous-correlation");
  }

  @Test
  void shouldHandleAfterRecordWithoutPreviousIntercept() {
    CorrelationContext.setCorrelationId("existing-correlation");

    interceptor.afterRecord(createRecord(null), null);

    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("existing-correlation");
  }

  @Test
  void shouldHandleMultipleRecordsSequentially() {
    var firstRecord = createRecord("correlation-1");
    var secondRecord = createRecord("correlation-2");

    interceptor.intercept(firstRecord, null);

    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("correlation-1");

    interceptor.afterRecord(firstRecord, null);

    assertThat(CorrelationContext.getCorrelationId()).isNull();

    interceptor.intercept(secondRecord, null);

    assertThat(CorrelationContext.getCorrelationId()).isEqualTo("correlation-2");

    interceptor.afterRecord(secondRecord, null);

    assertThat(CorrelationContext.getCorrelationId()).isNull();
  }

  private ConsumerRecord<Object, Object> createRecord(String correlationId) {
    ConsumerRecord<Object, Object> record =
        new ConsumerRecord<>("repository-analyzed", 0, 0L, "key", "value");

    if (correlationId != null) {
      record
          .headers()
          .add(
              new RecordHeader(
                  CorrelationIdHeaders.HEADER_NAME,
                  correlationId.getBytes(StandardCharsets.UTF_8)));
    }

    return record;
  }
}
