package io.developercontentcreator.messaging.config;

import io.developercontentcreator.messaging.contract.MessagingRegistryGroups;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "developer-content-creator.messaging")
public class MessagingProperties {

  /** Enables or disables the messaging infrastructure. */
  private boolean enabled = true;

  /** Apicurio Registry configuration. */
  private final Registry registry = new Registry();

  /** Retry configuration. */
  private final Retry retry = new Retry();

  /** Dead Letter Topic configuration. */
  private final DeadLetter deadLetter = new DeadLetter();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Registry getRegistry() {
    return registry;
  }

  public Retry getRetry() {
    return retry;
  }

  public DeadLetter getDeadLetter() {
    return deadLetter;
  }

  public static class Registry {

    private String url = "http://localhost:8080/apis/registry/v3";

    private boolean specificAvroReader = true;

    private String encoding = "BINARY";

    private String groupId = MessagingRegistryGroups.DEFAULT;

    public String getGroupId() {
      return groupId;
    }

    public void setGroupId(String groupId) {
      this.groupId = groupId;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public boolean isSpecificAvroReader() {
      return specificAvroReader;
    }

    public void setSpecificAvroReader(boolean specificAvroReader) {
      this.specificAvroReader = specificAvroReader;
    }

    public String getEncoding() {
      return encoding;
    }

    public void setEncoding(String encoding) {
      this.encoding = encoding;
    }
  }

  public static class Retry {

    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    private int maxAttempts = 4;

    private long initialInterval = 1000;

    private double multiplier = 2.0;

    private long maxInterval = 4000;

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public long getInitialInterval() {
      return initialInterval;
    }

    public void setInitialInterval(long initialInterval) {
      this.initialInterval = initialInterval;
    }

    public double getMultiplier() {
      return multiplier;
    }

    public void setMultiplier(double multiplier) {
      this.multiplier = multiplier;
    }

    public long getMaxInterval() {
      return maxInterval;
    }

    public void setMaxInterval(long maxInterval) {
      this.maxInterval = maxInterval;
    }
  }

  public static class DeadLetter {

    private boolean enabled = true;

    private String topicSuffix = ".DLT";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getTopicSuffix() {
      return topicSuffix;
    }

    public void setTopicSuffix(String topicSuffix) {
      this.topicSuffix = topicSuffix;
    }
  }
}
