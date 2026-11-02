package io.developercontentcreator.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.developercontentcreator.messaging.MessagingAutoConfiguration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

class KafkaErrorHandlerAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  MessagingAutoConfiguration.class, KafkaErrorHandlerAutoConfiguration.class))
          .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class));

  @Test
  void shouldRegisterDefaultErrorHandlerWhenDeadLetterIsEnabled() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(CommonErrorHandler.class);
          assertThat(context.getBean(CommonErrorHandler.class))
              .isInstanceOf(DefaultErrorHandler.class);
        });
  }

  @Test
  void shouldNotRegisterErrorHandlerWhenMessagingIsDisabled() {
    contextRunner
        .withPropertyValues("developer-content-creator.messaging.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(CommonErrorHandler.class);
            });
  }

  @Test
  void shouldNotRegisterErrorHandlerWhenDeadLetterIsDisabled() {
    contextRunner
        .withPropertyValues("developer-content-creator.messaging.dead-letter.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(CommonErrorHandler.class);
            });
  }

  @Test
  void shouldKeepApplicationErrorHandlerWhenOneIsProvided() {
    CommonErrorHandler applicationHandler = mock(CommonErrorHandler.class);

    contextRunner
        .withBean("applicationErrorHandler", CommonErrorHandler.class, () -> applicationHandler)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(CommonErrorHandler.class);
              assertThat(context.getBean(CommonErrorHandler.class)).isSameAs(applicationHandler);
            });
  }

  @Test
  void shouldConfigureFourTotalAttemptsWithExponentialBackoff() {
    ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);

    backOff.setInitialInterval(1000);
    backOff.setMultiplier(2.0);
    backOff.setMaxInterval(4000);

    var execution = backOff.start();

    assertThat(execution.nextBackOff()).isEqualTo(1000);
    assertThat(execution.nextBackOff()).isEqualTo(2000);
    assertThat(execution.nextBackOff()).isEqualTo(4000);
    assertThat(execution.nextBackOff()).isEqualTo(-1);
  }

  @Test
  void shouldUseConfiguredRetryProperties() {
    contextRunner
        .withPropertyValues(
            "developer-content-creator.messaging.retry.max-attempts=5",
            "developer-content-creator.messaging.retry.initial-interval=500",
            "developer-content-creator.messaging.retry.multiplier=3.0",
            "developer-content-creator.messaging.retry.max-interval=8000")
        .run(
            context -> {
              assertThat(context).hasNotFailed();

              MessagingProperties properties = context.getBean(MessagingProperties.class);

              assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(5);
              assertThat(properties.getRetry().getInitialInterval()).isEqualTo(500);
              assertThat(properties.getRetry().getMultiplier()).isEqualTo(3.0);
              assertThat(properties.getRetry().getMaxInterval()).isEqualTo(8000);
            });
  }

  @Test
  void shouldRouteFailedRecordToDeadLetterTopicWithSamePartition() {
    ConsumerRecord<String, String> record =
        new ConsumerRecord<>("content.generated", 2, 42L, "key", "value");

    TopicPartition destination =
        KafkaErrorHandlerAutoConfiguration.deadLetterDestination(".DLT")
            .apply(record, new RuntimeException("Processing failed"));

    assertThat(destination.topic()).isEqualTo("content.generated.DLT");
    assertThat(destination.partition()).isEqualTo(2);
  }

  @Test
  void shouldSupportCustomDeadLetterTopicSuffix() {
    ConsumerRecord<String, String> record =
        new ConsumerRecord<>("content.generated", 1, 10L, "key", "value");

    TopicPartition destination =
        KafkaErrorHandlerAutoConfiguration.deadLetterDestination(".DEAD")
            .apply(record, new RuntimeException("Processing failed"));

    assertThat(destination.topic()).isEqualTo("content.generated.DEAD");
    assertThat(destination.partition()).isEqualTo(1);
  }

  @Test
  void shouldDisableRetryWhenRetryIsDisabled() {
    MessagingProperties.Retry retryProperties = new MessagingProperties.Retry();
    retryProperties.setEnabled(false);

    var backOff = KafkaErrorHandlerAutoConfiguration.createRetryBackOff(retryProperties);

    assertThat(backOff).isInstanceOf(FixedBackOff.class);
    assertThat(backOff.start().nextBackOff()).isEqualTo(BackOffExecution.STOP);
  }

  @Test
  void shouldEnableExponentialRetryByDefault() {
    MessagingProperties.Retry retryProperties = new MessagingProperties.Retry();

    var backOff = KafkaErrorHandlerAutoConfiguration.createRetryBackOff(retryProperties);

    assertThat(backOff).isInstanceOf(ExponentialBackOffWithMaxRetries.class);

    var execution = backOff.start();

    assertThat(execution.nextBackOff()).isEqualTo(1000);
    assertThat(execution.nextBackOff()).isEqualTo(2000);
    assertThat(execution.nextBackOff()).isEqualTo(4000);
    assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
  }

  @Test
  void shouldKeepDeadLetterHandlerWhenRetryIsDisabled() {
    contextRunner
        .withPropertyValues("developer-content-creator.messaging.retry.enabled=false")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(CommonErrorHandler.class);
              assertThat(context.getBean(CommonErrorHandler.class))
                  .isInstanceOf(DefaultErrorHandler.class);
            });
  }
}
