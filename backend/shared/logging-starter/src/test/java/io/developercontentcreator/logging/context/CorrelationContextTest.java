package io.developercontentcreator.logging.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationContextTest {

  @AfterEach
  void cleanUp() {
    CorrelationContext.clear();
  }

  @Test
  void shouldSetAndGetCorrelationId() {
    CorrelationContext.setCorrelationId("request-123");

    assertEquals("request-123", CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldClearCorrelationId() {
    CorrelationContext.setCorrelationId("request-123");

    CorrelationContext.clear();

    assertNull(CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldRemoveCorrelationIdWhenValueIsNull() {
    CorrelationContext.setCorrelationId("request-123");

    CorrelationContext.setCorrelationId(null);

    assertNull(CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldRemoveCorrelationIdWhenValueIsBlank() {
    CorrelationContext.setCorrelationId("request-123");

    CorrelationContext.setCorrelationId(" ");

    assertNull(CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldRestorePreviousCorrelationIdAfterScope() {
    CorrelationContext.setCorrelationId("previous-id");

    try (CorrelationContext.Scope ignored = CorrelationContext.open("current-id")) {

      assertEquals("current-id", CorrelationContext.getCorrelationId());
    }

    assertEquals("previous-id", CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldClearCorrelationIdAfterScopeWhenNoPreviousValueExists() {
    try (CorrelationContext.Scope ignored = CorrelationContext.open("current-id")) {

      assertEquals("current-id", CorrelationContext.getCorrelationId());
    }

    assertNull(CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldRestorePreviousValueAfterNestedScopes() {
    CorrelationContext.setCorrelationId("root-id");

    try (CorrelationContext.Scope outer = CorrelationContext.open("outer-id")) {

      assertEquals("outer-id", CorrelationContext.getCorrelationId());

      try (CorrelationContext.Scope inner = CorrelationContext.open("inner-id")) {

        assertEquals("inner-id", CorrelationContext.getCorrelationId());
      }

      assertEquals("outer-id", CorrelationContext.getCorrelationId());
    }

    assertEquals("root-id", CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldNotModifyOtherMdcEntriesWhenClearingCorrelationId() {
    MDC.put("otherKey", "other-value");
    CorrelationContext.setCorrelationId("request-123");

    CorrelationContext.clear();

    assertNull(CorrelationContext.getCorrelationId());
    assertEquals("other-value", MDC.get("otherKey"));

    MDC.remove("otherKey");
  }
}
