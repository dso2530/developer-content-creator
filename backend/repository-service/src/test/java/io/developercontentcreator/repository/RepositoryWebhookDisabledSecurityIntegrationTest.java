package io.developercontentcreator.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test",
      "developer-content-creator.github.webhook.enabled=false"
    })
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(RepositoryWebhookDisabledSecurityIntegrationTest.TestJwtDecoderConfiguration.class)
class RepositoryWebhookDisabledSecurityIntegrationTest {

  private static final String WEBHOOK_ENDPOINT = "/api/webhooks/github";

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void shouldRejectAnonymousWebhookRequestWhenWebhookIsDisabled() {

    ResponseEntity<String> response =
        restTemplate.postForEntity(url(WEBHOOK_ENDPOINT), "{}", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldReturnNotFoundForAuthenticatedWebhookRequestWhenWebhookIsDisabled() {

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("test-token");

    ResponseEntity<String> response =
        restTemplate.exchange(
            url(WEBHOOK_ENDPOINT), HttpMethod.POST, new HttpEntity<>("{}", headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private String url(String endpoint) {
    return "http://localhost:" + port + endpoint;
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestJwtDecoderConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
      return token ->
          Jwt.withTokenValue(token)
              .header("alg", "none")
              .subject("test-subject")
              .claim(
                  "dcc_user_id", UUID.fromString("11111111-1111-1111-1111-111111111111").toString())
              .issuedAt(Instant.now())
              .expiresAt(Instant.now().plusSeconds(300))
              .build();
    }
  }
}
