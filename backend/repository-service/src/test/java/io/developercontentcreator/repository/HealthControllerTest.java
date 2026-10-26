package io.developercontentcreator.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTest {

  private final HealthController healthController = new HealthController();

  @Test
  void healthShouldReturnUpStatus() {
    Map<String, String> response = healthController.health();

    assertThat(response).containsExactly(Map.entry("status", "UP"));
  }
}