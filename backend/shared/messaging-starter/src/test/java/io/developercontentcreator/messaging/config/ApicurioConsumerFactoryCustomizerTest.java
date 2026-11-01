package io.developercontentcreator.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.apicurio.registry.resolver.config.SchemaResolverConfig;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroSerdeConfig;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

class ApicurioConsumerFactoryCustomizerTest {

  private final MessagingProperties properties = new MessagingProperties();

  private final ApicurioConsumerFactoryCustomizer customizer =
      new ApicurioConsumerFactoryCustomizer(properties);

  @Test
  void shouldApplyApicurioDefaultsToConsumerFactory() {
    DefaultKafkaConsumerFactory<String, Object> factory =
        new DefaultKafkaConsumerFactory<>(new HashMap<>());

    customizer.customize(factory);

    Map<String, Object> configuration = factory.getConfigurationProperties();

    assertThat(configuration)
        .containsEntry(SchemaResolverConfig.REGISTRY_URL, "http://localhost:8080/apis/registry/v3")
        .containsEntry(SchemaResolverConfig.AUTO_REGISTER_ARTIFACT, false)
        .containsEntry(SchemaResolverConfig.FIND_LATEST_ARTIFACT, false)
        .containsEntry(AvroSerdeConfig.USE_SPECIFIC_AVRO_READER, true)
        .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class);
  }

  @Test
  void shouldPreserveExplicitConsumerConfiguration() {
    Map<String, Object> initialConfiguration = new HashMap<>();
    initialConfiguration.put(
        SchemaResolverConfig.REGISTRY_URL, "http://custom-registry:8080/apis/registry/v3");
    initialConfiguration.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, String.class);
    initialConfiguration.put(AvroSerdeConfig.USE_SPECIFIC_AVRO_READER, false);

    DefaultKafkaConsumerFactory<String, Object> factory =
        new DefaultKafkaConsumerFactory<>(initialConfiguration);

    customizer.customize(factory);

    Map<String, Object> configuration = factory.getConfigurationProperties();

    assertThat(configuration)
        .containsEntry(
            SchemaResolverConfig.REGISTRY_URL, "http://custom-registry:8080/apis/registry/v3")
        .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, String.class)
        .containsEntry(AvroSerdeConfig.USE_SPECIFIC_AVRO_READER, false);
  }
}
