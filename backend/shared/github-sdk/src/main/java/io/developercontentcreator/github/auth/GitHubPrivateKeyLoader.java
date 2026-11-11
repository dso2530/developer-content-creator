package io.developercontentcreator.github.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import io.developercontentcreator.github.exception.GitHubAuthenticationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;

public class GitHubPrivateKeyLoader {

  public PrivateKey load(Path privateKeyPath) {
    if (privateKeyPath == null || !privateKeyPath.isAbsolute()) {
      throw new GitHubAuthenticationException(
          "GitHub App private key requires an absolute file path.");
    }

    try {
      String pem = Files.readString(privateKeyPath, StandardCharsets.UTF_8);

      JWK jwk = JWK.parseFromPEMEncodedObjects(pem);

      if (!(jwk instanceof RSAKey rsaKey) || !rsaKey.isPrivate()) {
        throw new GitHubAuthenticationException(
            "GitHub App private key must be a valid RSA private key.");
      }

      return rsaKey.toPrivateKey();

    } catch (IOException | JOSEException exception) {
      throw new GitHubAuthenticationException("Unable to load GitHub App private key.", exception);
    }
  }
}
