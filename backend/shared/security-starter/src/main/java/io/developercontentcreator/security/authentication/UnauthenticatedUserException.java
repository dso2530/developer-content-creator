package io.developercontentcreator.security.authentication;

public class UnauthenticatedUserException extends RuntimeException {

  public UnauthenticatedUserException(String message) {
    super(message);
  }
}
