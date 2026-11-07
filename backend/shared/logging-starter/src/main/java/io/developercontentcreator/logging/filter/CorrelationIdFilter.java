package io.developercontentcreator.logging.filter;

import io.developercontentcreator.logging.context.CorrelationContext;
import io.developercontentcreator.logging.properties.LoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorrelationIdFilter extends OncePerRequestFilter {

  private static final Pattern VALID_CORRELATION_ID = Pattern.compile("^[a-zA-Z0-9._:-]+$");

  private final LoggingProperties properties;

  public CorrelationIdFilter(LoggingProperties properties) {
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = resolveCorrelationId(request);

    response.setHeader(properties.getHeaderName(), correlationId);

    try (CorrelationContext.Scope ignored = CorrelationContext.open(correlationId)) {
      filterChain.doFilter(request, response);
    }
  }

  private String resolveCorrelationId(HttpServletRequest request) {
    String candidate = request.getHeader(properties.getHeaderName());

    if (candidate == null
        || candidate.length() > properties.getMaxLength()
        || !VALID_CORRELATION_ID.matcher(candidate).matches()) {
      return UUID.randomUUID().toString();
    }

    return candidate;
  }
}
