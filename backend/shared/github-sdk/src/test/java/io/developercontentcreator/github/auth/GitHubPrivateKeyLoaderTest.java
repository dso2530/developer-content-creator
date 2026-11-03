package io.developercontentcreator.github.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.developercontentcreator.github.exception.GitHubAuthenticationException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Base64;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitHubPrivateKeyLoaderTest {

  @TempDir Path tempDir;

  private GitHubPrivateKeyLoader loader;

  @BeforeEach
  void setUp() {
    loader = new GitHubPrivateKeyLoader();
  }

  @Test
  void shouldLoadValidRsaPrivateKey() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);

    KeyPair keyPair = generator.generateKeyPair();

    Path privateKeyPath = tempDir.resolve("github-app.pem");

    String encodedKey =
        Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(keyPair.getPrivate().getEncoded());

    String pem = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n";

    Files.writeString(privateKeyPath, pem);

    var loadedKey = loader.load(privateKeyPath);

    assertThat(loadedKey.getAlgorithm()).isEqualTo("RSA");
    assertThat(loadedKey.getEncoded()).isEqualTo(keyPair.getPrivate().getEncoded());
  }

  @Test
  void shouldRejectRelativePath() {
    assertThatThrownBy(() -> loader.load(Path.of("github-app.pem")))
        .isInstanceOf(GitHubAuthenticationException.class);
  }

  @Test
  void shouldRejectMissingFile() {
    Path missingFile = tempDir.resolve("missing.pem");

    assertThatThrownBy(() -> loader.load(missingFile))
        .isInstanceOf(GitHubAuthenticationException.class)
        .hasMessage("Unable to load GitHub App private key.");
  }

  @Test
  void shouldRejectInvalidPem() throws Exception {
    Path invalidFile = tempDir.resolve("invalid.pem");

    Files.writeString(invalidFile, "invalid-pem-content");

    assertThatThrownBy(() -> loader.load(invalidFile))
        .isInstanceOf(GitHubAuthenticationException.class);
  }

  @Test
  void shouldLoadValidPkcs1RsaPrivateKey() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);

    KeyPair keyPair = generator.generateKeyPair();
    RSAPrivateCrtKey originalKey = (RSAPrivateCrtKey) keyPair.getPrivate();

    RSAPrivateKey pkcs1Key =
        new RSAPrivateKey(
            originalKey.getModulus(),
            originalKey.getPublicExponent(),
            originalKey.getPrivateExponent(),
            originalKey.getPrimeP(),
            originalKey.getPrimeQ(),
            originalKey.getPrimeExponentP(),
            originalKey.getPrimeExponentQ(),
            originalKey.getCrtCoefficient());

    String encodedKey =
        Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(pkcs1Key.getEncoded());

    String pem =
        "-----BEGIN RSA PRIVATE KEY-----\n" + encodedKey + "\n-----END RSA PRIVATE KEY-----\n";

    Path privateKeyPath = tempDir.resolve("github-app-pkcs1.pem");
    Files.writeString(privateKeyPath, pem);

    var loadedKey = loader.load(privateKeyPath);

    assertThat(loadedKey.getAlgorithm()).isEqualTo("RSA");
    assertThat(loadedKey).isInstanceOf(RSAPrivateCrtKey.class);

    RSAPrivateCrtKey loadedRsaKey = (RSAPrivateCrtKey) loadedKey;

    assertThat(loadedRsaKey.getModulus()).isEqualTo(originalKey.getModulus());
    assertThat(loadedRsaKey.getPrivateExponent()).isEqualTo(originalKey.getPrivateExponent());
  }
}
