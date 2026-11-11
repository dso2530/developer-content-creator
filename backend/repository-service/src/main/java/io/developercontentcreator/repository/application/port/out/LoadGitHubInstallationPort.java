package io.developercontentcreator.repository.application.port.out;

import io.developercontentcreator.repository.domain.model.GitHubInstallation;
import java.util.Optional;
import java.util.UUID;

public interface LoadGitHubInstallationPort {

  Optional<GitHubInstallation> findByInstallationIdAndUserId(Long installationId, UUID userId);
}
