package io.developercontentcreator.repository.infrastructure.persistence.adapter;

import io.developercontentcreator.repository.application.exception.RepositoryAlreadyRegisteredException;
import io.developercontentcreator.repository.application.port.out.ExistsRegisteredRepositoryPort;
import io.developercontentcreator.repository.application.port.out.SaveRegisteredRepositoryPort;
import io.developercontentcreator.repository.domain.model.RegisteredRepository;
import io.developercontentcreator.repository.infrastructure.persistence.entity.RegisteredRepositoryEntity;
import io.developercontentcreator.repository.infrastructure.persistence.repository.SpringDataRegisteredRepositoryRepository;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class RegisteredRepositoryPersistenceAdapter
    implements ExistsRegisteredRepositoryPort, SaveRegisteredRepositoryPort {

  private static final String UNIQUE_CONSTRAINT =
      "uk_registered_repositories_installation_repository";

  private final SpringDataRegisteredRepositoryRepository repository;

  public RegisteredRepositoryPersistenceAdapter(
      SpringDataRegisteredRepositoryRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean existsByInstallationIdAndGitHubRepositoryId(
      Long installationId, Long githubRepositoryId) {

    return repository.existsByInstallationIdAndGithubRepositoryId(
        installationId, githubRepositoryId);
  }

  @Override
  @Transactional
  public RegisteredRepository save(RegisteredRepository registeredRepository) {

    RegisteredRepositoryEntity entity = RegisteredRepositoryEntity.fromDomain(registeredRepository);

    try {
      RegisteredRepositoryEntity saved = repository.saveAndFlush(entity);
      return saved.toDomain();
    } catch (DataIntegrityViolationException exception) {
      if (isUniqueRepositoryConstraintViolation(exception)) {
        throw new RepositoryAlreadyRegisteredException(
            registeredRepository.installationId(), registeredRepository.githubRepositoryId());
      }

      throw exception;
    }
  }

  private boolean isUniqueRepositoryConstraintViolation(Throwable exception) {

    Throwable current = exception;

    while (current != null) {
      if (current instanceof ConstraintViolationException violation) {
        String constraintName = violation.getConstraintName();

        return constraintName != null
            && constraintName
                .toLowerCase(Locale.ROOT)
                .contains(UNIQUE_CONSTRAINT.toLowerCase(Locale.ROOT));
      }

      current = current.getCause();
    }

    return false;
  }
}
