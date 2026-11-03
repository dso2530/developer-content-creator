package io.developercontentcreator.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

  @Test
  void shouldCreateAuthenticatedUser() {
    UUID userId = UUID.randomUUID();

    AuthenticatedUser user =
        new AuthenticatedUser(
            userId,
            Optional.of("github|12345"),
            Optional.of("developer"),
            Set.of("ROLE_USER", "REPOSITORY_READ"));

    assertThat(user.userId()).isEqualTo(userId);
    assertThat(user.subject()).contains("github|12345");
    assertThat(user.username()).contains("developer");
    assertThat(user.authorities()).containsExactlyInAnyOrder("ROLE_USER", "REPOSITORY_READ");
  }

  @Test
  void shouldAllowOptionalClaimsToBeAbsent() {
    AuthenticatedUser user =
        new AuthenticatedUser(UUID.randomUUID(), Optional.empty(), Optional.empty(), Set.of());

    assertThat(user.subject()).isEmpty();
    assertThat(user.username()).isEmpty();
    assertThat(user.authorities()).isEmpty();
  }

  @Test
  void shouldRejectNullUserId() {
    assertThatNullPointerException()
        .isThrownBy(() -> new AuthenticatedUser(null, Optional.empty(), Optional.empty(), Set.of()))
        .withMessage("userId must not be null");
  }

  @Test
  void shouldRejectNullOptionalValues() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> new AuthenticatedUser(UUID.randomUUID(), null, Optional.empty(), Set.of()));

    assertThatNullPointerException()
        .isThrownBy(
            () -> new AuthenticatedUser(UUID.randomUUID(), Optional.empty(), null, Set.of()));
  }

  @Test
  void shouldRejectNullAuthorities() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new AuthenticatedUser(UUID.randomUUID(), Optional.empty(), Optional.empty(), null));
  }

  @Test
  void shouldExposeImmutableAuthorities() {
    AuthenticatedUser user =
        new AuthenticatedUser(
            UUID.randomUUID(), Optional.empty(), Optional.empty(), Set.of("ROLE_USER"));

    assertThat(user.authorities()).containsExactly("ROLE_USER");
    assertThat(user.authorities()).isUnmodifiable();
  }
}
