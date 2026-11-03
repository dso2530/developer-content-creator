package io.developercontentcreator.security.authentication;

import io.developercontentcreator.security.config.SecurityProperties;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class SpringCurrentUserProvider implements CurrentUserProvider {

  private final SecurityProperties properties;

  public SpringCurrentUserProvider(SecurityProperties properties) {
    this.properties = properties;
  }

  @Override
  public AuthenticatedUser getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
        || !jwtAuthentication.isAuthenticated()) {
      throw new UnauthenticatedUserException(
          "No authenticated JWT user is available in the security context");
    }

    var jwt = jwtAuthentication.getToken();

    UUID userId = extractUserId(jwt.getClaim(properties.getUserIdClaim()));

    Optional<String> subject = Optional.ofNullable(jwt.getSubject());

    Optional<String> username =
        Optional.ofNullable(jwt.getClaimAsString(properties.getUsernameClaim()));

    Set<String> authorities = extractAuthorities(jwt.getClaim(properties.getAuthoritiesClaim()));

    return new AuthenticatedUser(userId, subject, username, authorities);
  }

  private UUID extractUserId(Object claim) {
    if (!(claim instanceof String value) || value.isBlank()) {
      throw new InvalidUserClaimException(
          "Required JWT claim '%s' is missing or invalid".formatted(properties.getUserIdClaim()));
    }

    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new InvalidUserClaimException(
          "JWT claim '%s' must contain a valid UUID".formatted(properties.getUserIdClaim()),
          exception);
    }
  }

  private Set<String> extractAuthorities(Object claim) {
    if (claim == null) {
      return Set.of();
    }

    if (!(claim instanceof Collection<?> values)) {
      throw new InvalidUserClaimException(
          "JWT claim '%s' must be a collection of strings"
              .formatted(properties.getAuthoritiesClaim()));
    }

    Set<String> authorities = new LinkedHashSet<>();

    for (Object value : values) {
      if (!(value instanceof String authority) || authority.isBlank()) {
        throw new InvalidUserClaimException(
            "JWT claim '%s' contains an invalid authority"
                .formatted(properties.getAuthoritiesClaim()));
      }

      authorities.add(authority);
    }

    return Set.copyOf(authorities);
  }
}
