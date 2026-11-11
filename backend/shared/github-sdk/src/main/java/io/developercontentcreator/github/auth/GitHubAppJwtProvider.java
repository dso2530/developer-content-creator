package io.developercontentcreator.github.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.developercontentcreator.github.config.GitHubAppProperties;
import io.developercontentcreator.github.exception.GitHubAuthenticationException;
import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

public class GitHubAppJwtProvider {

  private final Long appId;
  private final java.time.Duration jwtValidity;
  private final PrivateKey privateKey;
  private final Clock clock;

  public GitHubAppJwtProvider(
      GitHubAppProperties properties, GitHubPrivateKeyLoader privateKeyLoader) {
    this(properties, privateKeyLoader, Clock.systemUTC());
  }

  GitHubAppJwtProvider(
      GitHubAppProperties properties, GitHubPrivateKeyLoader privateKeyLoader, Clock clock) {

    this.appId = properties.getAppId();
    this.jwtValidity = properties.getJwtValidity();
    this.privateKey = privateKeyLoader.load(properties.getPrivateKeyPath());
    this.clock = clock;
  }

  public String generateToken() {
    Instant now = clock.instant();

    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(appId.toString())
            .issueTime(Date.from(now.minusSeconds(60)))
            .expirationTime(Date.from(now.plus(jwtValidity)))
            .build();

    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();

    SignedJWT signedJWT = new SignedJWT(header, claims);

    try {
      signedJWT.sign(new RSASSASigner(privateKey));
      return signedJWT.serialize();
    } catch (JOSEException exception) {
      throw new GitHubAuthenticationException("Unable to generate GitHub App JWT.", exception);
    }
  }
}
