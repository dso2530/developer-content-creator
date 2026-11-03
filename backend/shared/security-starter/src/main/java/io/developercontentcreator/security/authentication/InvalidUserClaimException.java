package io.developercontentcreator.security.authentication;

public class InvalidUserClaimException extends RuntimeException {

  public InvalidUserClaimException(String message) {
    super(message);
  }

  public InvalidUserClaimException(String message, Throwable cause) {
    super(message, cause);
  }
}
