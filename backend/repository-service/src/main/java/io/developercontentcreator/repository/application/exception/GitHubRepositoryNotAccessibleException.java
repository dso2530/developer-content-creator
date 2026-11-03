package io.developercontentcreator.repository.application.exception;

public class GitHubRepositoryNotAccessibleException extends RuntimeException {

  public GitHubRepositoryNotAccessibleException(Long installationId, Long githubRepositoryId) {
    super(
        "GitHub repository "
            + githubRepositoryId
            + " is not accessible through installation "
            + installationId);
  }
}
