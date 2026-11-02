package io.developercontentcreator.messaging.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

public class ApicurioConsumerFactoryCustomizer implements DefaultKafkaConsumerFactoryCustomizer {

  private final MessagingProperties properties;

  public ApicurioConsumerFactoryCustomizer(MessagingProperties properties) {
    this.properties = properties;
  }

  @Override
  public void customize(DefaultKafkaConsumerFactory<?, ?> factory) {
    Map<String, Object> existing = factory.getConfigurationProperties();
    Map<String, Object> additions = new HashMap<>();

    ApicurioKafkaConfiguration.consumerDefaults(properties)
        .forEach(
            (key, value) -> {
              if (!existing.containsKey(key)) {
                if (!ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG.equals(key)
                    || factory.getValueDeserializer() == null) {
                  additions.put(key, value);
                }
              }
            });

    if (!additions.isEmpty()) {
      factory.updateConfigs(additions);
    }
  }
}
