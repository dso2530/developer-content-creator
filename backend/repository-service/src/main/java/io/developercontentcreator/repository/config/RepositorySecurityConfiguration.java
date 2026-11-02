package io.developercontentcreator.repository.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class RepositorySecurityConfiguration {

  private static final String JWT_ISSUER_URI =
      "spring.security.oauth2.resourceserver.jwt.issuer-uri";

  private static final String JWT_JWK_SET_URI =
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri";

  private static final String[] PUBLIC_ENDPOINTS = {
    "/api/v1/health", "/actuator/health", "/actuator/health/**", "/actuator/info"
  };

  @Bean
  SecurityFilterChain repositorySecurityFilterChain(HttpSecurity http, Environment environment)
      throws Exception {

    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers(PUBLIC_ENDPOINTS)
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll());

    if (hasJwtConfiguration(environment)) {
      http.authorizeHttpRequests(authorization -> authorization.anyRequest().authenticated());
      http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    } else {
      http.authorizeHttpRequests(authorization -> authorization.anyRequest().denyAll());
    }

    return http.build();
  }

  private boolean hasJwtConfiguration(Environment environment) {
    return hasNonBlankProperty(environment, JWT_ISSUER_URI)
        || hasNonBlankProperty(environment, JWT_JWK_SET_URI);
  }

  private boolean hasNonBlankProperty(Environment environment, String property) {
    String value = environment.getProperty(property);
    return value != null && !value.isBlank();
  }
}
