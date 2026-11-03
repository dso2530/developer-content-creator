package io.developercontentcreator.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.developercontentcreator.repository.application.port.in.ProcessGitHubWebhookUseCase;
import io.developercontentcreator.repository.application.port.out.MarkGitHubWebhookDeliveryProcessedPort;
import io.developercontentcreator.repository.infrastructure.persistence.adapter.GitHubWebhookDeliveryPersistenceAdapter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(GitHubWebhookTransactionIntegrationTest.RetryTestConfiguration.class)
class GitHubWebhookTransactionIntegrationTest {

  private static final Long INSTALLATION_ID = 12345L;

  private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }

  @Autowired private ProcessGitHubWebhookUseCase processGitHubWebhookUseCase;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FailOnceMarkProcessedPort failOnceMarkProcessedPort;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM github_webhook_deliveries");

    jdbcTemplate.update(
        "DELETE FROM registered_repositories WHERE installation_id = ?", INSTALLATION_ID);

    jdbcTemplate.update(
        "DELETE FROM github_installations WHERE installation_id = ?", INSTALLATION_ID);

    jdbcTemplate.update(
        """
        INSERT INTO github_installations (
            installation_id,
            github_account_id,
            account_login,
            account_type,
            user_id,
            status
        )
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        INSTALLATION_ID,
        98765L,
        "acme",
        "ORGANIZATION",
        USER_ID,
        "ACTIVE");

    failOnceMarkProcessedPort.reset();
  }

  @Test
  void shouldRollbackAndAllowRetryWithSameDeliveryIdAfterProcessingFailure() {
    String deliveryId = "github-delivery-retry";

    // First attempt: simulate a failure during delivery processing.
    assertThatThrownBy(
            () ->
                processGitHubWebhookUseCase.processInstallationDeleted(deliveryId, INSTALLATION_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Simulated processing failure");

    // Verify that the entire transaction was rolled back.
    assertThat(countDeliveries(deliveryId)).isZero();
    assertThat(findInstallationStatus()).isEqualTo("ACTIVE");

    // Second attempt: reuse the same delivery ID.
    // The failure decorator now delegates to the real persistence adapter.
    processGitHubWebhookUseCase.processInstallationDeleted(deliveryId, INSTALLATION_ID);

    // Verify that the retry completed successfully.
    assertThat(countDeliveries(deliveryId)).isEqualTo(1);
    assertThat(countProcessedDeliveries(deliveryId)).isEqualTo(1);
    assertThat(findInstallationStatus()).isEqualTo("UNINSTALLED");
  }

  private int countDeliveries(String deliveryId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM github_webhook_deliveries
        WHERE delivery_id = ?
        """,
        Integer.class,
        deliveryId);
  }

  private int countProcessedDeliveries(String deliveryId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM github_webhook_deliveries
        WHERE delivery_id = ?
          AND status = 'PROCESSED'
          AND processed_at IS NOT NULL
        """,
        Integer.class,
        deliveryId);
  }

  private String findInstallationStatus() {
    return jdbcTemplate.queryForObject(
        """
        SELECT status
        FROM github_installations
        WHERE installation_id = ?
        """,
        String.class,
        INSTALLATION_ID);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class RetryTestConfiguration {

    @Bean
    @Primary
    FailOnceMarkProcessedPort failOnceMarkProcessedPort(
        GitHubWebhookDeliveryPersistenceAdapter delegate) {
      return new FailOnceMarkProcessedPort(delegate);
    }
  }

  static class FailOnceMarkProcessedPort implements MarkGitHubWebhookDeliveryProcessedPort {

    private final GitHubWebhookDeliveryPersistenceAdapter delegate;

    private final AtomicBoolean failNext = new AtomicBoolean(true);

    FailOnceMarkProcessedPort(GitHubWebhookDeliveryPersistenceAdapter delegate) {
      this.delegate = delegate;
    }

    @Override
    public void markProcessed(String deliveryId) {
      if (failNext.compareAndSet(true, false)) {
        throw new IllegalStateException("Simulated processing failure");
      }

      delegate.markProcessed(deliveryId);
    }

    void reset() {
      failNext.set(true);
    }
  }
}
