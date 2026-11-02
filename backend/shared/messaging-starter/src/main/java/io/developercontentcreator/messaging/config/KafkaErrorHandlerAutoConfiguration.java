package io.developercontentcreator.messaging.config;

import java.time.Duration;
import java.util.function.BiFunction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = "developer-content-creator.messaging",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class KafkaErrorHandlerAutoConfiguration {

  @Bean
  @ConditionalOnProperty(
      prefix = "developer-content-creator.messaging.dead-letter",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  @ConditionalOnMissingBean(CommonErrorHandler.class)
  public CommonErrorHandler kafkaErrorHandler(
      KafkaTemplate<?, ?> kafkaTemplate, MessagingProperties properties) {
    var deadLetterProperties = properties.getDeadLetter();
    var retryProperties = properties.getRetry();

    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate, deadLetterDestination(deadLetterProperties.getTopicSuffix()));

    recoverer.setFailIfSendResultIsError(true);
    recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(10));

    return new DefaultErrorHandler(recoverer, createRetryBackOff(retryProperties));
  }

  static BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> deadLetterDestination(
      String topicSuffix) {

    return (record, exception) ->
        new TopicPartition(record.topic() + topicSuffix, record.partition());
  }

  static BackOff createRetryBackOff(MessagingProperties.Retry retryProperties) {

    if (!retryProperties.isEnabled()) {
      return new FixedBackOff(0L, 0L);
    }

    ExponentialBackOffWithMaxRetries backOff =
        new ExponentialBackOffWithMaxRetries(retryProperties.getMaxAttempts() - 1);

    backOff.setInitialInterval(retryProperties.getInitialInterval());
    backOff.setMultiplier(retryProperties.getMultiplier());
    backOff.setMaxInterval(retryProperties.getMaxInterval());

    return backOff;
  }
}
