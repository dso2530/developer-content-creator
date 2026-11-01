package io.developercontentcreator.messaging.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.contracts.common.EventMetadata;
import io.developercontentcreator.contracts.content.ContentGenerated;
import io.developercontentcreator.messaging.config.ApicurioConsumerFactoryCustomizer;
import io.developercontentcreator.messaging.config.ApicurioProducerFactoryCustomizer;
import io.developercontentcreator.messaging.config.MessagingProperties;
import io.developercontentcreator.messaging.contract.MessagingArtifacts;
import io.developercontentcreator.messaging.contract.MessagingRegistryGroups;
import io.developercontentcreator.messaging.contract.MessagingTopics;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ApicurioKafkaIntegrationTest {

  private static final String GROUP_ID = MessagingRegistryGroups.DEFAULT;
  private static final String ARTIFACT_ID = MessagingArtifacts.CONTENT_GENERATED;
  private static final String ARTIFACT_VERSION = "1";
  private static final String TOPIC = MessagingTopics.CONTENT_GENERATED;

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
  void shouldRetryFailedMessageFourTimesBeforePublishingToDlt() throws Exception {

    String sourceTopic = TOPIC + ".retry-test-" + UUID.randomUUID();
    String deadLetterTopic = sourceTopic + ".DLT";

    MessagingProperties properties = new MessagingProperties();
    properties.getRegistry().setUrl(registryUrl);

    Map<String, Object> producerProperties = new HashMap<>();
    producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
    producerProperties.put("apicurio.registry.artifact.group-id", GROUP_ID);
    producerProperties.put("apicurio.registry.artifact.artifact-id", ARTIFACT_ID);
    producerProperties.put("apicurio.registry.artifact.version", ARTIFACT_VERSION);

    DefaultKafkaProducerFactory<String, ContentGenerated> producerFactory =
        new DefaultKafkaProducerFactory<>(producerProperties);

    new ApicurioProducerFactoryCustomizer(properties).customize(producerFactory);

    Map<String, Object> consumerProperties = new HashMap<>();
    consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
    consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "retry-listener-" + UUID.randomUUID());
    consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    DefaultKafkaConsumerFactory<String, ContentGenerated> listenerConsumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProperties);

    new ApicurioConsumerFactoryCustomizer(properties).customize(listenerConsumerFactory);

    Map<String, Object> dltConsumerProperties = new HashMap<>(consumerProperties);
    dltConsumerProperties.put(
        ConsumerConfig.GROUP_ID_CONFIG, "retry-dlt-consumer-" + UUID.randomUUID());

    DefaultKafkaConsumerFactory<String, ContentGenerated> dltConsumerFactory =
        new DefaultKafkaConsumerFactory<>(dltConsumerProperties);

    new ApicurioConsumerFactoryCustomizer(properties).customize(dltConsumerFactory);

    Map<String, Object> adminProperties =
        Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

    try (AdminClient adminClient = AdminClient.create(adminProperties)) {
      adminClient
          .createTopics(
              List.of(
                  new NewTopic(sourceTopic, 1, (short) 1),
                  new NewTopic(deadLetterTopic, 1, (short) 1)))
          .all()
          .get(10, TimeUnit.SECONDS);
    }

    AtomicInteger attempts = new AtomicInteger();

    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            new KafkaTemplate<>(producerFactory),
            (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

    recoverer.setFailIfSendResultIsError(true);
    recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(10));

    ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);

    backOff.setInitialInterval(100);
    backOff.setMultiplier(1.0);
    backOff.setMaxInterval(100);

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    ContainerProperties containerProperties = new ContainerProperties(sourceTopic);
    containerProperties.setAckMode(ContainerProperties.AckMode.RECORD);
    containerProperties.setMessageListener(
        (MessageListener<String, ContentGenerated>)
            record -> {
              attempts.incrementAndGet();
              throw new IllegalStateException("Simulated processing failure");
            });

    KafkaMessageListenerContainer<String, ContentGenerated> container =
        new KafkaMessageListenerContainer<>(listenerConsumerFactory, containerProperties);

    container.setCommonErrorHandler(errorHandler);

    try {
      container.start();

      try (Consumer<String, ContentGenerated> dltConsumer = dltConsumerFactory.createConsumer()) {

        dltConsumer.subscribe(List.of(deadLetterTopic));

        long assignmentDeadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();

        while (container.getAssignedPartitions() == null
            || container.getAssignedPartitions().isEmpty()) {

          if (System.nanoTime() >= assignmentDeadline) {
            throw new AssertionError("Listener container did not receive partition assignment");
          }

          Thread.sleep(50);
        }

        while (dltConsumer.assignment().isEmpty() && System.nanoTime() < assignmentDeadline) {
          dltConsumer.poll(Duration.ofMillis(100));
        }

        assertThat(dltConsumer.assignment()).as("DLT consumer partition assignment").isNotEmpty();

        Instant occurredAt = Instant.parse("2026-11-01T10:00:00Z");
        Instant generatedAt = Instant.parse("2026-11-01T10:05:00Z");

        EventMetadata metadata =
            EventMetadata.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType("content.generated")
                .setOccurredAt(occurredAt)
                .setProducer("content-service")
                .setSubject("repository:retry-test")
                .setCorrelationId("correlation-retry")
                .setCausationId("causation-retry")
                .build();

        ContentGenerated event =
            ContentGenerated.newBuilder()
                .setMetadata(metadata)
                .setContentId("content-retry")
                .setRepositoryId("repo-retry")
                .setContentType("ARTICLE")
                .setLanguage("fr")
                .setStatus("GENERATED")
                .setGeneratedAt(generatedAt)
                .build();

        KafkaTemplate<String, ContentGenerated> kafkaTemplate =
            new KafkaTemplate<>(producerFactory);

        kafkaTemplate
            .send(sourceTopic, event.getContentId().toString(), event)
            .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, ContentGenerated> dltRecord = null;

        long dltDeadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();

        while (dltRecord == null && System.nanoTime() < dltDeadline) {
          var records = dltConsumer.poll(Duration.ofMillis(250));

          if (!records.isEmpty()) {
            dltRecord = records.iterator().next();
          }
        }

        assertThat(dltRecord).as("Message recovered in the DLT").isNotNull();

        assertThat(attempts.get()).as("Initial delivery plus three retries").isEqualTo(4);

        assertThat(dltRecord.topic()).isEqualTo(deadLetterTopic);
        assertThat(dltRecord.partition()).isEqualTo(0);
        assertThat(dltRecord.key()).isEqualTo("content-retry");

        ContentGenerated received = dltRecord.value();

        assertThat(received).isNotNull();
        assertThat(received.getContentId()).isEqualTo("content-retry");
        assertThat(received.getRepositoryId()).isEqualTo("repo-retry");
        assertThat(received.getMetadata().getCorrelationId()).isEqualTo("correlation-retry");
      }
    } finally {
      container.stop();
      producerFactory.destroy();
    }
  }

  @Test
  void shouldPublishAvroEventToDeadLetterTopicUsingApicurioRegistry() throws Exception {

    String sourceTopic = TOPIC + ".dlt-test-" + UUID.randomUUID();
    String deadLetterTopic = sourceTopic + ".DLT";

    MessagingProperties properties = new MessagingProperties();
    properties.getRegistry().setUrl(registryUrl);

    Map<String, Object> producerProperties = new HashMap<>();
    producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
    producerProperties.put("apicurio.registry.artifact.group-id", GROUP_ID);
    producerProperties.put("apicurio.registry.artifact.artifact-id", ARTIFACT_ID);
    producerProperties.put("apicurio.registry.artifact.version", ARTIFACT_VERSION);

    DefaultKafkaProducerFactory<String, ContentGenerated> producerFactory =
        new DefaultKafkaProducerFactory<>(producerProperties);

    new ApicurioProducerFactoryCustomizer(properties).customize(producerFactory);

    Map<String, Object> consumerProperties = new HashMap<>();
    consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
    consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-integration-" + UUID.randomUUID());
    consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    DefaultKafkaConsumerFactory<String, ContentGenerated> consumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProperties);

    new ApicurioConsumerFactoryCustomizer(properties).customize(consumerFactory);

    Map<String, Object> adminProperties =
        Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

    try (AdminClient adminClient = AdminClient.create(adminProperties)) {
      adminClient
          .createTopics(
              List.of(
                  new NewTopic(sourceTopic, 1, (short) 1),
                  new NewTopic(deadLetterTopic, 1, (short) 1)))
          .all()
          .get(10, TimeUnit.SECONDS);
    }

    try {
      KafkaTemplate<String, ContentGenerated> kafkaTemplate = new KafkaTemplate<>(producerFactory);

      Instant occurredAt = Instant.parse("2026-11-01T10:00:00Z");
      Instant generatedAt = Instant.parse("2026-11-01T10:05:00Z");

      EventMetadata metadata =
          EventMetadata.newBuilder()
              .setEventId(UUID.randomUUID().toString())
              .setEventType("content.generated")
              .setOccurredAt(occurredAt)
              .setProducer("content-service")
              .setSubject("repository:repo-dlt")
              .setCorrelationId("correlation-dlt")
              .setCausationId("causation-dlt")
              .build();

      ContentGenerated event =
          ContentGenerated.newBuilder()
              .setMetadata(metadata)
              .setContentId("content-dlt")
              .setRepositoryId("repo-dlt")
              .setContentType("ARTICLE")
              .setLanguage("fr")
              .setStatus("GENERATED")
              .setGeneratedAt(generatedAt)
              .build();

      try (Consumer<String, ContentGenerated> consumer = consumerFactory.createConsumer()) {

        consumer.subscribe(List.of(deadLetterTopic));

        long assignmentDeadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();

        while (consumer.assignment().isEmpty() && System.nanoTime() < assignmentDeadline) {
          consumer.poll(Duration.ofMillis(100));
        }

        assertThat(consumer.assignment()).as("DLT consumer partition assignment").isNotEmpty();

        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                    new TopicPartition(record.topic() + ".DLT", record.partition()));

        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(10));

        ConsumerRecord<String, ContentGenerated> failedRecord =
            new ConsumerRecord<>(sourceTopic, 0, 42L, event.getContentId().toString(), event);
        recoverer.accept(
            failedRecord, null, new IllegalStateException("Simulated processing failure"));

        var records = consumer.poll(Duration.ofSeconds(10));

        assertThat(records.count()).as("Number of records published to the DLT").isPositive();

        var receivedRecord = records.iterator().next();

        assertThat(receivedRecord.topic()).isEqualTo(deadLetterTopic);
        assertThat(receivedRecord.partition()).isEqualTo(0);
        assertThat(receivedRecord.key()).isEqualTo("content-dlt");

        ContentGenerated received = receivedRecord.value();

        assertThat(received).isNotNull();
        assertThat(received.getContentId()).isEqualTo("content-dlt");
        assertThat(received.getRepositoryId()).isEqualTo("repo-dlt");
        assertThat(received.getMetadata().getEventType()).isEqualTo("content.generated");
        assertThat(received.getMetadata().getCorrelationId()).isEqualTo("correlation-dlt");
      }
    } finally {
      producerFactory.destroy();
    }
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
