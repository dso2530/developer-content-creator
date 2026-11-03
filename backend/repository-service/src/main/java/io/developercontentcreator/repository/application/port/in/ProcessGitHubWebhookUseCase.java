package io.developercontentcreator.repository.application.port.in;

public interface ProcessGitHubWebhookUseCase {

  void processInstallationDeleted(String deliveryId, Long installationId);
}
