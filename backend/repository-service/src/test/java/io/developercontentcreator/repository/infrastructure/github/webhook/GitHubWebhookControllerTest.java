package io.developercontentcreator.repository.infrastructure.github.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.developercontentcreator.repository.application.port.in.ProcessGitHubWebhookUseCase;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

class GitHubWebhookControllerTest {

  private static final String WEBHOOK_SECRET = "test-github-webhook-secret";
  private static final String INSTALLATION_EVENT = "installation";
  private static final String DELIVERY_ID = "github-delivery-12345";

  private static final String DELETED_PAYLOAD =
      """
      {
        "action": "deleted",
        "installation": {
          "id": 12345
        }
      }
      """;

  private ProcessGitHubWebhookUseCase processGitHubWebhookUseCase;
  private GitHubWebhookController controller;

  @BeforeEach
  void setUp() {
    processGitHubWebhookUseCase = mock(ProcessGitHubWebhookUseCase.class);

    GitHubWebhookSignatureValidator signatureValidator =
        new GitHubWebhookSignatureValidator(WEBHOOK_SECRET);

    controller =
        new GitHubWebhookController(
            signatureValidator, processGitHubWebhookUseCase, new ObjectMapper());
  }

  @Test
  void shouldRejectRequestWhenSignatureIsMissing() {
    ResponseEntity<Void> response =
        controller.handleWebhook(
            DELETED_PAYLOAD.getBytes(StandardCharsets.UTF_8),
            null,
            INSTALLATION_EVENT,
            DELIVERY_ID);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  @Test
  void shouldRejectRequestWhenSignatureIsInvalid() {
    ResponseEntity<Void> response =
        controller.handleWebhook(
            DELETED_PAYLOAD.getBytes(StandardCharsets.UTF_8),
            "sha256=" + "0".repeat(64),
            INSTALLATION_EVENT,
            DELIVERY_ID);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  @Test
  void shouldRejectRequestWhenEventHeaderIsMissing() throws Exception {
    byte[] payload = DELETED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

    ResponseEntity<Void> response =
        controller.handleWebhook(payload, sign(payload), null, DELIVERY_ID);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  @Test
  void shouldIgnoreUnsupportedEvent() throws Exception {
    byte[] payload = DELETED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

    ResponseEntity<Void> response = controller.handleWebhook(payload, sign(payload), "push", null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  @Test
  void shouldIgnoreInstallationActionOtherThanDeleted() throws Exception {
    byte[] payload =
        """
        {
          "action": "created",
          "installation": {
            "id": 12345
          }
        }
        """
            .getBytes(StandardCharsets.UTF_8);

    ResponseEntity<Void> response =
        controller.handleWebhook(payload, sign(payload), INSTALLATION_EVENT, null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  @Test
  void shouldRejectDeletedEventWhenDeliveryIdIsMissing() throws Exception {
    byte[] payload = DELETED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

    ResponseEntity<Void> response =
        controller.handleWebhook(payload, sign(payload), INSTALLATION_EVENT, null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  @Test
  void shouldRejectDeletedEventWhenDeliveryIdIsBlank() throws Exception {
    byte[] payload = DELETED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

    ResponseEntity<Void> response =
        controller.handleWebhook(payload, sign(payload), INSTALLATION_EVENT, " ");

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  @Test
  void shouldProcessInstallationDeletedEvent() throws Exception {
    byte[] payload = DELETED_PAYLOAD.getBytes(StandardCharsets.UTF_8);

    ResponseEntity<Void> response =
        controller.handleWebhook(payload, sign(payload), INSTALLATION_EVENT, DELIVERY_ID);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    verify(processGitHubWebhookUseCase).processInstallationDeleted(DELIVERY_ID, 12345L);
  }

  @Test
  void shouldRejectMalformedJson() throws Exception {
    byte[] payload = "{ invalid-json".getBytes(StandardCharsets.UTF_8);

    ResponseEntity<Void> response =
        controller.handleWebhook(payload, sign(payload), INSTALLATION_EVENT, DELIVERY_ID);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  @Test
  void shouldRejectDeletedEventWhenInstallationIdIsInvalid() throws Exception {
    byte[] payload =
        """
        {
          "action": "deleted",
          "installation": {
            "id": -123
          }
        }
        """
            .getBytes(StandardCharsets.UTF_8);

    ResponseEntity<Void> response =
        controller.handleWebhook(payload, sign(payload), INSTALLATION_EVENT, DELIVERY_ID);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verifyNoInteractions(processGitHubWebhookUseCase);
  }

  private String sign(byte[] payload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");

    SecretKeySpec secretKey =
        new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    mac.init(secretKey);

    byte[] signature = mac.doFinal(payload);

    return "sha256=" + HexFormat.of().formatHex(signature);
  }
}
