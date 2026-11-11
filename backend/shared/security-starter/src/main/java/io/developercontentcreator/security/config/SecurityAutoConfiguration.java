package io.developercontentcreator.security.config;

import io.developercontentcreator.security.authentication.CurrentUserProvider;
import io.developercontentcreator.security.authentication.SpringCurrentUserProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "developer-content-creator.security",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(CurrentUserProvider.class)
  public CurrentUserProvider currentUserProvider(SecurityProperties properties) {
    return new SpringCurrentUserProvider(properties);
  }
}
