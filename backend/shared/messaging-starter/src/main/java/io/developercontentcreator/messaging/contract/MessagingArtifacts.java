package io.developercontentcreator.messaging.contract;

public final class MessagingArtifacts {

  public static final String REPOSITORY_ANALYZED = "repository-analyzed";
  public static final String CONTENT_GENERATED = "content-generated";
  public static final String CONTENT_PUBLISHED = "content-published";

  private MessagingArtifacts() {
    throw new UnsupportedOperationException("Utility class");
  }
}
