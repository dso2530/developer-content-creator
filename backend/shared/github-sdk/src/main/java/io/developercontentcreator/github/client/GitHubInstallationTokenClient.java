package io.developercontentcreator.github.client;

import io.developercontentcreator.github.model.GitHubInstallationToken;

public interface GitHubInstallationTokenClient {

  GitHubInstallationToken createForInstallation(Long installationId);
}
