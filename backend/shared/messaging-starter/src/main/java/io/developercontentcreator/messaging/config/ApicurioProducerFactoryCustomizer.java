package io.developercontentcreator.messaging.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

public class ApicurioProducerFactoryCustomizer implements DefaultKafkaProducerFactoryCustomizer {

  private final MessagingProperties properties;

  public ApicurioProducerFactoryCustomizer(MessagingProperties properties) {
    this.properties = properties;
  }

  @Override
  public void customize(DefaultKafkaProducerFactory<?, ?> factory) {
    Map<String, Object> existing = factory.getConfigurationProperties();
    Map<String, Object> additions = new HashMap<>();

    ApicurioKafkaConfiguration.producerDefaults(properties)
        .forEach(
            (key, value) -> {
              if (!existing.containsKey(key)) {
                additions.put(key, value);
              }
            });

    if (!additions.isEmpty()) {
      factory.updateConfigs(additions);
    }
  }
}
