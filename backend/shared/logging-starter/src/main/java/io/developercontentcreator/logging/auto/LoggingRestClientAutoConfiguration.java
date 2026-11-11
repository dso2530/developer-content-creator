package io.developercontentcreator.logging.auto;

import io.developercontentcreator.logging.http.CorrelationIdClientHttpRequestInterceptor;
import io.developercontentcreator.logging.http.CorrelationIdRestClientCustomizer;
import io.developercontentcreator.logging.properties.LoggingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@ConditionalOnClass({RestClient.class, RestClientCustomizer.class})
@ConditionalOnProperty(
    prefix = "developer-content-creator.logging.correlation",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingRestClientAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(CorrelationIdClientHttpRequestInterceptor.class)
  public CorrelationIdClientHttpRequestInterceptor correlationIdClientHttpRequestInterceptor(
      LoggingProperties properties) {
    return new CorrelationIdClientHttpRequestInterceptor(properties);
  }

  @Bean
  @ConditionalOnMissingBean(CorrelationIdRestClientCustomizer.class)
  public CorrelationIdRestClientCustomizer correlationIdRestClientCustomizer(
      CorrelationIdClientHttpRequestInterceptor interceptor) {
    return new CorrelationIdRestClientCustomizer(interceptor);
  }
}
