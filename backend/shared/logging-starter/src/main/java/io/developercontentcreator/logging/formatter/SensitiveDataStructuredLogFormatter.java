package io.developercontentcreator.logging.formatter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import io.developercontentcreator.logging.masking.MaskingProperties;
import io.developercontentcreator.logging.masking.SensitiveDataMasker;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;
import org.springframework.core.env.Environment;

/** Logstash-compatible structured formatter that masks sensitive data. */
public class SensitiveDataStructuredLogFormatter
    extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

  private static final String CORRELATION_ID_KEY = "correlationId";

  public SensitiveDataStructuredLogFormatter(
      Environment environment, StructuredLoggingJsonMembersCustomizer<?> customizer) {

    super(members -> addMembers(members, createMasker(environment)), customizer);
  }

  private static void addMembers(
      JsonWriter.Members<ILoggingEvent> members, SensitiveDataMasker masker) {

    members
        .add("@timestamp", ILoggingEvent::getInstant)
        .as(
            instant ->
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                    OffsetDateTime.ofInstant(instant, ZoneId.systemDefault())));

    members.add("@version", "1");

    members.add("message", ILoggingEvent::getFormattedMessage).as(masker::mask);

    members.add("logger_name", ILoggingEvent::getLoggerName);

    members.add("thread_name", ILoggingEvent::getThreadName);

    members.add("level", ILoggingEvent::getLevel).as(Level::toString);

    members.add("level_value", ILoggingEvent::getLevel).as(Level::toInt);

    members
        .add(CORRELATION_ID_KEY, event -> event.getMDCPropertyMap().get(CORRELATION_ID_KEY))
        .whenNotNull();

    members
        .add("stack_trace", ILoggingEvent::getThrowableProxy)
        .whenNotNull()
        .as(throwableProxy -> masker.mask(ThrowableProxyUtil.asString(throwableProxy)));
  }

  private static SensitiveDataMasker createMasker(Environment environment) {

    MaskingProperties properties =
        Binder.get(environment)
            .bind("developer-content-creator.logging.masking", MaskingProperties.class)
            .orElseGet(MaskingProperties::new);

    return new SensitiveDataMasker(properties);
  }
}
