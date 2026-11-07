package io.developercontentcreator.logging.masking;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;

/** Applies sensitive data masking to structured JSON log values. */
public class SensitiveDataLoggingJsonMembersCustomizer
    implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

  private static final String PROPERTY_PREFIX = "developer-content-creator.logging.masking";

  private static final Pattern SENSITIVE_FIELD_NAME =
      Pattern.compile(
          "(?i)^(password|passwd|pwd|client[_-]?secret|api[_-]?key|"
              + "access[_-]?token|refresh[_-]?token|id[_-]?token|"
              + "auth[_-]?token|secret|authorization|cookie|set-cookie|"
              + "private[_-]?key)$");

  private final SensitiveDataMasker masker;
  private final MaskingProperties properties;

  public SensitiveDataLoggingJsonMembersCustomizer(Environment environment) {

    this.properties =
        Binder.get(environment)
            .bind(PROPERTY_PREFIX, Bindable.of(MaskingProperties.class))
            .orElseGet(MaskingProperties::new);

    this.masker = new SensitiveDataMasker(properties);
  }

  @Override
  public void customize(JsonWriter.Members<ILoggingEvent> members) {

    members.applyingValueProcessor(
        new JsonWriter.ValueProcessor<Object>() {

          @Override
          public Object processValue(JsonWriter.MemberPath path, Object value) {

            if (!(value instanceof String text)) {
              return value;
            }

            String fieldName = path.name();

            if (fieldName != null && SENSITIVE_FIELD_NAME.matcher(fieldName).matches()) {
              return resolveReplacement();
            }

            return masker.mask(text);
          }
        });
  }

  private String resolveReplacement() {
    String replacement = properties.getReplacement();

    return replacement == null || replacement.isBlank() ? "[REDACTED]" : replacement;
  }
}
