package io.developercontentcreator.repository.application.service;

import io.developercontentcreator.repository.application.port.in.ProcessGitHubWebhookUseCase;
import io.developercontentcreator.repository.application.port.out.DeactivateGitHubInstallationPort;
import io.developercontentcreator.repository.application.port.out.MarkGitHubWebhookDeliveryProcessedPort;
import io.developercontentcreator.repository.application.port.out.RegisterGitHubWebhookDeliveryPort;
import org.springframework.transaction.annotation.Transactional;

public class ProcessGitHubWebhookService implements ProcessGitHubWebhookUseCase {

  private static final String EVENT_TYPE = "installation";
  private static final String DELETED_ACTION = "deleted";

  private final RegisterGitHubWebhookDeliveryPort registerDeliveryPort;
  private final DeactivateGitHubInstallationPort deactivateInstallationPort;
  private final MarkGitHubWebhookDeliveryProcessedPort markDeliveryProcessedPort;

  public ProcessGitHubWebhookService(
      RegisterGitHubWebhookDeliveryPort registerDeliveryPort,
      DeactivateGitHubInstallationPort deactivateInstallationPort,
      MarkGitHubWebhookDeliveryProcessedPort markDeliveryProcessedPort) {

    this.registerDeliveryPort = registerDeliveryPort;
    this.deactivateInstallationPort = deactivateInstallationPort;
    this.markDeliveryProcessedPort = markDeliveryProcessedPort;
  }

  @Override
  @Transactional
  public void processInstallationDeleted(String deliveryId, Long installationId) {

    validate(deliveryId, installationId);

    boolean registered = registerDeliveryPort.register(deliveryId, EVENT_TYPE, DELETED_ACTION);

    if (!registered) {
      return;
    }

    deactivateInstallationPort.deactivate(installationId);

    markDeliveryProcessedPort.markProcessed(deliveryId);
  }

  private void validate(String deliveryId, Long installationId) {

    if (deliveryId == null || deliveryId.isBlank()) {
      throw new IllegalArgumentException("GitHub delivery ID must not be blank");
    }

    if (installationId == null || installationId <= 0) {
      throw new IllegalArgumentException("GitHub installation ID must be positive");
    }
  }
}
