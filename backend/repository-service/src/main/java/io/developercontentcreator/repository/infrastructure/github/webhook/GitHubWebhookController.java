package io.developercontentcreator.repository.infrastructure.github.webhook;

import io.developercontentcreator.repository.application.port.in.ProcessGitHubWebhookUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/webhooks/github")
@ConditionalOnProperty(
    prefix = "developer-content-creator.github.webhook",
    name = "enabled",
    havingValue = "true")
public class GitHubWebhookController {

  private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
  private static final String EVENT_HEADER = "X-GitHub-Event";
  private static final String DELIVERY_HEADER = "X-GitHub-Delivery";

  private static final String INSTALLATION_EVENT = "installation";
  private static final String DELETED_ACTION = "deleted";

  private final GitHubWebhookSignatureValidator signatureValidator;
  private final ProcessGitHubWebhookUseCase processGitHubWebhookUseCase;
  private final ObjectMapper objectMapper;

  public GitHubWebhookController(
      GitHubWebhookSignatureValidator signatureValidator,
      ProcessGitHubWebhookUseCase processGitHubWebhookUseCase,
      ObjectMapper objectMapper) {

    this.signatureValidator = signatureValidator;
    this.processGitHubWebhookUseCase = processGitHubWebhookUseCase;
    this.objectMapper = objectMapper;
  }

  @PostMapping
  public ResponseEntity<Void> handleWebhook(
      @RequestBody(required = false) byte[] payload,
      @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
      @RequestHeader(value = EVENT_HEADER, required = false) String event,
      @RequestHeader(value = DELIVERY_HEADER, required = false) String deliveryId) {

    // 1. Authenticate the raw request body before parsing JSON.
    if (!signatureValidator.isValid(payload, signature)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // 2. Validate the GitHub event header.
    if (event == null || event.isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    // 3. Ignore events that this endpoint does not process.
    if (!INSTALLATION_EVENT.equals(event)) {
      return ResponseEntity.ok().build();
    }

    // 4. Parse the authenticated payload.
    final JsonNode root;

    try {
      root = objectMapper.readTree(payload);
    } catch (JacksonException exception) {
      return ResponseEntity.badRequest().build();
    }

    if (root == null || !root.isObject()) {
      return ResponseEntity.badRequest().build();
    }

    JsonNode actionNode = root.get("action");

    if (actionNode == null || !actionNode.isTextual()) {
      return ResponseEntity.badRequest().build();
    }

    // 5. Ignore installation actions other than "deleted".
    if (!DELETED_ACTION.equals(actionNode.asText())) {
      return ResponseEntity.ok().build();
    }

    // 6. Validate the delivery ID for idempotent processing.
    if (deliveryId == null || deliveryId.isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    // 7. Extract and validate the installation ID.
    JsonNode installationIdNode = root.path("installation").path("id");

    if (!installationIdNode.isIntegralNumber()
        || !installationIdNode.canConvertToLong()
        || installationIdNode.longValue() <= 0) {
      return ResponseEntity.badRequest().build();
    }

    // 8. Delegate the complete operation to the application layer.
    processGitHubWebhookUseCase.processInstallationDeleted(
        deliveryId, installationIdNode.longValue());

    return ResponseEntity.ok().build();
  }
}
