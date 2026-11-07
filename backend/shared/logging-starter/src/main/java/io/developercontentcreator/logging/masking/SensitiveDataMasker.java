package io.developercontentcreator.logging.masking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/** Masks sensitive information in log messages. */
public class SensitiveDataMasker {

  private static final Pattern HTTP_AUTHORIZATION_PREFIX =
      Pattern.compile("^Authorization\\s*:\\s*$");

  private final MaskingProperties properties;

  public SensitiveDataMasker(MaskingProperties properties) {
    this.properties = properties;
  }

  /**
   * Masks sensitive information according to the configured properties.
   *
   * @param input original text
   * @return masked text
   */
  public String mask(String input) {
    if (input == null || input.isEmpty() || !properties.isEnabled()) {
      return input;
    }

    String replacement = resolveReplacement();
    String result = input;

    // Always mask named secrets when masking is globally enabled.
    result = maskAuthorizationValues(result, replacement);
    result = maskSensitiveKeyValues(result, replacement);

    if (properties.isMaskJwt()) {
      result = replace(SensitiveDataPatterns.JWT, result, replacement);
    }

    if (properties.isMaskEmail()) {
      result = replace(SensitiveDataPatterns.EMAIL, result, replacement);
    }

    if (properties.isMaskPhone()) {
      result = replace(SensitiveDataPatterns.PHONE, result, replacement);
    }

    return result;
  }

  /**
   * Masks authorization credentials while preserving the authentication scheme only for HTTP
   * Authorization headers.
   *
   * @param input original text
   * @param replacement replacement value
   * @return masked text
   */
  private String maskAuthorizationValues(String input, String replacement) {
    Matcher matcher = SensitiveDataPatterns.AUTHORIZATION_VALUE.matcher(input);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String prefix = matcher.group(1);
      String openingQuote = matcher.group(2);
      String scheme = matcher.group(3);
      String schemeSeparator = matcher.group(4);
      String closingQuote = matcher.group(6);

      StringBuilder maskedValue = new StringBuilder(prefix);
      maskedValue.append(openingQuote);

      boolean isHttpAuthorization = HTTP_AUTHORIZATION_PREFIX.matcher(prefix).matches();

      if (isHttpAuthorization && scheme != null) {
        maskedValue.append(scheme).append(schemeSeparator);
      }

      maskedValue.append(replacement);
      maskedValue.append(closingQuote);

      matcher.appendReplacement(result, Matcher.quoteReplacement(maskedValue.toString()));
    }

    matcher.appendTail(result);
    return result.toString();
  }

  /**
   * Masks values assigned to sensitive keys while preserving their delimiters.
   *
   * @param input original text
   * @param replacement replacement value
   * @return masked text
   */
  private String maskSensitiveKeyValues(String input, String replacement) {
    Matcher matcher = SensitiveDataPatterns.SECRET_KEY_VALUE.matcher(input);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String prefix = matcher.group(1);
      String quote = matcher.group(2);

      String maskedValue;

      if (quote != null) {
        maskedValue = quote + replacement + quote;
      } else {
        maskedValue = replacement;
      }

      matcher.appendReplacement(result, Matcher.quoteReplacement(prefix + maskedValue));
    }

    matcher.appendTail(result);
    return result.toString();
  }

  private String replace(Pattern pattern, String input, String replacement) {
    return pattern.matcher(input).replaceAll(Matcher.quoteReplacement(replacement));
  }

  private String resolveReplacement() {
    return StringUtils.hasText(properties.getReplacement())
        ? properties.getReplacement()
        : "[REDACTED]";
  }
}
