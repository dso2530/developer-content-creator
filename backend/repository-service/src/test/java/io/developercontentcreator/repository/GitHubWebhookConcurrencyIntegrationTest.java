package io.developercontentcreator.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.repository.application.port.in.ProcessGitHubWebhookUseCase;
import io.developercontentcreator.repository.application.port.out.DeactivateGitHubInstallationPort;
import io.developercontentcreator.repository.infrastructure.persistence.adapter.GitHubInstallationPersistenceAdapter;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
@Import(GitHubWebhookConcurrencyIntegrationTest.ConcurrencyTestConfiguration.class)
class GitHubWebhookConcurrencyIntegrationTest {

  private static final Long INSTALLATION_ID = 12346L;

  private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired private ProcessGitHubWebhookUseCase processGitHubWebhookUseCase;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private DeactivationCountingPort deactivationCountingPort;

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }

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
        98766L,
        "acme-concurrent",
        "ORGANIZATION",
        USER_ID,
        "ACTIVE");

    deactivationCountingPort.reset();
  }

  @Test
  void shouldProcessConcurrentDuplicateDeliveriesOnlyOnce() throws Exception {

    String deliveryId = "github-concurrent-" + UUID.randomUUID();

    CountDownLatch readySignal = new CountDownLatch(2);
    CountDownLatch startSignal = new CountDownLatch(1);

    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<?> first =
          executor.submit(() -> processWebhookConcurrently(deliveryId, readySignal, startSignal));

      Future<?> second =
          executor.submit(() -> processWebhookConcurrently(deliveryId, readySignal, startSignal));

      assertThat(readySignal.await(10, TimeUnit.SECONDS))
          .as("Both webhook requests should be ready")
          .isTrue();

      startSignal.countDown();

      first.get(30, TimeUnit.SECONDS);
      second.get(30, TimeUnit.SECONDS);

      Integer deliveryCount =
          jdbcTemplate.queryForObject(
              """
              SELECT COUNT(*)
              FROM github_webhook_deliveries
              WHERE delivery_id = ?
              """,
              Integer.class,
              deliveryId);

      assertThat(deliveryCount).isEqualTo(1);

      String deliveryStatus =
          jdbcTemplate.queryForObject(
              """
              SELECT status
              FROM github_webhook_deliveries
              WHERE delivery_id = ?
              """,
              String.class,
              deliveryId);

      assertThat(deliveryStatus).isEqualTo("PROCESSED");

      String installationStatus =
          jdbcTemplate.queryForObject(
              """
              SELECT status
              FROM github_installations
              WHERE installation_id = ?
              """,
              String.class,
              INSTALLATION_ID);

      assertThat(installationStatus).isEqualTo("UNINSTALLED");

      assertThat(deactivationCountingPort.invocationCount())
          .as("Installation should be deactivated only once")
          .isEqualTo(1);

    } finally {
      startSignal.countDown();
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  private void processWebhookConcurrently(
      String deliveryId, CountDownLatch readySignal, CountDownLatch startSignal) {

    readySignal.countDown();

    try {
      if (!startSignal.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Concurrent webhook start timed out");
      }

      processGitHubWebhookUseCase.processInstallationDeleted(deliveryId, INSTALLATION_ID);

    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Webhook processing interrupted", exception);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ConcurrencyTestConfiguration {

    @Bean
    @Primary
    DeactivationCountingPort deactivationCountingPort(
        GitHubInstallationPersistenceAdapter delegate) {
      return new DeactivationCountingPort(delegate);
    }
  }

  static class DeactivationCountingPort implements DeactivateGitHubInstallationPort {

    private final GitHubInstallationPersistenceAdapter delegate;
    private final AtomicInteger invocationCount = new AtomicInteger();

    DeactivationCountingPort(GitHubInstallationPersistenceAdapter delegate) {
      this.delegate = delegate;
    }

    @Override
    public void deactivate(Long installationId) {
      invocationCount.incrementAndGet();
      delegate.deactivate(installationId);
    }

    int invocationCount() {
      return invocationCount.get();
    }

    void reset() {
      invocationCount.set(0);
    }
  }
}
