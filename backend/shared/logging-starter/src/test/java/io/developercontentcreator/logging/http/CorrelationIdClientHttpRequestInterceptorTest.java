package io.developercontentcreator.logging.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.developercontentcreator.logging.context.CorrelationContext;
import io.developercontentcreator.logging.properties.LoggingProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class CorrelationIdClientHttpRequestInterceptorTest {

  private LoggingProperties properties;
  private CorrelationIdClientHttpRequestInterceptor interceptor;
  private ClientHttpRequestExecution execution;
  private ClientHttpResponse response;

  @BeforeEach
  void setUp() throws Exception {
    properties = new LoggingProperties();
    interceptor = new CorrelationIdClientHttpRequestInterceptor(properties);

    execution = mock(ClientHttpRequestExecution.class);
    response = mock(ClientHttpResponse.class);

    when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(response);

    CorrelationContext.clear();
  }

  @AfterEach
  void tearDown() {
    CorrelationContext.clear();
  }

  @Test
  void shouldPropagateCorrelationIdFromMdc() throws Exception {
    CorrelationContext.setCorrelationId("correlation-123");

    HttpHeaders headers = executeRequest(new HttpHeaders());

    assertThat(headers.getFirst("X-Correlation-ID")).isEqualTo("correlation-123");
  }

  @Test
  void shouldNotAddHeaderWhenMdcIsEmpty() throws Exception {
    HttpHeaders headers = executeRequest(new HttpHeaders());

    assertThat(headers.getFirst("X-Correlation-ID")).isNull();
  }

  @Test
  void shouldNotPropagateInvalidCorrelationId() throws Exception {
    CorrelationContext.setCorrelationId("invalid/id");

    HttpHeaders headers = executeRequest(new HttpHeaders());

    assertThat(headers.getFirst("X-Correlation-ID")).isNull();
  }

  @Test
  void shouldNotPropagateCorrelationIdExceedingMaxLength() throws Exception {
    properties.setMaxLength(10);
    CorrelationContext.setCorrelationId("correlation-too-long");

    HttpHeaders headers = executeRequest(new HttpHeaders());

    assertThat(headers.getFirst("X-Correlation-ID")).isNull();
  }

  @Test
  void shouldPreserveExistingCorrelationIdHeader() throws Exception {
    CorrelationContext.setCorrelationId("correlation-context");

    HttpHeaders existingHeaders = new HttpHeaders();
    existingHeaders.set("X-Correlation-ID", "correlation-existing");

    HttpHeaders headers = executeRequest(existingHeaders);

    assertThat(headers.getFirst("X-Correlation-ID")).isEqualTo("correlation-existing");
  }

  @Test
  void shouldUseConfiguredHeaderName() throws Exception {
    properties.setHeaderName("X-Request-Correlation");
    CorrelationContext.setCorrelationId("correlation-456");

    HttpHeaders headers = executeRequest(new HttpHeaders());

    assertThat(headers.getFirst("X-Request-Correlation")).isEqualTo("correlation-456");
  }

  @Test
  void shouldUseCurrentCorrelationIdForEachRequest() throws Exception {
    CorrelationContext.setCorrelationId("correlation-first");
    HttpHeaders firstHeaders = executeRequest(new HttpHeaders());

    CorrelationContext.setCorrelationId("correlation-second");
    HttpHeaders secondHeaders = executeRequest(new HttpHeaders());

    assertThat(firstHeaders.getFirst("X-Correlation-ID")).isEqualTo("correlation-first");

    assertThat(secondHeaders.getFirst("X-Correlation-ID")).isEqualTo("correlation-second");
  }

  private HttpHeaders executeRequest(HttpHeaders headers) throws Exception {
    HttpRequest request = mock(HttpRequest.class);
    when(request.getHeaders()).thenReturn(headers);

    interceptor.intercept(request, new byte[0], execution);

    return headers;
  }
}
