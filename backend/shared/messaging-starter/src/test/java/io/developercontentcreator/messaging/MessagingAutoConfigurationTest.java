package io.developercontentcreator.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.messaging.config.MessagingProperties;
import io.developercontentcreator.messaging.interceptor.CorrelationIdRecordInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.listener.RecordInterceptor;

class MessagingAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(MessagingAutoConfiguration.class));

  @Test
  void shouldLoadMessagingPropertiesByDefault() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(MessagingProperties.class);
        });
  }

  @Test
  void shouldBindCustomRegistryProperties() {
    contextRunner
        .withPropertyValues(
            "developer-content-creator.messaging.registry.url=http://registry:8080/apis/registry/v3",
            "developer-content-creator.messaging.registry.specific-avro-reader=false",
            "developer-content-creator.messaging.registry.encoding=JSON")
        .run(
            context -> {
              assertThat(context).hasNotFailed();

              MessagingProperties properties = context.getBean(MessagingProperties.class);

              assertThat(properties.getRegistry().getUrl())
                  .isEqualTo("http://registry:8080/apis/registry/v3");

              assertThat(properties.getRegistry().isSpecificAvroReader()).isFalse();

              assertThat(properties.getRegistry().getEncoding()).isEqualTo("JSON");
            });
  }

  @Test
  void shouldDisableMessagingAutoConfiguration() {
    contextRunner
        .withPropertyValues("developer-content-creator.messaging.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(MessagingProperties.class);
            });
  }

  @Test
  void shouldExposeDefaultMessagingProperties() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();

          MessagingProperties properties = context.getBean(MessagingProperties.class);

          assertThat(properties.isEnabled()).isTrue();

          assertThat(properties.getRegistry().getUrl())
              .isEqualTo("http://localhost:8080/apis/registry/v3");

          assertThat(properties.getRegistry().isSpecificAvroReader()).isTrue();

          assertThat(properties.getRegistry().getEncoding()).isEqualTo("BINARY");

          assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(4);

          assertThat(properties.getRetry().getInitialInterval()).isEqualTo(1000);

          assertThat(properties.getRetry().getMultiplier()).isEqualTo(2.0);

          assertThat(properties.getRetry().getMaxInterval()).isEqualTo(4000);

          assertThat(properties.getDeadLetter().isEnabled()).isTrue();

          assertThat(properties.getDeadLetter().getTopicSuffix()).isEqualTo(".DLT");
        });
  }

  @Test
  void shouldBindRetryAndDeadLetterProperties() {
    contextRunner
        .withPropertyValues(
            "developer-content-creator.messaging.retry.max-attempts=5",
            "developer-content-creator.messaging.retry.initial-interval=500",
            "developer-content-creator.messaging.retry.multiplier=3.0",
            "developer-content-creator.messaging.retry.max-interval=8000",
            "developer-content-creator.messaging.dead-letter.enabled=false",
            "developer-content-creator.messaging.dead-letter.topic-suffix=.DEAD")
        .run(
            context -> {
              assertThat(context).hasNotFailed();

              MessagingProperties properties = context.getBean(MessagingProperties.class);

              assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(5);

              assertThat(properties.getRetry().getInitialInterval()).isEqualTo(500);

              assertThat(properties.getRetry().getMultiplier()).isEqualTo(3.0);

              assertThat(properties.getRetry().getMaxInterval()).isEqualTo(8000);

              assertThat(properties.getDeadLetter().isEnabled()).isFalse();

              assertThat(properties.getDeadLetter().getTopicSuffix()).isEqualTo(".DEAD");
            });
  }

  @Test
  void shouldNotRegisterFactoryCustomizersWhenMessagingIsDisabled() {
    contextRunner
        .withPropertyValues("developer-content-creator.messaging.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(DefaultKafkaProducerFactoryCustomizer.class);
              assertThat(context).doesNotHaveBean(DefaultKafkaConsumerFactoryCustomizer.class);
              assertThat(context).doesNotHaveBean(CorrelationIdRecordInterceptor.class);
              assertThat(context).doesNotHaveBean(RecordInterceptor.class);
            });
  }

  @Test
  void shouldRegisterApicurioAndCorrelationCustomizersAlongsideApplicationCustomizers() {
    contextRunner
        .withBean(
            "applicationProducerCustomizer",
            DefaultKafkaProducerFactoryCustomizer.class,
            () -> factory -> {})
        .withBean(
            "applicationConsumerCustomizer",
            DefaultKafkaConsumerFactoryCustomizer.class,
            () -> factory -> {})
        .run(
            context -> {
              assertThat(context).hasNotFailed();

              assertThat(context)
                  .hasBean("apicurioProducerFactoryCustomizer")
                  .hasBean("correlationIdProducerFactoryCustomizer")
                  .hasBean("apicurioConsumerFactoryCustomizer")
                  .hasBean("correlationIdRecordInterceptor");

              assertThat(context.getBeansOfType(DefaultKafkaProducerFactoryCustomizer.class))
                  .hasSize(3);

              assertThat(context.getBeansOfType(DefaultKafkaConsumerFactoryCustomizer.class))
                  .hasSize(2);

              assertThat(context).hasSingleBean(CorrelationIdRecordInterceptor.class);

              assertThat(context).hasSingleBean(RecordInterceptor.class);
            });
  }
}
