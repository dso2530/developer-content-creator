package io.developercontentcreator.github.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import io.developercontentcreator.github.config.GitHubAppProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitHubAppJwtProviderTest {

  private static final Long APP_ID = 123456L;

  private static final Instant FIXED_INSTANT = Instant.parse("2026-11-02T12:00:00Z");

  @TempDir Path tempDir;

  private GitHubAppJwtProvider provider;
  private KeyPair keyPair;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    keyPair = generator.generateKeyPair();

    Path privateKeyPath = tempDir.resolve("github-app.pem");

    String encodedKey =
        Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(keyPair.getPrivate().getEncoded());

    String pem = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n";

    Files.writeString(privateKeyPath, pem);

    GitHubAppProperties properties = new GitHubAppProperties();
    properties.setAppId(APP_ID);
    properties.setPrivateKeyPath(privateKeyPath);
    properties.setJwtValidity(Duration.ofMinutes(9));
    properties.afterPropertiesSet();

    Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    provider = new GitHubAppJwtProvider(properties, new GitHubPrivateKeyLoader(), clock);
  }

  @Test
  void shouldGenerateValidSignedJwtWithExpectedClaims() throws Exception {
    String token = provider.generateToken();

    SignedJWT signedJWT = SignedJWT.parse(token);

    assertThat(signedJWT.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
    assertThat(signedJWT.getHeader().getType()).isEqualTo(JOSEObjectType.JWT);

    assertThat(signedJWT.verify(new RSASSAVerifier((RSAPublicKey) keyPair.getPublic()))).isTrue();

    var claims = signedJWT.getJWTClaimsSet();

    assertThat(claims.getIssuer()).isEqualTo(APP_ID.toString());

    assertThat(claims.getIssueTime().toInstant()).isEqualTo(FIXED_INSTANT.minusSeconds(60));

    assertThat(claims.getExpirationTime().toInstant())
        .isEqualTo(FIXED_INSTANT.plus(Duration.ofMinutes(9)));
  }

  @Test
  void shouldGenerateDifferentExpirationWhenValidityChanges() throws Exception {
    GitHubAppProperties properties = new GitHubAppProperties();
    properties.setAppId(APP_ID);
    properties.setPrivateKeyPath(tempDir.resolve("github-app.pem"));
    properties.setJwtValidity(Duration.ofMinutes(5));
    properties.afterPropertiesSet();

    GitHubAppJwtProvider fiveMinuteProvider =
        new GitHubAppJwtProvider(
            properties, new GitHubPrivateKeyLoader(), Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

    SignedJWT signedJWT = SignedJWT.parse(fiveMinuteProvider.generateToken());

    assertThat(signedJWT.getJWTClaimsSet().getExpirationTime().toInstant())
        .isEqualTo(FIXED_INSTANT.plus(Duration.ofMinutes(5)));
  }
}
