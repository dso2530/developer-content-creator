package io.developercontentcreator.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.apicurio.registry.resolver.config.SchemaResolverConfig;
import io.apicurio.registry.serde.avro.AvroSerdeConfig;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;

class ApicurioKafkaConfigurationTest {

  private final MessagingProperties properties = new MessagingProperties();

  @Test
  void shouldExposeDefaultProducerConfiguration() {
    Map<String, Object> configuration = ApicurioKafkaConfiguration.producerDefaults(properties);

    assertThat(configuration)
        .containsEntry(SchemaResolverConfig.REGISTRY_URL, "http://localhost:8080/apis/registry/v3")
        .containsEntry(SchemaResolverConfig.AUTO_REGISTER_ARTIFACT, false)
        .containsEntry(SchemaResolverConfig.FIND_LATEST_ARTIFACT, false)
        .containsEntry(AvroSerdeConfig.AVRO_ENCODING, "BINARY")
        .containsEntry(SchemaResolverConfig.EXPLICIT_ARTIFACT_GROUP_ID, "developer-content-creator")
        .containsEntry(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            io.apicurio.registry.serde.avro.AvroKafkaSerializer.class);
  }

  @Test
  void shouldExposeDefaultConsumerConfiguration() {
    Map<String, Object> configuration = ApicurioKafkaConfiguration.consumerDefaults(properties);

    assertThat(configuration)
        .containsEntry(SchemaResolverConfig.REGISTRY_URL, "http://localhost:8080/apis/registry/v3")
        .containsEntry(SchemaResolverConfig.AUTO_REGISTER_ARTIFACT, false)
        .containsEntry(SchemaResolverConfig.FIND_LATEST_ARTIFACT, false)
        .containsEntry(AvroSerdeConfig.AVRO_ENCODING, "BINARY")
        .containsEntry(AvroSerdeConfig.USE_SPECIFIC_AVRO_READER, true)
        .containsEntry(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            io.apicurio.registry.serde.avro.AvroKafkaDeserializer.class)
        .doesNotContainKey(SchemaResolverConfig.EXPLICIT_ARTIFACT_GROUP_ID);
  }

  @Test
  void shouldUseCustomRegistryPropertiesForProducer() {
    properties.getRegistry().setUrl("http://registry:8080/apis/registry/v3");
    properties.getRegistry().setEncoding("JSON");

    Map<String, Object> configuration = ApicurioKafkaConfiguration.producerDefaults(properties);

    assertThat(configuration)
        .containsEntry(SchemaResolverConfig.REGISTRY_URL, "http://registry:8080/apis/registry/v3")
        .containsEntry(AvroSerdeConfig.AVRO_ENCODING, "JSON");
  }

  @Test
  void shouldUseCustomRegistryPropertiesForConsumer() {
    properties.getRegistry().setUrl("http://registry:8080/apis/registry/v3");
    properties.getRegistry().setEncoding("JSON");
    properties.getRegistry().setSpecificAvroReader(false);

    Map<String, Object> configuration = ApicurioKafkaConfiguration.consumerDefaults(properties);

    assertThat(configuration)
        .containsEntry(SchemaResolverConfig.REGISTRY_URL, "http://registry:8080/apis/registry/v3")
        .containsEntry(AvroSerdeConfig.AVRO_ENCODING, "JSON")
        .containsEntry(AvroSerdeConfig.USE_SPECIFIC_AVRO_READER, false);
  }

  @Test
  void shouldUseCustomRegistryGroupIdForProducer() {
    properties.getRegistry().setGroupId("custom-registry-group");

    Map<String, Object> configuration = ApicurioKafkaConfiguration.producerDefaults(properties);

    assertThat(configuration)
        .containsEntry(SchemaResolverConfig.EXPLICIT_ARTIFACT_GROUP_ID, "custom-registry-group");
  }
}
