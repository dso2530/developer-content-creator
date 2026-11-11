package io.developercontentcreator.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.developercontentcreator.security.config.SecurityProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SpringCurrentUserProviderTest {

  private static final String USER_ID_CLAIM = "dcc_user_id";

  private SecurityProperties properties;
  private SpringCurrentUserProvider provider;

  @BeforeEach
  void setUp() {
    properties = new SecurityProperties();
    provider = new SpringCurrentUserProvider(properties);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldExtractAuthenticatedUserFromJwt() {
    UUID userId = UUID.randomUUID();

    Jwt jwt =
        createJwt(
            Map.of(
                USER_ID_CLAIM,
                userId.toString(),
                "preferred_username",
                "developer",
                "roles",
                List.of("ROLE_USER", "REPOSITORY_READ")));

    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "test-user"));

    AuthenticatedUser user = provider.getCurrentUser();

    assertThat(user.userId()).isEqualTo(userId);
    assertThat(user.subject()).contains("github|12345");
    assertThat(user.username()).contains("developer");
    assertThat(user.authorities()).containsExactlyInAnyOrder("ROLE_USER", "REPOSITORY_READ");
  }

  @Test
  void shouldAllowOptionalClaimsToBeAbsent() {
    UUID userId = UUID.randomUUID();

    Jwt jwt = createJwt(Map.of(USER_ID_CLAIM, userId.toString()));

    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "test-user"));

    AuthenticatedUser user = provider.getCurrentUser();

    assertThat(user.userId()).isEqualTo(userId);
    assertThat(user.username()).isEmpty();
    assertThat(user.authorities()).isEmpty();
  }

  @Test
  void shouldRejectMissingAuthentication() {
    assertThatThrownBy(provider::getCurrentUser).isInstanceOf(UnauthenticatedUserException.class);
  }

  @Test
  void shouldRejectNonJwtAuthentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("user", "password", List.of()));

    assertThatThrownBy(provider::getCurrentUser).isInstanceOf(UnauthenticatedUserException.class);
  }

  @Test
  void shouldRejectMissingInternalUserIdClaim() {
    Jwt jwt = createJwt(Map.of("sub", "12345"));

    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "test-user"));

    assertThatThrownBy(provider::getCurrentUser)
        .isInstanceOf(InvalidUserClaimException.class)
        .hasMessageContaining(USER_ID_CLAIM);
  }

  @Test
  void shouldRejectInvalidInternalUserIdClaim() {
    Jwt jwt = createJwt(Map.of(USER_ID_CLAIM, "not-a-uuid"));

    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "test-user"));

    assertThatThrownBy(provider::getCurrentUser)
        .isInstanceOf(InvalidUserClaimException.class)
        .hasMessageContaining("valid UUID");
  }

  @Test
  void shouldRejectMalformedAuthoritiesClaim() {
    Jwt jwt = createJwt(Map.of(USER_ID_CLAIM, UUID.randomUUID().toString(), "roles", "ADMIN"));

    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "test-user"));

    assertThatThrownBy(provider::getCurrentUser)
        .isInstanceOf(InvalidUserClaimException.class)
        .hasMessageContaining("collection of strings");
  }

  @Test
  void shouldRejectNonStringAuthority() {
    Jwt jwt =
        createJwt(
            Map.of(
                USER_ID_CLAIM, UUID.randomUUID().toString(), "roles", List.of("ROLE_USER", 123)));

    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "test-user"));

    assertThatThrownBy(provider::getCurrentUser)
        .isInstanceOf(InvalidUserClaimException.class)
        .hasMessageContaining("invalid authority");
  }

  @Test
  void shouldUseConfiguredClaimNames() {
    properties.setUserIdClaim("internal_user_id");
    properties.setUsernameClaim("email");
    properties.setAuthoritiesClaim("permissions");

    UUID userId = UUID.randomUUID();

    Jwt jwt =
        createJwt(
            Map.of(
                "internal_user_id",
                userId.toString(),
                "email",
                "developer@example.test",
                "permissions",
                List.of("REPOSITORY_READ")));

    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "test-user"));

    AuthenticatedUser user = provider.getCurrentUser();

    assertThat(user.userId()).isEqualTo(userId);
    assertThat(user.username()).contains("developer@example.test");
    assertThat(user.authorities()).containsExactly("REPOSITORY_READ");
  }

  private Jwt createJwt(Map<String, Object> claims) {
    return Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .subject("github|12345")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .claims(existingClaims -> existingClaims.putAll(claims))
        .build();
  }
}
