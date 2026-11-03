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
    properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(RepositorySecurityIntegrationTest.TestJwtDecoderConfiguration.class)
class RepositorySecurityIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void shouldRejectRequestWithoutJwtWhenIssuerIsConfigured() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/repositories", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldAcceptValidJwtAndReachMvcRouting() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("test-token");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/repositories",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    // 404 is expected: the route is not implemented yet.
    // It proves that JWT authentication succeeded and MVC received the request.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
