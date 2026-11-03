package io.developercontentcreator.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.developercontentcreator.repository.infrastructure.persistence.adapter.GitHubWebhookDeliveryPersistenceAdapter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class FlywayPostgreSqlIntegrationTest {

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  @Test
  void shouldApplyAllMigrationsThroughVersionFour() throws SQLException {
    try (Connection connection = getConnection();
        Statement statement = connection.createStatement();
        ResultSet result =
            statement.executeQuery(
                """
                     SELECT version
                     FROM flyway_schema_history
                     WHERE success = TRUE
                     ORDER BY installed_rank DESC
                     LIMIT 1
                     """)) {

      assertTrue(result.next());
      assertEquals("4", result.getString("version"));
    }
  }

  @Test
  void shouldCreateAllExpectedTables() throws SQLException {
    try (Connection connection = getConnection()) {
      for (String table :
          new String[] {
            "registered_repositories", "github_installations", "github_webhook_deliveries"
          }) {
        try (PreparedStatement statement =
            connection.prepareStatement(
                """
                        SELECT to_regclass(?)
                        """)) {

          statement.setString(1, "public." + table);

          try (ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            assertNotNull(result.getString(1), "Table missing: " + table);
          }
        }
      }
    }
  }

  @Test
  void shouldEnforceUniqueDeliveryId() throws SQLException {
    String deliveryId = UUID.randomUUID().toString();

    try (Connection connection = getConnection()) {
      insertDelivery(connection, deliveryId);

      SQLException exception =
          assertThrows(SQLException.class, () -> insertDelivery(connection, deliveryId));

      assertEquals("23505", exception.getSQLState());
    }
  }

  @Test
  void shouldRejectReceivedDeliveryWithProcessedAt() throws SQLException {
    try (Connection connection = getConnection();
        Statement statement = connection.createStatement()) {

      SQLException exception =
          assertThrows(
              SQLException.class,
              () ->
                  statement.executeUpdate(
                      """
                            INSERT INTO github_webhook_deliveries (
                                id, delivery_id, event_type, action,
                                status, received_at, processed_at
                            )
                            VALUES (
                                gen_random_uuid(),
                                'delivery-invalid-received',
                                'installation',
                                'deleted',
                                'RECEIVED',
                                CURRENT_TIMESTAMP,
                                CURRENT_TIMESTAMP
                            )
                            """));

      assertEquals("23514", exception.getSQLState());
    }
  }

  @Test
  void shouldRejectProcessedDeliveryWithoutProcessedAt() throws SQLException {
    try (Connection connection = getConnection();
        Statement statement = connection.createStatement()) {

      SQLException exception =
          assertThrows(
              SQLException.class,
              () ->
                  statement.executeUpdate(
                      """
                            INSERT INTO github_webhook_deliveries (
                                id, delivery_id, event_type, action,
                                status, received_at, processed_at
                            )
                            VALUES (
                                gen_random_uuid(),
                                'delivery-invalid-processed',
                                'installation',
                                'deleted',
                                'PROCESSED',
                                CURRENT_TIMESTAMP,
                                NULL
                            )
                            """));

      assertEquals("23514", exception.getSQLState());
    }
  }

  @Test
  void shouldAcceptProcessedDeliveryWithProcessedAt() throws SQLException {
    try (Connection connection = getConnection()) {
      String deliveryId = UUID.randomUUID().toString();

      insertDelivery(connection, deliveryId);

      try (PreparedStatement statement =
          connection.prepareStatement(
              """
                    UPDATE github_webhook_deliveries
                    SET status = 'PROCESSED',
                        processed_at = CURRENT_TIMESTAMP
                    WHERE delivery_id = ?
                    """)) {

        statement.setString(1, deliveryId);

        assertEquals(1, statement.executeUpdate());
      }
    }
  }

  private static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static void insertDelivery(Connection connection, String deliveryId) throws SQLException {

    try (PreparedStatement statement =
        connection.prepareStatement(
            """
                INSERT INTO github_webhook_deliveries (
                    id, delivery_id, event_type, action,
                    status, received_at, processed_at
                )
                VALUES (
                    ?, ?, 'installation', 'deleted',
                    'RECEIVED', CURRENT_TIMESTAMP, NULL
                )
                """)) {

      statement.setObject(1, UUID.randomUUID());
      statement.setString(2, deliveryId);

      statement.executeUpdate();
    }
  }

  @Test
  void shouldRegisterNewDeliveryAndIgnoreDuplicate() {
    GitHubWebhookDeliveryPersistenceAdapter adapter = createAdapter();
    String deliveryId = UUID.randomUUID().toString();

    assertTrue(adapter.register(deliveryId, "installation", "deleted"));
    assertFalse(adapter.register(deliveryId, "installation", "deleted"));

    Integer count =
        adapterJdbcTemplate()
            .queryForObject(
                "SELECT COUNT(*) FROM github_webhook_deliveries WHERE delivery_id = ?",
                Integer.class,
                deliveryId);

    assertEquals(1, count);
  }

  @Test
  void shouldMarkRegisteredDeliveryAsProcessed() {
    GitHubWebhookDeliveryPersistenceAdapter adapter = createAdapter();
    String deliveryId = UUID.randomUUID().toString();

    assertTrue(adapter.register(deliveryId, "installation", "deleted"));

    adapter.markProcessed(deliveryId);

    try (Connection connection = getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                """
              SELECT status, processed_at
              FROM github_webhook_deliveries
              WHERE delivery_id = ?
              """)) {

      statement.setString(1, deliveryId);

      try (ResultSet result = statement.executeQuery()) {
        assertTrue(result.next());
        assertEquals("PROCESSED", result.getString("status"));
        assertNotNull(result.getTimestamp("processed_at"));
      }
    } catch (SQLException exception) {
      throw new AssertionError("Unable to verify processed delivery", exception);
    }
  }

  @Test
  void shouldRejectMarkingUnknownDeliveryAsProcessed() {
    GitHubWebhookDeliveryPersistenceAdapter adapter = createAdapter();

    assertThrows(
        IllegalStateException.class, () -> adapter.markProcessed(UUID.randomUUID().toString()));
  }

  @Test
  void shouldHandleConcurrentRegistrationOfSameDelivery() throws Exception {
    GitHubWebhookDeliveryPersistenceAdapter adapter = createAdapter();
    String deliveryId = UUID.randomUUID().toString();

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startSignal = new CountDownLatch(1);

    try {
      Future<Boolean> first =
          executor.submit(
              () -> {
                startSignal.await();
                return adapter.register(deliveryId, "installation", "deleted");
              });

      Future<Boolean> second =
          executor.submit(
              () -> {
                startSignal.await();
                return adapter.register(deliveryId, "installation", "deleted");
              });

      startSignal.countDown();

      boolean firstResult = first.get();
      boolean secondResult = second.get();

      assertTrue(firstResult ^ secondResult);

      Integer count =
          adapterJdbcTemplate()
              .queryForObject(
                  "SELECT COUNT(*) FROM github_webhook_deliveries WHERE delivery_id = ?",
                  Integer.class,
                  deliveryId);

      assertEquals(1, count);
    } finally {
      executor.shutdownNow();
    }
  }

  private static GitHubWebhookDeliveryPersistenceAdapter createAdapter() {
    return new GitHubWebhookDeliveryPersistenceAdapter(adapterJdbcTemplate());
  }

  private static JdbcTemplate adapterJdbcTemplate() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setUrl(POSTGRES.getJdbcUrl());
    dataSource.setUsername(POSTGRES.getUsername());
    dataSource.setPassword(POSTGRES.getPassword());

    return new JdbcTemplate(dataSource);
  }
}
