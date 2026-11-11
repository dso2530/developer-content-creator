package io.developercontentcreator.messaging.interceptor;

import io.developercontentcreator.logging.context.CorrelationContext;
import java.util.ArrayDeque;
import java.util.Deque;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RecordInterceptor;

public class CorrelationIdRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

  private final ThreadLocal<Deque<CorrelationContext.Scope>> scopes =
      ThreadLocal.withInitial(ArrayDeque::new);

  @Override
  public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {

    String correlationId =
        CorrelationIdHeaders.read(record.headers().lastHeader(CorrelationIdHeaders.HEADER_NAME));

    // Un message sans correlationId doit également isoler son contexte MDC.
    CorrelationContext.Scope scope = CorrelationContext.open(correlationId);

    scopes.get().push(scope);

    return record;
  }

  @Override
  public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {

    Deque<CorrelationContext.Scope> currentScopes = scopes.get();

    if (currentScopes.isEmpty()) {
      scopes.remove();
      return;
    }

    currentScopes.pop().close();

    if (currentScopes.isEmpty()) {
      scopes.remove();
    }
  }

  @Override
  public void clearThreadState(Consumer<?, ?> consumer) {

    Deque<CorrelationContext.Scope> currentScopes = scopes.get();

    try {
      while (!currentScopes.isEmpty()) {
        currentScopes.pop().close();
      }
    } finally {
      scopes.remove();
    }
  }
}
