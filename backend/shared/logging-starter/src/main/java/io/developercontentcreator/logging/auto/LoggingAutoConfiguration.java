package io.developercontentcreator.logging.auto;

import io.developercontentcreator.logging.filter.CorrelationIdFilter;
import io.developercontentcreator.logging.properties.LoggingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration
@ConditionalOnClass(OncePerRequestFilter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class LoggingAutoConfiguration {

  @Bean
  @ConditionalOnProperty(
      prefix = "developer-content-creator.logging.correlation",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public OncePerRequestFilter correlationIdFilter(LoggingProperties properties) {
    return new CorrelationIdFilter(properties);
  }
}
