package io.developercontentcreator.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "developer-content-creator.security")
public class SecurityProperties {

  /** Enables or disables the security starter. */
  private boolean enabled = true;

  /** JWT claim containing the internal DCC user UUID. */
  private String userIdClaim = "dcc_user_id";

  /** JWT claim containing the username. */
  private String usernameClaim = "preferred_username";

  /** JWT claim containing the user's authorities. */
  private String authoritiesClaim = "roles";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getUserIdClaim() {
    return userIdClaim;
  }

  public void setUserIdClaim(String userIdClaim) {
    this.userIdClaim = userIdClaim;
  }

  public String getUsernameClaim() {
    return usernameClaim;
  }

  public void setUsernameClaim(String usernameClaim) {
    this.usernameClaim = usernameClaim;
  }

  public String getAuthoritiesClaim() {
    return authoritiesClaim;
  }

  public void setAuthoritiesClaim(String authoritiesClaim) {
    this.authoritiesClaim = authoritiesClaim;
  }
}
