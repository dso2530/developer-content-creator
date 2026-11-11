package io.developercontentcreator.logging.masking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SensitiveDataPatternsTest {

  @Test
  void shouldRecognizeSensitiveKeyValueAssignments() {
    assertTrue(SensitiveDataPatterns.SECRET_KEY_VALUE.matcher("password=secret123").find());

    assertTrue(
        SensitiveDataPatterns.SECRET_KEY_VALUE.matcher("\"client_secret\": \"secret123\"").find());

    assertTrue(SensitiveDataPatterns.SECRET_KEY_VALUE.matcher("access_token: abc123").find());

    assertTrue(SensitiveDataPatterns.SECRET_KEY_VALUE.matcher("api-key=abc123").find());

    assertTrue(SensitiveDataPatterns.SECRET_KEY_VALUE.matcher("refresh_token=abc123").find());
  }

  @Test
  void shouldNotRecognizeOrdinaryKeysAsSensitive() {
    assertFalse(SensitiveDataPatterns.SECRET_KEY_VALUE.matcher("username=john").find());

    assertFalse(
        SensitiveDataPatterns.SECRET_KEY_VALUE
            .matcher("application_name=repository-service")
            .find());
  }

  @Test
  void shouldRecognizeJwtLikeTokens() {
    String jwt =
        "eyJhbGciOiJIUzI1NiJ9."
            + "eyJzdWIiOiIxMjM0NTY3ODkwIn0."
            + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    assertTrue(SensitiveDataPatterns.JWT.matcher(jwt).find());
  }

  @Test
  void shouldNotRecognizeOrdinaryTextAsJwt() {
    assertFalse(SensitiveDataPatterns.JWT.matcher("repository-service-started").find());

    assertFalse(SensitiveDataPatterns.JWT.matcher("one.two").find());
  }

  @Test
  void shouldRecognizeEmailAddresses() {
    assertTrue(SensitiveDataPatterns.EMAIL.matcher("Contact: user@example.com").find());

    assertTrue(SensitiveDataPatterns.EMAIL.matcher("admin.user+test@example.fr").find());
  }

  @Test
  void shouldNotRecognizeInvalidEmailAddresses() {
    assertFalse(SensitiveDataPatterns.EMAIL.matcher("not-an-email").find());

    assertFalse(SensitiveDataPatterns.EMAIL.matcher("user@example").find());
  }

  @Test
  void shouldRecognizePhoneNumbers() {
    assertTrue(SensitiveDataPatterns.PHONE.matcher("+33 6 12 34 56 78").find());

    assertTrue(SensitiveDataPatterns.PHONE.matcher("06 12 34 56 78").find());
  }

  @Test
  void shouldNotRecognizeShortNumbersAsPhoneNumbers() {
    assertFalse(SensitiveDataPatterns.PHONE.matcher("12345").find());
  }
}
