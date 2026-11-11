package io.developercontentcreator.repository.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public record GitHubRepositoryResponse(
    Long id,
    String name,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("html_url") URI htmlUrl,
    @JsonProperty("default_branch") String defaultBranch,
    String visibility) {}
