package io.developercontentcreator.logging.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.developercontentcreator.logging.context.CorrelationContext;
import io.developercontentcreator.logging.properties.LoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private static final String HEADER = "X-Correlation-ID";

  private TestableCorrelationIdFilter filter;

  @BeforeEach
  void setUp() {
    filter = new TestableCorrelationIdFilter(new LoggingProperties());
  }

  @AfterEach
  void cleanUp() {
    CorrelationContext.clear();
  }

  @Test
  void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.execute(request, response, (req, res) -> {});

    String correlationId = response.getHeader(HEADER);

    assertNotNull(correlationId);
    assertTrue(isUuid(correlationId));
    assertNull(CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldPreserveValidCorrelationId() throws Exception {
    String expectedId = "request-123";

    MockHttpServletRequest request = requestWithId(expectedId);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.execute(
        request,
        response,
        (req, res) -> assertEquals(expectedId, CorrelationContext.getCorrelationId()));

    assertEquals(expectedId, response.getHeader(HEADER));
    assertNull(CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldGenerateCorrelationIdWhenHeaderIsEmpty() throws Exception {
    MockHttpServletRequest request = requestWithId("");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.execute(request, response, (req, res) -> {});

    assertTrue(isUuid(response.getHeader(HEADER)));
  }

  @Test
  void shouldGenerateCorrelationIdWhenHeaderContainsInvalidCharacters() throws Exception {

    MockHttpServletRequest request = requestWithId("invalid correlation id!");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.execute(request, response, (req, res) -> {});

    assertTrue(isUuid(response.getHeader(HEADER)));
  }

  @Test
  void shouldGenerateCorrelationIdWhenHeaderIsTooLong() throws Exception {
    MockHttpServletRequest request = requestWithId("a".repeat(129));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.execute(request, response, (req, res) -> {});

    assertTrue(isUuid(response.getHeader(HEADER)));
  }

  @Test
  void shouldAcceptCorrelationIdAtMaximumLength() throws Exception {
    String expectedId = "a".repeat(128);

    MockHttpServletRequest request = requestWithId(expectedId);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.execute(request, response, (req, res) -> {});

    assertEquals(expectedId, response.getHeader(HEADER));
  }

  @Test
  void shouldRestorePreviousCorrelationIdAfterRequest() throws Exception {
    CorrelationContext.setCorrelationId("previous-id");

    MockHttpServletRequest request = requestWithId("current-id");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.execute(
        request,
        response,
        (req, res) -> assertEquals("current-id", CorrelationContext.getCorrelationId()));

    assertEquals("previous-id", CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldClearCorrelationIdWhenRequestThrowsException() {
    MockHttpServletRequest request = requestWithId("request-789");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain chain =
        (req, res) -> {
          assertEquals("request-789", CorrelationContext.getCorrelationId());
          throw new ServletException("Simulated failure");
        };

    assertThrows(ServletException.class, () -> filter.execute(request, response, chain));

    assertNull(CorrelationContext.getCorrelationId());
  }

  @Test
  void shouldUseConfiguredHeaderName() throws Exception {
    LoggingProperties properties = new LoggingProperties();
    properties.setHeaderName("X-Request-ID");

    TestableCorrelationIdFilter customFilter = new TestableCorrelationIdFilter(properties);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Request-ID", "custom-request-123");

    MockHttpServletResponse response = new MockHttpServletResponse();

    customFilter.execute(request, response, (req, res) -> {});

    assertEquals("custom-request-123", response.getHeader("X-Request-ID"));
  }

  private MockHttpServletRequest requestWithId(String correlationId) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HEADER, correlationId);
    return request;
  }

  private boolean isUuid(String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException | NullPointerException exception) {
      return false;
    }
  }

  private static class TestableCorrelationIdFilter extends CorrelationIdFilter {

    TestableCorrelationIdFilter(LoggingProperties properties) {
      super(properties);
    }

    void execute(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

      doFilterInternal(request, response, chain);
    }
  }
}
