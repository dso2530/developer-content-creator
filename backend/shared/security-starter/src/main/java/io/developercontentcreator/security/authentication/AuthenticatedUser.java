package io.developercontentcreator.security.authentication;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
    UUID userId, Optional<String> subject, Optional<String> username, Set<String> authorities) {

  public AuthenticatedUser {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(subject, "subject must not be null");
    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(authorities, "authorities must not be null");

    authorities = Set.copyOf(authorities);
  }
}
