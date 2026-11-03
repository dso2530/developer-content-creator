package io.developercontentcreator.repository.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.developercontentcreator.repository.domain.model.RegisteredRepository;
import io.developercontentcreator.repository.domain.model.RepositoryVisibility;
import io.developercontentcreator.repository.infrastructure.persistence.entity.RegisteredRepositoryEntity;
import io.developercontentcreator.repository.infrastructure.persistence.repository.SpringDataRegisteredRepositoryRepository;
import java.net.URI;
import java.sql.SQLException;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class RegisteredRepositoryPersistenceAdapterUnitTest {

  @Test
  void shouldPropagateOtherIntegrityConstraintViolations() {
    SpringDataRegisteredRepositoryRepository repository =
        mock(SpringDataRegisteredRepositoryRepository.class);

    RegisteredRepositoryPersistenceAdapter adapter =
        new RegisteredRepositoryPersistenceAdapter(repository);

    DataIntegrityViolationException databaseException =
        new DataIntegrityViolationException(
            "Database integrity violation",
            new ConstraintViolationException(
                "Check constraint violation",
                new SQLException("Invalid value"),
                "chk_registered_repositories_name"));

    when(repository.saveAndFlush(any(RegisteredRepositoryEntity.class)))
        .thenThrow(databaseException);

    RegisteredRepository registeredRepository =
        new RegisteredRepository(
            UUID.randomUUID(),
            123456L,
            98765L,
            "developer-content-creator",
            "acme/developer-content-creator",
            URI.create("https://github.com/acme/developer-content-creator"),
            "main",
            RepositoryVisibility.PRIVATE);

    assertThatThrownBy(() -> adapter.save(registeredRepository)).isSameAs(databaseException);
  }
}
