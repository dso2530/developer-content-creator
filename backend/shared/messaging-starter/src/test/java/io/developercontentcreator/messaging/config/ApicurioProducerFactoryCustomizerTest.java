package io.developercontentcreator.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.apicurio.registry.resolver.config.SchemaResolverConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

class ApicurioProducerFactoryCustomizerTest {

  private final MessagingProperties properties = new MessagingProperties();

  private final ApicurioProducerFactoryCustomizer customizer =
      new ApicurioProducerFactoryCustomizer(properties);

  @Test
  void shouldApplyApicurioDefaultsToProducerFactory() {
    DefaultKafkaProducerFactory<String, Object> factory =
        new DefaultKafkaProducerFactory<>(new HashMap<>());

    customizer.customize(factory);

    Map<String, Object> configuration = factory.getConfigurationProperties();

    assertThat(configuration)
        .containsEntry(SchemaResolverConfig.REGISTRY_URL, "http://localhost:8080/apis/registry/v3")
        .containsEntry(SchemaResolverConfig.AUTO_REGISTER_ARTIFACT, false)
        .containsEntry(SchemaResolverConfig.FIND_LATEST_ARTIFACT, false)
        .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class);
  }

  @Test
  void shouldPreserveExplicitProducerConfiguration() {
    Map<String, Object> initialConfiguration = new HashMap<>();
    initialConfiguration.put(
        SchemaResolverConfig.REGISTRY_URL, "http://custom-registry:8080/apis/registry/v3");
    initialConfiguration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, String.class);

    DefaultKafkaProducerFactory<String, Object> factory =
        new DefaultKafkaProducerFactory<>(initialConfiguration);

    customizer.customize(factory);

    Map<String, Object> configuration = factory.getConfigurationProperties();

    assertThat(configuration)
        .containsEntry(
            SchemaResolverConfig.REGISTRY_URL, "http://custom-registry:8080/apis/registry/v3")
        .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, String.class);
  }
}
