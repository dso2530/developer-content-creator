package io.developercontentcreator.repository.application.port.out;

import io.developercontentcreator.repository.domain.model.RegisteredRepository;

public interface SaveRegisteredRepositoryPort {

  RegisteredRepository save(RegisteredRepository repository);
}
