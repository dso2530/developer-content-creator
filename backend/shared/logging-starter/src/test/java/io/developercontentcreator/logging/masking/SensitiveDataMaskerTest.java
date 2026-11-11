package io.developercontentcreator.logging.masking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

  private MaskingProperties properties;
  private SensitiveDataMasker masker;

  @BeforeEach
  void setUp() {
    properties = new MaskingProperties();
    masker = new SensitiveDataMasker(properties);
  }

  @Test
  void shouldReturnNullWhenInputIsNull() {
    assertNull(masker.mask(null));
  }

  @Test
  void shouldReturnEmptyInputUnchanged() {
    assertEquals("", masker.mask(""));
  }

  @Test
  void shouldMaskPassword() {
    assertEquals("password=[REDACTED]", masker.mask("password=secret123"));
  }

  @Test
  void shouldMaskQuotedPasswordContainingSpaces() {
    assertEquals("password=\"[REDACTED]\"", masker.mask("password=\"my secret value\""));
  }

  @Test
  void shouldMaskSingleQuotedPasswordContainingSpaces() {
    assertEquals("password='[REDACTED]'", masker.mask("password='my secret value'"));
  }

  @Test
  void shouldMaskJsonClientSecret() {
    assertEquals(
        "\"client_secret\": \"[REDACTED]\"", masker.mask("\"client_secret\": \"secret123\""));
  }

  @Test
  void shouldMaskJsonSecretContainingSpaces() {
    assertEquals(
        "\"client_secret\": \"[REDACTED]\"", masker.mask("\"client_secret\": \"my secret value\""));
  }

  @Test
  void shouldMaskYamlAuthorization() {
    assertEquals(
        "authorization: [REDACTED]", masker.mask("authorization: Bearer confidential-value"));
  }

  @Test
  void shouldMaskCookie() {
    assertEquals("cookie=[REDACTED]", masker.mask("cookie=session-confidential-value"));
  }

  @Test
  void shouldMaskSetCookie() {
    assertEquals("set-cookie=[REDACTED]", masker.mask("set-cookie=session-confidential-value"));
  }

  @Test
  void shouldMaskPrivateKey() {
    assertEquals("private_key=[REDACTED]", masker.mask("private_key=confidential-value"));
  }

  @Test
  void shouldMaskHyphenatedPrivateKey() {
    assertEquals("private-key=[REDACTED]", masker.mask("private-key=confidential-value"));
  }

  @Test
  void shouldMaskEmptySensitiveValue() {
    assertEquals("password=[REDACTED]", masker.mask("password="));
  }

  @Test
  void shouldPreserveJsonStructureWhenMaskingSecret() {
    assertEquals(
        "{\"password\":\"[REDACTED]\",\"enabled\":true}",
        masker.mask("{\"password\":\"secret123\",\"enabled\":true}"));
  }

  @Test
  void shouldMaskJwt() {
    String jwt =
        "eyJhbGciOiJIUzI1NiJ9."
            + "eyJzdWIiOiIxMjM0NTY3ODkwIn0."
            + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    assertEquals("Authorization: Bearer [REDACTED]", masker.mask("Authorization: Bearer " + jwt));
  }

  @Test
  void shouldMaskEmail() {
    assertEquals("Contact: [REDACTED]", masker.mask("Contact: user@example.com"));
  }

  @Test
  void shouldMaskPhone() {
    assertEquals("Phone: [REDACTED]", masker.mask("Phone: +33 6 12 34 56 78"));
  }

  @Test
  void shouldPreserveEmailWhenEmailMaskingIsDisabled() {
    properties.setMaskEmail(false);

    assertEquals("Contact: user@example.com", masker.mask("Contact: user@example.com"));
  }

  @Test
  void shouldPreserveJwtWhenJwtMaskingIsDisabled() {
    properties.setMaskJwt(false);

    String jwt =
        "eyJhbGciOiJIUzI1NiJ9."
            + "eyJzdWIiOiIxMjM0NTY3ODkwIn0."
            + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    assertEquals(jwt, masker.mask(jwt));
  }

  @Test
  void shouldPreservePhoneWhenPhoneMaskingIsDisabled() {
    properties.setMaskPhone(false);

    assertEquals("Phone: +33 6 12 34 56 78", masker.mask("Phone: +33 6 12 34 56 78"));
  }

  @Test
  void shouldAlwaysMaskNamedSecretsWhenGloballyEnabled() {
    properties.setMaskJwt(false);
    properties.setMaskEmail(false);
    properties.setMaskPhone(false);

    assertEquals("password=[REDACTED]", masker.mask("password=secret123"));
  }

  @Test
  void shouldPreserveInputWhenGloballyDisabled() {
    properties.setEnabled(false);

    String input = "password=secret123 user@example.com";

    assertEquals(input, masker.mask(input));
  }

  @Test
  void shouldUseCustomReplacement() {
    properties.setReplacement("***");

    assertEquals("password=***", masker.mask("password=secret123"));
  }

  @Test
  void shouldPreserveCustomReplacementInsideQuotes() {
    properties.setReplacement("***");

    assertEquals("password=\"***\"", masker.mask("password=\"my secret value\""));
  }

  @Test
  void shouldPreserveOrdinaryText() {
    String input = "Repository analysis completed successfully";

    assertEquals(input, masker.mask(input));
  }

  @Test
  void shouldPreserveOrdinaryKeyValuePairs() {
    String input = "username=john application_name=repository-service";

    assertEquals(input, masker.mask(input));
  }
}
