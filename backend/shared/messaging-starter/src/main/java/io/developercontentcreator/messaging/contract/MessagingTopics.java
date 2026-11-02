package io.developercontentcreator.messaging.contract;

public final class MessagingTopics {

  public static final String REPOSITORY_ANALYZED = "repository-analyzed";
  public static final String CONTENT_GENERATED = "content-generated";
  public static final String CONTENT_PUBLISHED = "content-published";

  private MessagingTopics() {
    throw new UnsupportedOperationException("Utility class");
  }
}
