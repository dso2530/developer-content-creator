package io.developercontentcreator.repository.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.repository.domain.model.GitHubAccount;
import io.developercontentcreator.repository.domain.model.GitHubAccountType;
import io.developercontentcreator.repository.domain.model.GitHubInstallation;
import io.developercontentcreator.repository.infrastructure.persistence.entity.GitHubInstallationEntity;
import io.developercontentcreator.repository.infrastructure.persistence.repository.SpringDataGitHubInstallationRepository;
import io.developercontentcreator.repository.infrastructure.persistence.repository.SpringDataRegisteredRepositoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GitHubInstallationPersistenceAdapterTest {

  @Autowired private GitHubInstallationPersistenceAdapter adapter;

  @Autowired private SpringDataGitHubInstallationRepository repository;

  @Autowired private SpringDataRegisteredRepositoryRepository registeredRepositoryRepository;

  private UUID ownerId;

  @BeforeEach
  void cleanDatabase() {
    // Delete dependent entities before their referenced installations.
    registeredRepositoryRepository.deleteAll();
    repository.deleteAll();

    ownerId = UUID.randomUUID();
  }

  @Test
  void shouldReturnInstallationWhenItBelongsToUser() {
    GitHubInstallation installation = createInstallation(98765L);

    repository.saveAndFlush(GitHubInstallationEntity.fromDomain(installation, ownerId));

    var result = adapter.findByInstallationIdAndUserId(98765L, ownerId);

    assertThat(result).isPresent();
    assertThat(result.get().installationId()).isEqualTo(98765L);
    assertThat(result.get().account().githubId()).isEqualTo(123456L);
    assertThat(result.get().account().login()).isEqualTo("acme");
    assertThat(result.get().account().type()).isEqualTo(GitHubAccountType.ORGANIZATION);
  }

  @Test
  void shouldReturnEmptyWhenInstallationDoesNotExist() {
    var result = adapter.findByInstallationIdAndUserId(98765L, ownerId);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenInstallationBelongsToAnotherUser() {
    UUID anotherUserId = UUID.randomUUID();
    GitHubInstallation installation = createInstallation(98765L);

    repository.saveAndFlush(GitHubInstallationEntity.fromDomain(installation, anotherUserId));

    var result = adapter.findByInstallationIdAndUserId(98765L, ownerId);

    assertThat(result).isEmpty();
  }

  private GitHubInstallation createInstallation(Long installationId) {
    GitHubAccount account = new GitHubAccount(123456L, "acme", GitHubAccountType.ORGANIZATION);

    return new GitHubInstallation(installationId, account);
  }
}
