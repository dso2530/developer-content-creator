package io.developercontentcreator.repository.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.repository.domain.model.GitHubAccount;
import io.developercontentcreator.repository.domain.model.GitHubAccountType;
import io.developercontentcreator.repository.domain.model.GitHubInstallation;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GitHubInstallationEntityTest {

  @Test
  void shouldConvertDomainToEntityAndBack() {
    GitHubInstallation original =
        new GitHubInstallation(
            98765L, new GitHubAccount(123456L, "acme", GitHubAccountType.ORGANIZATION));

    UUID userId = UUID.randomUUID();

    GitHubInstallationEntity entity = GitHubInstallationEntity.fromDomain(original, userId);

    GitHubInstallation restored = entity.toDomain();

    assertThat(restored.installationId()).isEqualTo(original.installationId());
    assertThat(restored.account().githubId()).isEqualTo(original.account().githubId());
    assertThat(restored.account().login()).isEqualTo(original.account().login());
    assertThat(restored.account().type()).isEqualTo(original.account().type());
  }
}
