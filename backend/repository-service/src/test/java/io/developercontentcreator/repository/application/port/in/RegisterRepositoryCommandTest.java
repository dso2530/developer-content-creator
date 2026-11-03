package io.developercontentcreator.repository.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterRepositoryCommandTest {

  private static final Long INSTALLATION_ID = 98765L;
  private static final Long REPOSITORY_ID = 123456L;
  private static final UUID REQUESTED_BY = UUID.randomUUID();

  @Test
  void shouldCreateCommandWithValidValues() {
    RegisterRepositoryUseCase.RegisterRepositoryCommand command =
        new RegisterRepositoryUseCase.RegisterRepositoryCommand(
            INSTALLATION_ID, REPOSITORY_ID, REQUESTED_BY);

    assertThat(command.installationId()).isEqualTo(INSTALLATION_ID);
    assertThat(command.githubRepositoryId()).isEqualTo(REPOSITORY_ID);
    assertThat(command.requestedBy()).isEqualTo(REQUESTED_BY);
  }

  @Test
  void shouldRejectNullInstallationId() {
    assertThatThrownBy(
            () ->
                new RegisterRepositoryUseCase.RegisterRepositoryCommand(
                    null, REPOSITORY_ID, REQUESTED_BY))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Installation ID must be positive");
  }

  @Test
  void shouldRejectNonPositiveInstallationId() {
    assertThatThrownBy(
            () ->
                new RegisterRepositoryUseCase.RegisterRepositoryCommand(
                    0L, REPOSITORY_ID, REQUESTED_BY))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Installation ID must be positive");
  }

  @Test
  void shouldRejectNullRepositoryId() {
    assertThatThrownBy(
            () ->
                new RegisterRepositoryUseCase.RegisterRepositoryCommand(
                    INSTALLATION_ID, null, REQUESTED_BY))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("GitHub repository ID must be positive");
  }

  @Test
  void shouldRejectNonPositiveRepositoryId() {
    assertThatThrownBy(
            () ->
                new RegisterRepositoryUseCase.RegisterRepositoryCommand(
                    INSTALLATION_ID, -1L, REQUESTED_BY))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("GitHub repository ID must be positive");
  }

  @Test
  void shouldRejectNullRequestingUser() {
    assertThatThrownBy(
            () ->
                new RegisterRepositoryUseCase.RegisterRepositoryCommand(
                    INSTALLATION_ID, REPOSITORY_ID, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Requesting user ID must not be null");
  }
}
