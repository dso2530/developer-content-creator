package io.developercontentcreator.logging.masking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaskingPropertiesTest {

  @Test
  void shouldUseDefaultValues() {
    MaskingProperties properties = new MaskingProperties();

    assertTrue(properties.isEnabled());
    assertEquals("[REDACTED]", properties.getReplacement());
    assertTrue(properties.isMaskJwt());
    assertTrue(properties.isMaskEmail());
    assertTrue(properties.isMaskPhone());
  }

  @Test
  void shouldAllowDisablingMasking() {
    MaskingProperties properties = new MaskingProperties();

    properties.setEnabled(false);

    assertFalse(properties.isEnabled());
  }

  @Test
  void shouldAllowCustomReplacement() {
    MaskingProperties properties = new MaskingProperties();

    properties.setReplacement("***");

    assertEquals("***", properties.getReplacement());
  }

  @Test
  void shouldAllowDisablingIndividualMaskingRules() {
    MaskingProperties properties = new MaskingProperties();

    properties.setMaskJwt(false);
    properties.setMaskEmail(false);
    properties.setMaskPhone(false);

    assertFalse(properties.isMaskJwt());
    assertFalse(properties.isMaskEmail());
    assertFalse(properties.isMaskPhone());
  }

  @Test
  void shouldAllowEnablingIndividualMaskingRules() {
    MaskingProperties properties = new MaskingProperties();

    properties.setMaskJwt(false);
    properties.setMaskEmail(false);
    properties.setMaskPhone(false);

    properties.setMaskJwt(true);
    properties.setMaskEmail(true);
    properties.setMaskPhone(true);

    assertTrue(properties.isMaskJwt());
    assertTrue(properties.isMaskEmail());
    assertTrue(properties.isMaskPhone());
  }
}
