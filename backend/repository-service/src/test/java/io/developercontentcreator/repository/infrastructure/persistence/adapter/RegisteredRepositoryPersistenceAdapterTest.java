package io.developercontentcreator.repository.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.developercontentcreator.repository.application.exception.RepositoryAlreadyRegisteredException;
import io.developercontentcreator.repository.domain.model.RegisteredRepository;
import io.developercontentcreator.repository.domain.model.RepositoryVisibility;
import io.developercontentcreator.repository.infrastructure.persistence.repository.SpringDataRegisteredRepositoryRepository;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RegisteredRepositoryPersistenceAdapterTest {

  @Autowired private RegisteredRepositoryPersistenceAdapter adapter;

  @Autowired private SpringDataRegisteredRepositoryRepository repository;

  @Autowired private SpringDataRegisteredRepositoryRepository registeredRepositoryRepository;

  private UUID ownerId;

  @BeforeEach
  void cleanDatabase() {
    registeredRepositoryRepository.deleteAll();
    repository.deleteAll();
    ownerId = UUID.randomUUID();
  }

  @Test
  void shouldSaveAndReturnRegisteredRepository() {
    RegisteredRepository repositoryToSave = createRepository(123456L, 98765L);

    RegisteredRepository saved = adapter.save(repositoryToSave);

    assertThat(saved.id()).isEqualTo(repositoryToSave.id());
    assertThat(saved.githubRepositoryId()).isEqualTo(123456L);
    assertThat(saved.installationId()).isEqualTo(98765L);
    assertThat(saved.fullName()).isEqualTo("acme/developer-content-creator");
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void shouldReturnTrueWhenRepositoryAlreadyExists() {
    RegisteredRepository repositoryToSave = createRepository(123456L, 98765L);
    adapter.save(repositoryToSave);

    boolean exists = adapter.existsByInstallationIdAndGitHubRepositoryId(98765L, 123456L);

    assertThat(exists).isTrue();
  }

  @Test
  void shouldReturnFalseWhenRepositoryDoesNotExist() {
    boolean exists = adapter.existsByInstallationIdAndGitHubRepositoryId(98765L, 123456L);

    assertThat(exists).isFalse();
  }

  @Test
  void shouldTranslateUniqueConstraintViolationIntoBusinessException() {
    RegisteredRepository first = createRepository(123456L, 98765L);

    RegisteredRepository duplicate =
        new RegisteredRepository(
            UUID.randomUUID(),
            first.githubRepositoryId(),
            first.installationId(),
            first.name(),
            first.fullName(),
            first.htmlUrl(),
            first.defaultBranch().orElse(null),
            first.visibility());

    adapter.save(first);

    assertThatThrownBy(() -> adapter.save(duplicate))
        .isInstanceOf(RepositoryAlreadyRegisteredException.class)
        .hasMessageContaining("123456")
        .hasMessageContaining("98765");

    assertThat(repository.count()).isEqualTo(1);
  }

  private RegisteredRepository createRepository(Long githubRepositoryId, Long installationId) {
    return new RegisteredRepository(
        UUID.randomUUID(),
        githubRepositoryId,
        installationId,
        "developer-content-creator",
        "acme/developer-content-creator",
        URI.create("https://github.com/acme/developer-content-creator"),
        "main",
        RepositoryVisibility.PRIVATE);
  }
}
