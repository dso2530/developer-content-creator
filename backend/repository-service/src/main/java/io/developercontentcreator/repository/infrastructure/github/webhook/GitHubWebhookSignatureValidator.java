package io.developercontentcreator.repository.infrastructure.github.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class GitHubWebhookSignatureValidator {

  private static final String SIGNATURE_PREFIX = "sha256=";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int SHA256_SIGNATURE_HEX_LENGTH = 64;

  private final byte[] secret;

  public GitHubWebhookSignatureValidator(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("GitHub webhook secret must not be blank.");
    }

    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  public boolean isValid(byte[] payload, String signatureHeader) {
    if (payload == null || signatureHeader == null) {
      return false;
    }

    if (!signatureHeader.startsWith(SIGNATURE_PREFIX)) {
      return false;
    }

    String providedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());

    if (providedHex.length() != SHA256_SIGNATURE_HEX_LENGTH) {
      return false;
    }

    final byte[] providedSignature;

    try {
      providedSignature = HexFormat.of().parseHex(providedHex);
    } catch (IllegalArgumentException exception) {
      return false;
    }

    byte[] expectedSignature = calculateHmac(payload);

    return MessageDigest.isEqual(expectedSignature, providedSignature);
  }

  private byte[] calculateHmac(byte[] payload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
      return mac.doFinal(payload);
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to calculate GitHub webhook signature.", exception);
    }
  }
}
