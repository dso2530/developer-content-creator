package io.developercontentcreator.logging.auto;

import io.developercontentcreator.logging.masking.MaskingProperties;
import io.developercontentcreator.logging.properties.LoggingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties({LoggingProperties.class, MaskingProperties.class})
public class LoggingPropertiesAutoConfiguration {}
