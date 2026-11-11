package io.developercontentcreator.logging.masking;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for sensitive data masking. */
@ConfigurationProperties(prefix = "developer-content-creator.logging.masking")
public class MaskingProperties {

  private boolean enabled = true;

  private String replacement = "[REDACTED]";

  private boolean maskJwt = true;

  private boolean maskEmail = true;

  private boolean maskPhone = true;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getReplacement() {
    return replacement;
  }

  public void setReplacement(String replacement) {
    this.replacement = replacement;
  }

  public boolean isMaskJwt() {
    return maskJwt;
  }

  public void setMaskJwt(boolean maskJwt) {
    this.maskJwt = maskJwt;
  }

  public boolean isMaskEmail() {
    return maskEmail;
  }

  public void setMaskEmail(boolean maskEmail) {
    this.maskEmail = maskEmail;
  }

  public boolean isMaskPhone() {
    return maskPhone;
  }

  public void setMaskPhone(boolean maskPhone) {
    this.maskPhone = maskPhone;
  }
}
