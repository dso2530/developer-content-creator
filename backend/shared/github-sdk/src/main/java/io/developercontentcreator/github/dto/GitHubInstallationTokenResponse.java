package io.developercontentcreator.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public record GitHubInstallationTokenResponse(
    String token,
    @JsonProperty("expires_at") Instant expiresAt,
    Map<String, String> permissions,
    @JsonProperty("repository_selection") String repositorySelection) {}
