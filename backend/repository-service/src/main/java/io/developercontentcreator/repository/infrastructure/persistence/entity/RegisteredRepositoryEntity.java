package io.developercontentcreator.repository.infrastructure.persistence.entity;

import io.developercontentcreator.repository.domain.model.RegisteredRepository;
import io.developercontentcreator.repository.domain.model.RepositoryVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.URI;
import java.util.UUID;

@Entity
@Table(name = "registered_repositories")
public class RegisteredRepositoryEntity {

  @Id private UUID id;

  @Column(name = "github_repository_id", nullable = false)
  private Long githubRepositoryId;

  @Column(name = "installation_id", nullable = false)
  private Long installationId;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(name = "full_name", nullable = false, length = 512)
  private String fullName;

  @Column(name = "html_url", nullable = false, columnDefinition = "TEXT")
  private String htmlUrl;

  @Column(name = "default_branch", length = 255)
  private String defaultBranch;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RepositoryVisibility visibility;

  protected RegisteredRepositoryEntity() {
    // Required by JPA.
  }

  public static RegisteredRepositoryEntity fromDomain(RegisteredRepository repository) {
    RegisteredRepositoryEntity entity = new RegisteredRepositoryEntity();

    entity.id = repository.id();
    entity.githubRepositoryId = repository.githubRepositoryId();
    entity.installationId = repository.installationId();
    entity.name = repository.name();
    entity.fullName = repository.fullName();
    entity.htmlUrl = repository.htmlUrl().toString();
    entity.defaultBranch = repository.defaultBranch().orElse(null);
    entity.visibility = repository.visibility();

    return entity;
  }

  public RegisteredRepository toDomain() {
    return new RegisteredRepository(
        id,
        githubRepositoryId,
        installationId,
        name,
        fullName,
        URI.create(htmlUrl),
        defaultBranch,
        visibility);
  }
}
