package io.developercontentcreator.logging.http;

import io.developercontentcreator.logging.context.CorrelationContext;
import io.developercontentcreator.logging.properties.LoggingProperties;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class CorrelationIdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

  private static final Pattern VALIDATION_PATTERN = Pattern.compile("^[a-zA-Z0-9._:-]+$");

  private final LoggingProperties properties;

  public CorrelationIdClientHttpRequestInterceptor(LoggingProperties properties) {
    this.properties = properties;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    HttpHeaders headers = request.getHeaders();
    String headerName = properties.getHeaderName();

    if (headers.getFirst(headerName) == null) {
      String correlationId = CorrelationContext.getCorrelationId();

      if (isValid(correlationId)) {
        headers.set(headerName, correlationId);
      }
    }

    return execution.execute(request, body);
  }

  private boolean isValid(String correlationId) {
    return correlationId != null
        && !correlationId.isBlank()
        && correlationId.length() <= properties.getMaxLength()
        && VALIDATION_PATTERN.matcher(correlationId).matches();
  }
}
