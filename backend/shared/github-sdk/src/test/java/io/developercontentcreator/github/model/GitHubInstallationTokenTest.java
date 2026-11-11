package io.developercontentcreator.github.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitHubInstallationTokenTest {

  private static final String SECRET_TOKEN = "ghs-sensitive-token";

  private Instant now;

  @BeforeEach
  void setUp() {
    now = Instant.parse("2026-11-02T10:00:00Z");
  }

  @Test
  void shouldCreateInstallationToken() {
    GitHubInstallationToken token =
        new GitHubInstallationToken(
            SECRET_TOKEN, now.plus(Duration.ofHours(1)), Map.of("contents", "read"), "all");

    assertEquals(SECRET_TOKEN, token.getToken());
    assertEquals(now.plus(Duration.ofHours(1)), token.getExpiresAt());
    assertEquals(Map.of("contents", "read"), token.getPermissions());
    assertEquals("all", token.getRepositorySelection());
  }

  @Test
  void shouldRejectBlankToken() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GitHubInstallationToken(" ", now, Map.of(), "all"));
  }

  @Test
  void shouldRejectNullExpiration() {
    assertThrows(
        NullPointerException.class,
        () -> new GitHubInstallationToken(SECRET_TOKEN, null, Map.of(), "all"));
  }

  @Test
  void shouldUseEmptyPermissionsWhenNull() {
    GitHubInstallationToken token =
        new GitHubInstallationToken(SECRET_TOKEN, now.plusSeconds(3600), null, "all");

    assertTrue(token.getPermissions().isEmpty());
  }

  @Test
  void shouldExposeImmutablePermissions() {
    Map<String, String> permissions = new HashMap<>();
    permissions.put("contents", "read");

    GitHubInstallationToken token =
        new GitHubInstallationToken(SECRET_TOKEN, now.plusSeconds(3600), permissions, "all");

    permissions.put("issues", "write");

    assertEquals(1, token.getPermissions().size());

    assertThrows(
        UnsupportedOperationException.class, () -> token.getPermissions().put("issues", "write"));
  }

  @Test
  void shouldBeUsableWhenExpirationExceedsRefreshMargin() {
    GitHubInstallationToken token =
        new GitHubInstallationToken(SECRET_TOKEN, now.plusSeconds(600), Map.of(), "all");

    assertTrue(token.isUsableAt(now, Duration.ofMinutes(5)));
  }

  @Test
  void shouldNotBeUsableWhenExpirationIsWithinRefreshMargin() {
    GitHubInstallationToken token =
        new GitHubInstallationToken(SECRET_TOKEN, now.plusSeconds(120), Map.of(), "all");

    assertFalse(token.isUsableAt(now, Duration.ofMinutes(5)));
  }

  @Test
  void shouldNotBeUsableAtExactRefreshBoundary() {
    GitHubInstallationToken token =
        new GitHubInstallationToken(SECRET_TOKEN, now.plusSeconds(300), Map.of(), "all");

    assertFalse(token.isUsableAt(now, Duration.ofMinutes(5)));
  }

  @Test
  void shouldRejectNegativeRefreshMargin() {
    GitHubInstallationToken token =
        new GitHubInstallationToken(SECRET_TOKEN, now.plusSeconds(600), Map.of(), "all");

    assertThrows(
        IllegalArgumentException.class, () -> token.isUsableAt(now, Duration.ofSeconds(-1)));
  }

  @Test
  void shouldRejectNullArgumentsInUsabilityCheck() {
    GitHubInstallationToken token =
        new GitHubInstallationToken(SECRET_TOKEN, now.plusSeconds(600), Map.of(), "all");

    assertThrows(NullPointerException.class, () -> token.isUsableAt(null, Duration.ofMinutes(5)));

    assertThrows(NullPointerException.class, () -> token.isUsableAt(now, null));
  }

  @Test
  void shouldNeverExposeTokenInToString() {
    GitHubInstallationToken token =
        new GitHubInstallationToken(
            SECRET_TOKEN, now.plusSeconds(600), Map.of("contents", "read"), "all");

    String representation = token.toString();

    assertFalse(representation.contains(SECRET_TOKEN));
    assertTrue(representation.contains("REDACTED"));
  }

  @Test
  void shouldCheckUsabilityUsingCurrentTime() {
    GitHubInstallationToken token =
        new GitHubInstallationToken(
            SECRET_TOKEN, Instant.now().plus(Duration.ofHours(1)), Map.of(), "all");

    assertTrue(token.isUsable(Duration.ofMinutes(5)));
  }
}
