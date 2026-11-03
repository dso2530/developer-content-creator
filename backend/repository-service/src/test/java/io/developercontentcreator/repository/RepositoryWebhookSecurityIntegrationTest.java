package io.developercontentcreator.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.developercontentcreator.repository.application.port.in.ProcessGitHubWebhookUseCase;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test",
      "developer-content-creator.github.webhook.enabled=true",
      "developer-content-creator.github.webhook.secret=test-github-webhook-secret"
    })
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(RepositoryWebhookSecurityIntegrationTest.TestJwtDecoderConfiguration.class)
class RepositoryWebhookSecurityIntegrationTest {

  private static final String WEBHOOK_ENDPOINT = "/api/webhooks/github";
  private static final String REPOSITORIES_ENDPOINT = "/api/v1/repositories";
  private static final String WEBHOOK_SECRET = "test-github-webhook-secret";

  private static final String PAYLOAD =
      """
      {
        "action": "deleted",
        "installation": {
          "id": 12345
        }
      }
      """;

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @MockitoBean private ProcessGitHubWebhookUseCase processGitHubWebhookUseCase;

  @Test
  void shouldAcceptValidWebhookWithoutJwt() throws Exception {
    byte[] payload = PAYLOAD.getBytes(StandardCharsets.UTF_8);
    String deliveryId = UUID.randomUUID().toString();

    HttpHeaders headers = webhookHeaders(sign(payload), deliveryId);

    ResponseEntity<String> response =
        restTemplate.exchange(
            url(WEBHOOK_ENDPOINT),
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    verify(processGitHubWebhookUseCase).processInstallationDeleted(deliveryId, 12345L);
  }

  @Test
  void shouldRejectWebhookWithInvalidSignature() throws Exception {
    byte[] payload = PAYLOAD.getBytes(StandardCharsets.UTF_8);
    String deliveryId = UUID.randomUUID().toString();

    HttpHeaders headers = webhookHeaders("sha256=" + "0".repeat(64), deliveryId);

    ResponseEntity<String> response =
        restTemplate.exchange(
            url(WEBHOOK_ENDPOINT),
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  @Test
  void shouldKeepBusinessEndpointsProtectedByJwtWhenWebhookIsEnabled() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(url(REPOSITORIES_ENDPOINT), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  private HttpHeaders webhookHeaders(String signature, String deliveryId) {
    HttpHeaders headers = new HttpHeaders();

    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Hub-Signature-256", signature);
    headers.set("X-GitHub-Event", "installation");
    headers.set("X-GitHub-Delivery", deliveryId);

    return headers;
  }

  private String url(String endpoint) {
    return "http://localhost:" + port + endpoint;
  }

  private String sign(byte[] payload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");

    SecretKeySpec secretKey =
        new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    mac.init(secretKey);

    return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
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
