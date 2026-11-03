package io.developercontentcreator.repository.application.port.out;

public interface RegisterGitHubWebhookDeliveryPort {

  /**
   * Registers a GitHub webhook delivery if it has not already been received.
   *
   * @return true if the delivery was newly registered, false if it already exists
   */
  boolean register(String deliveryId, String eventType, String action);
}
