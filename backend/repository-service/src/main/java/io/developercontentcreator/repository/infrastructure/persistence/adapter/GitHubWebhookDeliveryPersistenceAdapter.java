package io.developercontentcreator.repository.infrastructure.persistence.adapter;

import io.developercontentcreator.repository.application.port.out.MarkGitHubWebhookDeliveryProcessedPort;
import io.developercontentcreator.repository.application.port.out.RegisterGitHubWebhookDeliveryPort;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class GitHubWebhookDeliveryPersistenceAdapter
    implements RegisterGitHubWebhookDeliveryPort, MarkGitHubWebhookDeliveryProcessedPort {

  private static final String INSERT_DELIVERY_SQL =
      """
      INSERT INTO github_webhook_deliveries (
          id,
          delivery_id,
          event_type,
          action,
          status,
          received_at
      )
      VALUES (?, ?, ?, ?, 'RECEIVED', CURRENT_TIMESTAMP)
      ON CONFLICT (delivery_id) DO NOTHING
      """;

  private static final String MARK_PROCESSED_SQL =
      """
      UPDATE github_webhook_deliveries
         SET status = 'PROCESSED',
             processed_at = CURRENT_TIMESTAMP
       WHERE delivery_id = ?
         AND status = 'RECEIVED'
      """;

  private final JdbcTemplate jdbcTemplate;

  public GitHubWebhookDeliveryPersistenceAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public boolean register(String deliveryId, String eventType, String action) {

    int affectedRows =
        jdbcTemplate.update(INSERT_DELIVERY_SQL, UUID.randomUUID(), deliveryId, eventType, action);

    return affectedRows == 1;
  }

  @Override
  @Transactional
  public void markProcessed(String deliveryId) {

    int affectedRows = jdbcTemplate.update(MARK_PROCESSED_SQL, deliveryId);

    if (affectedRows != 1) {
      throw new IllegalStateException(
          "GitHub webhook delivery could not be marked as processed: " + deliveryId);
    }
  }
}
