package io.developercontentcreator.logging.masking;

import java.util.regex.Pattern;

/** Centralized regular expressions used to detect sensitive information. */
public final class SensitiveDataPatterns {

  /**
   * Matches sensitive key-value assignments in JSON, YAML and properties-like messages.
   *
   * <p>Examples: password=secret "client_secret": "secret value" access_token: abc123
   */
  public static final Pattern SECRET_KEY_VALUE =
      Pattern.compile(
          "(?i)([\"']?(?:"
              + "password|passwd|pwd|"
              + "client[_-]?secret|"
              + "api[_-]?key|"
              + "access[_-]?token|"
              + "refresh[_-]?token|"
              + "id[_-]?token|"
              + "auth[_-]?token|"
              + "secret|"
              + "cookie|"
              + "set-cookie|"
              + "private[_-]?key"
              + ")[\"']?\\s*[:=]\\s*)"
              + "(?:([\"'])([^\\r\\n]*?)\\2|([^\\s,;}]*))");

  /**
   * Matches authorization values while preserving the authentication scheme.
   *
   * <p>Examples: Authorization: Bearer token "authorization": "Bearer token"
   */
  public static final Pattern AUTHORIZATION_VALUE =
      Pattern.compile(
          "(?i)([\"']?authorization[\"']?\\s*[:=]\\s*)"
              + "([\"']?)"
              + "(?:(Bearer|Basic)([\\t ]+))?"
              + "([^\\r\\n,;}\"']*?)"
              + "([\"']?)"
              + "(?=[\\t ]*[,;}\\r\\n]|$)");

  /** Matches JWT-like compact tokens containing three Base64URL segments. */
  public static final Pattern JWT =
      Pattern.compile(
          "(?<![A-Za-z0-9_-])"
              + "[A-Za-z0-9_-]{8,}\\."
              + "[A-Za-z0-9_-]{8,}\\."
              + "[A-Za-z0-9_-]{8,}"
              + "(?![A-Za-z0-9_-])");

  /** Matches common email address formats. */
  public static final Pattern EMAIL =
      Pattern.compile(
          "(?i)(?<![A-Z0-9._%+-])"
              + "[A-Z0-9._%+-]+"
              + "@"
              + "[A-Z0-9.-]+\\.[A-Z]{2,}"
              + "(?![A-Z0-9._%+-])");

  /**
   * Matches common international and national phone number formats.
   *
   * <p>Requires at least eight digits to reduce false positives.
   */
  public static final Pattern PHONE =
      Pattern.compile(
          "(?<!\\w)" + "(?=(?:\\D*\\d){8,})" + "\\+?\\d[\\d\\s().-]{6,}\\d" + "(?!\\w)");

  private SensitiveDataPatterns() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }
}
