package io.developercontentcreator.repository.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.developercontentcreator.repository.application.exception.GitHubInstallationNotFoundException;
import io.developercontentcreator.repository.application.exception.GitHubRepositoryNotAccessibleException;
import io.developercontentcreator.repository.application.exception.RepositoryAlreadyRegisteredException;
import io.developercontentcreator.repository.application.port.in.RegisterRepositoryUseCase.RegisterRepositoryCommand;
import io.developercontentcreator.repository.application.port.out.ExistsRegisteredRepositoryPort;
import io.developercontentcreator.repository.application.port.out.LoadGitHubInstallationPort;
import io.developercontentcreator.repository.application.port.out.LoadGitHubRepositoryPort;
import io.developercontentcreator.repository.application.port.out.SaveRegisteredRepositoryPort;
import io.developercontentcreator.repository.domain.model.GitHubAccount;
import io.developercontentcreator.repository.domain.model.GitHubAccountType;
import io.developercontentcreator.repository.domain.model.GitHubInstallation;
import io.developercontentcreator.repository.domain.model.GitHubRepositorySnapshot;
import io.developercontentcreator.repository.domain.model.RegisteredRepository;
import io.developercontentcreator.repository.domain.model.RepositoryVisibility;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterRepositoryServiceTest {

  private static final Long INSTALLATION_ID = 12345L;
  private static final Long GITHUB_REPOSITORY_ID = 67890L;
  private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  @Mock private LoadGitHubInstallationPort installationPort;

  @Mock private LoadGitHubRepositoryPort repositoryPort;

  @Mock private ExistsRegisteredRepositoryPort existsPort;

  @Mock private SaveRegisteredRepositoryPort savePort;

  private RegisterRepositoryService service;

  private RegisterRepositoryCommand command;

  private GitHubInstallation installation;

  private GitHubRepositorySnapshot repositorySnapshot;

  @BeforeEach
  void setUp() {
    service = new RegisterRepositoryService(installationPort, repositoryPort, existsPort, savePort);

    command = new RegisterRepositoryCommand(INSTALLATION_ID, GITHUB_REPOSITORY_ID, USER_ID);

    installation =
        new GitHubInstallation(
            INSTALLATION_ID, new GitHubAccount(100L, "acme", GitHubAccountType.ORGANIZATION));

    repositorySnapshot =
        new GitHubRepositorySnapshot(
            GITHUB_REPOSITORY_ID,
            "developer-content-creator",
            "acme/developer-content-creator",
            URI.create("https://github.com/acme/developer-content-creator"),
            "main",
            RepositoryVisibility.PRIVATE);
  }

  @Test
  void shouldRegisterRepositoryWhenInstallationIsAuthorizedAndRepositoryIsAccessible() {
    when(installationPort.findByInstallationIdAndUserId(INSTALLATION_ID, USER_ID))
        .thenReturn(Optional.of(installation));

    when(repositoryPort.findByInstallationIdAndRepositoryId(INSTALLATION_ID, GITHUB_REPOSITORY_ID))
        .thenReturn(Optional.of(repositorySnapshot));

    when(existsPort.existsByInstallationIdAndGitHubRepositoryId(
            INSTALLATION_ID, GITHUB_REPOSITORY_ID))
        .thenReturn(false);

    when(savePort.save(any(RegisteredRepository.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RegisteredRepository result = service.register(command);

    assertThat(result).isNotNull();
    assertThat(result.id()).isNotNull();
    assertThat(result.githubRepositoryId()).isEqualTo(GITHUB_REPOSITORY_ID);
    assertThat(result.installationId()).isEqualTo(INSTALLATION_ID);
    assertThat(result.name()).isEqualTo("developer-content-creator");
    assertThat(result.fullName()).isEqualTo("acme/developer-content-creator");
    assertThat(result.htmlUrl())
        .isEqualTo(URI.create("https://github.com/acme/developer-content-creator"));
    assertThat(result.defaultBranch()).contains("main");
    assertThat(result.visibility()).isEqualTo(RepositoryVisibility.PRIVATE);

    InOrder order = inOrder(installationPort, repositoryPort, existsPort, savePort);

    order.verify(installationPort).findByInstallationIdAndUserId(INSTALLATION_ID, USER_ID);

    order
        .verify(repositoryPort)
        .findByInstallationIdAndRepositoryId(INSTALLATION_ID, GITHUB_REPOSITORY_ID);

    order
        .verify(existsPort)
        .existsByInstallationIdAndGitHubRepositoryId(INSTALLATION_ID, GITHUB_REPOSITORY_ID);

    order.verify(savePort).save(any(RegisteredRepository.class));
  }

  @Test
  void shouldRejectRegistrationWhenInstallationIsNotAuthorized() {
    when(installationPort.findByInstallationIdAndUserId(INSTALLATION_ID, USER_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.register(command))
        .isInstanceOf(GitHubInstallationNotFoundException.class);

    verifyNoInteractions(repositoryPort, existsPort, savePort);
  }

  @Test
  void shouldRejectRegistrationWhenRepositoryIsNotAccessible() {
    when(installationPort.findByInstallationIdAndUserId(INSTALLATION_ID, USER_ID))
        .thenReturn(Optional.of(installation));

    when(repositoryPort.findByInstallationIdAndRepositoryId(INSTALLATION_ID, GITHUB_REPOSITORY_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.register(command))
        .isInstanceOf(GitHubRepositoryNotAccessibleException.class);

    verify(existsPort, never()).existsByInstallationIdAndGitHubRepositoryId(any(), any());

    verifyNoInteractions(savePort);
  }

  @Test
  void shouldRejectRegistrationWhenRepositoryIsAlreadyRegistered() {
    when(installationPort.findByInstallationIdAndUserId(INSTALLATION_ID, USER_ID))
        .thenReturn(Optional.of(installation));

    when(repositoryPort.findByInstallationIdAndRepositoryId(INSTALLATION_ID, GITHUB_REPOSITORY_ID))
        .thenReturn(Optional.of(repositorySnapshot));

    when(existsPort.existsByInstallationIdAndGitHubRepositoryId(
            INSTALLATION_ID, GITHUB_REPOSITORY_ID))
        .thenReturn(true);

    assertThatThrownBy(() -> service.register(command))
        .isInstanceOf(RepositoryAlreadyRegisteredException.class);

    verify(savePort, never()).save(any(RegisteredRepository.class));
  }

  @Test
  void shouldPropagatePersistenceFailure() {
    RuntimeException persistenceException = new RuntimeException("Database unavailable");

    when(installationPort.findByInstallationIdAndUserId(INSTALLATION_ID, USER_ID))
        .thenReturn(Optional.of(installation));

    when(repositoryPort.findByInstallationIdAndRepositoryId(INSTALLATION_ID, GITHUB_REPOSITORY_ID))
        .thenReturn(Optional.of(repositorySnapshot));

    when(existsPort.existsByInstallationIdAndGitHubRepositoryId(
            INSTALLATION_ID, GITHUB_REPOSITORY_ID))
        .thenReturn(false);

    when(savePort.save(any(RegisteredRepository.class))).thenThrow(persistenceException);

    assertThatThrownBy(() -> service.register(command)).isSameAs(persistenceException);
  }

  @Test
  void shouldGenerateNewInternalRepositoryId() {
    when(installationPort.findByInstallationIdAndUserId(INSTALLATION_ID, USER_ID))
        .thenReturn(Optional.of(installation));

    when(repositoryPort.findByInstallationIdAndRepositoryId(INSTALLATION_ID, GITHUB_REPOSITORY_ID))
        .thenReturn(Optional.of(repositorySnapshot));

    when(existsPort.existsByInstallationIdAndGitHubRepositoryId(
            INSTALLATION_ID, GITHUB_REPOSITORY_ID))
        .thenReturn(false);

    when(savePort.save(any(RegisteredRepository.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ArgumentCaptor<RegisteredRepository> captor =
        ArgumentCaptor.forClass(RegisteredRepository.class);

    service.register(command);

    verify(savePort).save(captor.capture());

    assertThat(captor.getValue().id()).isNotNull();
    assertThat(captor.getValue().id()).isInstanceOf(UUID.class);
  }
}
