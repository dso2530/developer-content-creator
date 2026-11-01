package io.developercontentcreator.messaging.config;

import io.apicurio.registry.resolver.config.SchemaResolverConfig;
import io.apicurio.registry.serde.avro.AvroSerdeConfig;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

public final class ApicurioKafkaConfiguration {

  private ApicurioKafkaConfiguration() {}

  public static Map<String, Object> producerDefaults(MessagingProperties properties) {
    Map<String, Object> defaults = registryDefaults(properties);

    defaults.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        io.apicurio.registry.serde.avro.AvroKafkaSerializer.class);

    return defaults;
  }

  public static Map<String, Object> consumerDefaults(MessagingProperties properties) {
    Map<String, Object> defaults = registryDefaults(properties);

    defaults.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        io.apicurio.registry.serde.avro.AvroKafkaDeserializer.class);

    defaults.put(
        AvroSerdeConfig.USE_SPECIFIC_AVRO_READER, properties.getRegistry().isSpecificAvroReader());

    return defaults;
  }

  private static Map<String, Object> registryDefaults(MessagingProperties properties) {
    Map<String, Object> defaults = new HashMap<>();

    defaults.put(SchemaResolverConfig.REGISTRY_URL, properties.getRegistry().getUrl());
    defaults.put(SchemaResolverConfig.AUTO_REGISTER_ARTIFACT, false);
    defaults.put(SchemaResolverConfig.FIND_LATEST_ARTIFACT, false);
    defaults.put(AvroSerdeConfig.AVRO_ENCODING, properties.getRegistry().getEncoding());

    return defaults;
  }
}
