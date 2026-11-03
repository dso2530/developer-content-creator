package io.developercontentcreator.repository.application.port.out;

public interface MarkGitHubWebhookDeliveryProcessedPort {

  /**
   * Marks a previously registered GitHub webhook delivery as processed.
   *
   * @param deliveryId GitHub webhook delivery identifier
   */
  void markProcessed(String deliveryId);
}
