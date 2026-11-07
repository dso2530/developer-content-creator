package io.developercontentcreator.messaging;

import io.developercontentcreator.messaging.config.ApicurioConsumerFactoryCustomizer;
import io.developercontentcreator.messaging.config.ApicurioProducerFactoryCustomizer;
import io.developercontentcreator.messaging.config.CorrelationIdProducerFactoryCustomizer;
import io.developercontentcreator.messaging.config.MessagingProperties;
import io.developercontentcreator.messaging.interceptor.CorrelationIdRecordInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration(before = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(
    prefix = "developer-content-creator.messaging",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(ApicurioProducerFactoryCustomizer.class)
  ApicurioProducerFactoryCustomizer apicurioProducerFactoryCustomizer(
      MessagingProperties properties) {
    return new ApicurioProducerFactoryCustomizer(properties);
  }

  @Bean
  @ConditionalOnMissingBean(ApicurioConsumerFactoryCustomizer.class)
  ApicurioConsumerFactoryCustomizer apicurioConsumerFactoryCustomizer(
      MessagingProperties properties) {
    return new ApicurioConsumerFactoryCustomizer(properties);
  }

  @Bean
  @ConditionalOnMissingBean(CorrelationIdProducerFactoryCustomizer.class)
  CorrelationIdProducerFactoryCustomizer correlationIdProducerFactoryCustomizer() {
    return new CorrelationIdProducerFactoryCustomizer();
  }

  @Bean
  @ConditionalOnMissingBean(CorrelationIdRecordInterceptor.class)
  CorrelationIdRecordInterceptor<?, ?> correlationIdRecordInterceptor() {
    return new CorrelationIdRecordInterceptor();
  }
}
