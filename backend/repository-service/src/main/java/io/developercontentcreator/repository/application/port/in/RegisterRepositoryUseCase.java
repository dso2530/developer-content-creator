package io.developercontentcreator.repository.application.port.in;

import io.developercontentcreator.repository.domain.model.RegisteredRepository;
import java.util.UUID;

public interface RegisterRepositoryUseCase {

  RegisteredRepository register(RegisterRepositoryCommand command);

  record RegisterRepositoryCommand(Long installationId, Long githubRepositoryId, UUID requestedBy) {

    public RegisterRepositoryCommand {
      if (installationId == null || installationId <= 0) {
        throw new IllegalArgumentException("Installation ID must be positive");
      }

      if (githubRepositoryId == null || githubRepositoryId <= 0) {
        throw new IllegalArgumentException("GitHub repository ID must be positive");
      }

      if (requestedBy == null) {
        throw new IllegalArgumentException("Requesting user ID must not be null");
      }
    }
  }
}
