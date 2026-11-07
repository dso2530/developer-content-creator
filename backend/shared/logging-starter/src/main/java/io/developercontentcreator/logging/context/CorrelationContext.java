package io.developercontentcreator.logging.context;

import org.slf4j.MDC;

public final class CorrelationContext {

  public static final String DEFAULT_MDC_KEY = "correlationId";

  private CorrelationContext() {}

  public static String getCorrelationId() {
    return MDC.get(DEFAULT_MDC_KEY);
  }

  public static void setCorrelationId(String correlationId) {
    if (correlationId == null || correlationId.isBlank()) {
      MDC.remove(DEFAULT_MDC_KEY);
      return;
    }

    MDC.put(DEFAULT_MDC_KEY, correlationId);
  }

  public static void clear() {
    MDC.remove(DEFAULT_MDC_KEY);
  }

  public static Scope open(String correlationId) {
    String previous = MDC.get(DEFAULT_MDC_KEY);

    setCorrelationId(correlationId);

    return () -> {
      if (previous == null) {
        clear();
      } else {
        setCorrelationId(previous);
      }
    };
  }

  @FunctionalInterface
  public interface Scope extends AutoCloseable {
    @Override
    void close();
  }
}
