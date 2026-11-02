package io.developercontentcreator.repository.infrastructure.persistence.entity;

import io.developercontentcreator.repository.domain.model.GitHubAccount;
import io.developercontentcreator.repository.domain.model.GitHubAccountType;
import io.developercontentcreator.repository.domain.model.GitHubInstallation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "github_installations")
public class GitHubInstallationEntity {

  @Id
  @Column(name = "installation_id", nullable = false)
  private Long installationId;

  @Column(name = "github_account_id", nullable = false)
  private Long githubAccountId;

  @Column(name = "account_login", nullable = false, length = 255)
  private String accountLogin;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false, length = 32)
  private GitHubAccountType accountType;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  protected GitHubInstallationEntity() {
    // Required by JPA.
  }

  public static GitHubInstallationEntity fromDomain(GitHubInstallation installation, UUID userId) {

    GitHubInstallationEntity entity = new GitHubInstallationEntity();

    entity.installationId = installation.installationId();
    entity.githubAccountId = installation.account().githubId();
    entity.accountLogin = installation.account().login();
    entity.accountType = installation.account().type();
    entity.userId = userId;

    return entity;
  }

  public GitHubInstallation toDomain() {
    GitHubAccount account = new GitHubAccount(githubAccountId, accountLogin, accountType);

    return new GitHubInstallation(installationId, account);
  }
}
