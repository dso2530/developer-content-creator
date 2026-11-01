package io.developercontentcreator.messaging.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.contracts.common.EventMetadata;
import io.developercontentcreator.contracts.content.ContentGenerated;
import io.developercontentcreator.messaging.config.ApicurioConsumerFactoryCustomizer;
import io.developercontentcreator.messaging.config.ApicurioProducerFactoryCustomizer;
import io.developercontentcreator.messaging.config.MessagingProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ApicurioKafkaIntegrationTest {

  private static final String GROUP_ID = "developer-content-creator";
  private static final String ARTIFACT_ID = "content-generated";
  private static final String ARTIFACT_VERSION = "1";
  private static final String TOPIC = "content-generated";

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.2"));

  @Container
  static final GenericContainer<?> REGISTRY =
      new GenericContainer<>(DockerImageName.parse("apicurio/apicurio-registry:3.0.7"))
          .withExposedPorts(8080)
          .waitingFor(Wait.forHttp("/apis/registry/v3/system/info").forStatusCode(200));

  private static String registryUrl;

  @BeforeAll
  static void registerSchema() throws Exception {
    registryUrl =
        "http://" + REGISTRY.getHost() + ":" + REGISTRY.getMappedPort(8080) + "/apis/registry/v3";

    String schema = ContentGenerated.getClassSchema().toString();

    String escapedSchema =
        schema
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");

    String body =
        """
        {
          "artifactId": "%s",
          "artifactType": "AVRO",
          "firstVersion": {
            "content": {
              "content": "%s",
              "contentType": "application/json"
            }
          }
        }
        """
            .formatted(ARTIFACT_ID, escapedSchema);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(registryUrl + "/groups/" + GROUP_ID + "/artifacts"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode())
        .as("Apicurio schema registration response: %s", response.body())
        .isBetween(200, 299);
  }

  @Test
  void shouldPublishAndConsumeAvroEventUsingApicurioRegistry() throws Exception {

    MessagingProperties properties = new MessagingProperties();
    properties.getRegistry().setUrl(registryUrl);

    Map<String, Object> producerProperties = new HashMap<>();
    producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");

    // Explicit artifact reference: the producer must use the
    // already registered schema instead of automatically registering it.
    producerProperties.put("apicurio.registry.artifact.group-id", GROUP_ID);
    producerProperties.put("apicurio.registry.artifact.artifact-id", ARTIFACT_ID);
    producerProperties.put("apicurio.registry.artifact.version", ARTIFACT_VERSION);

    DefaultKafkaProducerFactory<String, ContentGenerated> producerFactory =
        new DefaultKafkaProducerFactory<>(producerProperties);

    new ApicurioProducerFactoryCustomizer(properties).customize(producerFactory);

    Map<String, Object> consumerProperties = new HashMap<>();
    consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
    consumerProperties.put(
        ConsumerConfig.GROUP_ID_CONFIG, "apicurio-integration-" + UUID.randomUUID());
    consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    DefaultKafkaConsumerFactory<String, ContentGenerated> consumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProperties);

    new ApicurioConsumerFactoryCustomizer(properties).customize(consumerFactory);

    Instant occurredAt = Instant.parse("2026-11-01T10:00:00Z");
    Instant generatedAt = Instant.parse("2026-11-01T10:05:00Z");

    EventMetadata metadata =
        EventMetadata.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setEventType("content.generated")
            .setOccurredAt(occurredAt)
            .setProducer("content-service")
            .setSubject("repository:repo-123")
            .setCorrelationId("correlation-123")
            .setCausationId("causation-123")
            .build();

    ContentGenerated event =
        ContentGenerated.newBuilder()
            .setMetadata(metadata)
            .setContentId("content-123")
            .setRepositoryId("repo-123")
            .setContentType("ARTICLE")
            .setLanguage("fr")
            .setStatus("GENERATED")
            .setGeneratedAt(generatedAt)
            .build();

    Map<String, Object> adminProperties =
        Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

    try (AdminClient adminClient = AdminClient.create(adminProperties)) {
      NewTopic topic = new NewTopic(TOPIC, 1, (short) 1);

      adminClient.createTopics(List.of(topic)).all().get(10, TimeUnit.SECONDS);
    }

    try {
      KafkaTemplate<String, ContentGenerated> kafkaTemplate = new KafkaTemplate<>(producerFactory);

      try (Consumer<String, ContentGenerated> consumer = consumerFactory.createConsumer()) {

        consumer.subscribe(List.of(TOPIC));

        // Trigger consumer group assignment before publishing.
        long assignmentDeadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();

        while (consumer.assignment().isEmpty() && System.nanoTime() < assignmentDeadline) {
          consumer.poll(Duration.ofMillis(100));
        }

        assertThat(consumer.assignment()).as("Kafka consumer partition assignment").isNotEmpty();

        kafkaTemplate.send(TOPIC, event.getContentId().toString(), event).get(10, TimeUnit.SECONDS);

        var records = consumer.poll(Duration.ofSeconds(10));

        assertThat(records.count()).as("Number of consumed records").isPositive();

        var record = records.iterator().next();

        assertThat(record.key()).isEqualTo("content-123");

        ContentGenerated received = record.value();

        assertThat(received).isNotNull();
        assertThat(received.getContentId()).isEqualTo("content-123");
        assertThat(received.getRepositoryId()).isEqualTo("repo-123");
        assertThat(received.getContentType()).isEqualTo("ARTICLE");
        assertThat(received.getLanguage()).isEqualTo("fr");
        assertThat(received.getStatus()).isEqualTo("GENERATED");
        assertThat(received.getGeneratedAt()).isEqualTo(generatedAt);

        assertThat(received.getMetadata().getEventType()).isEqualTo("content.generated");
        assertThat(received.getMetadata().getProducer()).isEqualTo("content-service");
        assertThat(received.getMetadata().getCorrelationId()).isEqualTo("correlation-123");
        assertThat(received.getMetadata().getCausationId()).isEqualTo("causation-123");
        assertThat(received.getMetadata().getOccurredAt()).isEqualTo(occurredAt);
      }
    } finally {
      producerFactory.destroy();
    }
  }
}
