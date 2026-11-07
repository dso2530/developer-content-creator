package io.developercontentcreator.messaging.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.developercontentcreator.messaging.config.MessagingProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessagingConventionsTest {

  @Test
  void shouldExposeExpectedKafkaTopics() {
    assertThat(
            List.of(
                MessagingTopics.REPOSITORY_ANALYZED,
                MessagingTopics.CONTENT_GENERATED,
                MessagingTopics.CONTENT_PUBLISHED))
        .containsExactly("repository-analyzed", "content-generated", "content-published")
        .doesNotHaveDuplicates();
  }

  @Test
  void shouldExposeExpectedApicurioArtifacts() {
    assertThat(
            List.of(
                MessagingArtifacts.REPOSITORY_ANALYZED,
                MessagingArtifacts.CONTENT_GENERATED,
                MessagingArtifacts.CONTENT_PUBLISHED))
        .containsExactly("repository-analyzed", "content-generated", "content-published")
        .doesNotHaveDuplicates();
  }

  @Test
  void shouldExposeDefaultApicurioRegistryGroup() {
    assertThat(MessagingRegistryGroups.DEFAULT).isEqualTo("developer-content-creator");
  }

  @Test
  void shouldUseDefaultRegistryGroupFromMessagingProperties() {
    MessagingProperties properties = new MessagingProperties();

    assertThat(properties.getRegistry().getGroupId()).isEqualTo(MessagingRegistryGroups.DEFAULT);
  }

  @Test
  void shouldAllowCustomRegistryGroup() {
    MessagingProperties properties = new MessagingProperties();

    properties.getRegistry().setGroupId("custom-registry-group");

    assertThat(properties.getRegistry().getGroupId()).isEqualTo("custom-registry-group");
  }

  @Test
  void shouldKeepKafkaTopicsAndApicurioArtifactsAligned() {
    assertThat(MessagingArtifacts.REPOSITORY_ANALYZED)
        .isEqualTo(MessagingTopics.REPOSITORY_ANALYZED);

    assertThat(MessagingArtifacts.CONTENT_GENERATED).isEqualTo(MessagingTopics.CONTENT_GENERATED);

    assertThat(MessagingArtifacts.CONTENT_PUBLISHED).isEqualTo(MessagingTopics.CONTENT_PUBLISHED);
  }

  @Test
  void shouldPreventInstantiationOfMessagingArtifacts() throws Exception {
    var constructor = MessagingArtifacts.class.getDeclaredConstructor();

    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance)
        .hasCauseInstanceOf(UnsupportedOperationException.class)
        .hasRootCauseMessage("Utility class");
  }

  @Test
  void shouldPreventInstantiationOfMessagingTopics() throws Exception {
    var constructor = MessagingTopics.class.getDeclaredConstructor();

    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance)
        .hasCauseInstanceOf(UnsupportedOperationException.class)
        .hasRootCauseMessage("Utility class");
  }

  @Test
  void shouldPreventInstantiationOfMessagingRegistryGroups() throws Exception {
    var constructor = MessagingRegistryGroups.class.getDeclaredConstructor();

    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance)
        .hasCauseInstanceOf(UnsupportedOperationException.class)
        .hasRootCauseMessage("Utility class");
  }
}
