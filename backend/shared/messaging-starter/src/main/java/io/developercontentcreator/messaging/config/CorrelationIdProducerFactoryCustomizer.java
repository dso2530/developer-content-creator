package io.developercontentcreator.messaging.config;

import io.developercontentcreator.messaging.interceptor.CorrelationIdProducerInterceptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

public class CorrelationIdProducerFactoryCustomizer
    implements DefaultKafkaProducerFactoryCustomizer {

  @Override
  public void customize(DefaultKafkaProducerFactory<?, ?> factory) {

    Map<String, Object> existing = factory.getConfigurationProperties();

    Object configured = existing.get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);

    List<String> interceptorClasses = new ArrayList<>();

    if (configured instanceof List<?> configuredList) {
      configuredList.forEach(value -> interceptorClasses.add(toClassName(value)));
    } else if (configured instanceof String configuredString && !configuredString.isBlank()) {
      for (String value : configuredString.split(",")) {
        interceptorClasses.add(value.trim());
      }
    }

    String correlationInterceptor = CorrelationIdProducerInterceptor.class.getName();

    if (!interceptorClasses.contains(correlationInterceptor)) {
      interceptorClasses.add(correlationInterceptor);
    }

    factory.updateConfigs(Map.of(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptorClasses));
  }

  private String toClassName(Object value) {
    if (value instanceof Class<?> type) {
      return type.getName();
    }

    return String.valueOf(value);
  }
}
