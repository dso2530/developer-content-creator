package io.developercontentcreator.repository.application.exception;

public class RepositoryAlreadyRegisteredException extends RuntimeException {

  public RepositoryAlreadyRegisteredException(Long installationId, Long githubRepositoryId) {
    super(
        "GitHub repository "
            + githubRepositoryId
            + " is already registered for installation "
            + installationId);
  }
}
