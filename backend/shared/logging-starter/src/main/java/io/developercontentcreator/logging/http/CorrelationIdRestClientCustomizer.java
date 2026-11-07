package io.developercontentcreator.logging.http;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.web.client.RestClient;

public class CorrelationIdRestClientCustomizer implements RestClientCustomizer {

  private final CorrelationIdClientHttpRequestInterceptor interceptor;

  public CorrelationIdRestClientCustomizer(CorrelationIdClientHttpRequestInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Override
  public void customize(RestClient.Builder builder) {
    builder.requestInterceptor(interceptor);
  }
}
