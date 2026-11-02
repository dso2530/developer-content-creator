package io.developercontentcreator.repository.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import io.developercontentcreator.repository.domain.model.RegisteredRepository;
import io.developercontentcreator.repository.domain.model.RepositoryVisibility;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisteredRepositoryEntityTest {

  @Test
  void shouldConvertDomainToEntityAndBack() {
    RegisteredRepository original = createRepository("main");

    RegisteredRepositoryEntity entity = RegisteredRepositoryEntity.fromDomain(original);
    RegisteredRepository restored = entity.toDomain();

    assertThat(restored.id()).isEqualTo(original.id());
    assertThat(restored.githubRepositoryId()).isEqualTo(original.githubRepositoryId());
    assertThat(restored.installationId()).isEqualTo(original.installationId());
    assertThat(restored.name()).isEqualTo(original.name());
    assertThat(restored.fullName()).isEqualTo(original.fullName());
    assertThat(restored.htmlUrl()).isEqualTo(original.htmlUrl());
    assertThat(restored.defaultBranch()).contains("main");
    assertThat(restored.visibility()).isEqualTo(original.visibility());
  }

  @Test
  void shouldPreserveNullDefaultBranch() {
    RegisteredRepository original = createRepository(null);

    RegisteredRepositoryEntity entity = RegisteredRepositoryEntity.fromDomain(original);
    RegisteredRepository restored = entity.toDomain();

    assertThat(restored.defaultBranch()).isEmpty();
  }

  private RegisteredRepository createRepository(String defaultBranch) {
    return new RegisteredRepository(
        UUID.randomUUID(),
        123456L,
        98765L,
        "developer-content-creator",
        "acme/developer-content-creator",
        URI.create("https://github.com/acme/developer-content-creator"),
        defaultBranch,
        RepositoryVisibility.PRIVATE);
  }
}
