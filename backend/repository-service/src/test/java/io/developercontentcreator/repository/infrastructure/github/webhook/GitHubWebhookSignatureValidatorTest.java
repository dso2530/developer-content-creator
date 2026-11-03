package io.developercontentcreator.repository.infrastructure.github.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitHubWebhookSignatureValidatorTest {

  private static final String SECRET = "key";
  private static final String PAYLOAD = "The quick brown fox jumps over the lazy dog";
  private static final String VALID_SIGNATURE =
      "sha256=f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8";

  private GitHubWebhookSignatureValidator validator;

  @BeforeEach
  void setUp() {
    validator = new GitHubWebhookSignatureValidator(SECRET);
  }

  @Test
  void shouldAcceptValidSignature() {
    assertThat(validator.isValid(payload(), VALID_SIGNATURE)).isTrue();
  }

  @Test
  void shouldRejectModifiedPayload() {
    byte[] modifiedPayload =
        "The quick brown fox jumps over the lazy dog!".getBytes(StandardCharsets.UTF_8);

    assertThat(validator.isValid(modifiedPayload, VALID_SIGNATURE)).isFalse();
  }

  @Test
  void shouldRejectSignatureGeneratedWithDifferentSecret() {
    GitHubWebhookSignatureValidator otherValidator =
        new GitHubWebhookSignatureValidator("another-secret");

    assertThat(otherValidator.isValid(payload(), VALID_SIGNATURE)).isFalse();
  }

  @Test
  void shouldRejectNullPayload() {
    assertThat(validator.isValid(null, VALID_SIGNATURE)).isFalse();
  }

  @Test
  void shouldRejectNullSignature() {
    assertThat(validator.isValid(payload(), null)).isFalse();
  }

  @Test
  void shouldRejectMissingSha256Prefix() {
    assertThat(validator.isValid(payload(), VALID_SIGNATURE.substring(7))).isFalse();
  }

  @Test
  void shouldRejectInvalidPrefix() {
    assertThat(validator.isValid(payload(), "sha1=" + VALID_SIGNATURE.substring(7))).isFalse();
  }

  @Test
  void shouldRejectShortSignature() {
    assertThat(validator.isValid(payload(), "sha256=abcd")).isFalse();
  }

  @Test
  void shouldRejectNonHexadecimalSignature() {
    String invalidSignature = "sha256=" + "z".repeat(64);

    assertThat(validator.isValid(payload(), invalidSignature)).isFalse();
  }

  @Test
  void shouldRejectBlankSecret() {
    assertThatThrownBy(() -> new GitHubWebhookSignatureValidator(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullSecret() {
    assertThatThrownBy(() -> new GitHubWebhookSignatureValidator(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAcceptEmptyPayloadWithValidSignature() {
    String signature = "sha256=5d5d139563c95b5967b9bd9a8c9b233a9dedb45072794cd232dc1b74832607d0";

    assertThat(validator.isValid(new byte[0], signature)).isTrue();
  }

  @Test
  void shouldAcceptUppercaseHexadecimalSignature() {
    assertThat(
            validator.isValid(
                payload(), VALID_SIGNATURE.toUpperCase().replace("SHA256=", "sha256=")))
        .isTrue();
  }

  @Test
  void shouldRejectSignatureWithTrailingWhitespace() {
    assertThat(validator.isValid(payload(), VALID_SIGNATURE + " ")).isFalse();
  }

  @Test
  void shouldRejectEmptySignature() {
    assertThat(validator.isValid(payload(), "")).isFalse();
  }

  private byte[] payload() {
    return PAYLOAD.getBytes(StandardCharsets.UTF_8);
  }
}
