package io.developercontentcreator.repository.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.developercontentcreator.repository.application.port.out.DeactivateGitHubInstallationPort;
import io.developercontentcreator.repository.application.port.out.MarkGitHubWebhookDeliveryProcessedPort;
import io.developercontentcreator.repository.application.port.out.RegisterGitHubWebhookDeliveryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessGitHubWebhookServiceTest {

  private static final String DELIVERY_ID = "github-delivery-123";
  private static final Long INSTALLATION_ID = 12345L;

  @Mock private RegisterGitHubWebhookDeliveryPort registerDeliveryPort;

  @Mock private DeactivateGitHubInstallationPort deactivateInstallationPort;

  @Mock private MarkGitHubWebhookDeliveryProcessedPort markDeliveryProcessedPort;

  private ProcessGitHubWebhookService service;

  @BeforeEach
  void setUp() {
    service =
        new ProcessGitHubWebhookService(
            registerDeliveryPort, deactivateInstallationPort, markDeliveryProcessedPort);
  }

  @Test
  void shouldProcessNewInstallationDeletedDelivery() {
    when(registerDeliveryPort.register(DELIVERY_ID, "installation", "deleted")).thenReturn(true);

    service.processInstallationDeleted(DELIVERY_ID, INSTALLATION_ID);

    InOrder order =
        inOrder(registerDeliveryPort, deactivateInstallationPort, markDeliveryProcessedPort);

    order.verify(registerDeliveryPort).register(DELIVERY_ID, "installation", "deleted");

    order.verify(deactivateInstallationPort).deactivate(INSTALLATION_ID);

    order.verify(markDeliveryProcessedPort).markProcessed(DELIVERY_ID);
  }

  @Test
  void shouldIgnoreAlreadyRegisteredDelivery() {
    when(registerDeliveryPort.register(DELIVERY_ID, "installation", "deleted")).thenReturn(false);

    service.processInstallationDeleted(DELIVERY_ID, INSTALLATION_ID);

    verify(registerDeliveryPort).register(DELIVERY_ID, "installation", "deleted");

    verifyNoInteractions(deactivateInstallationPort, markDeliveryProcessedPort);
  }

  @Test
  void shouldRejectNullDeliveryId() {
    assertThatThrownBy(() -> service.processInstallationDeleted(null, INSTALLATION_ID))
        .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(
        registerDeliveryPort, deactivateInstallationPort, markDeliveryProcessedPort);
  }

  @Test
  void shouldRejectBlankDeliveryId() {
    assertThatThrownBy(() -> service.processInstallationDeleted("  ", INSTALLATION_ID))
        .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(
        registerDeliveryPort, deactivateInstallationPort, markDeliveryProcessedPort);
  }

  @Test
  void shouldRejectNullInstallationId() {
    assertThatThrownBy(() -> service.processInstallationDeleted(DELIVERY_ID, null))
        .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(
        registerDeliveryPort, deactivateInstallationPort, markDeliveryProcessedPort);
  }

  @Test
  void shouldRejectNonPositiveInstallationId() {
    assertThatThrownBy(() -> service.processInstallationDeleted(DELIVERY_ID, 0L))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> service.processInstallationDeleted(DELIVERY_ID, -1L))
        .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(
        registerDeliveryPort, deactivateInstallationPort, markDeliveryProcessedPort);
  }

  @Test
  void shouldNotMarkDeliveryProcessedWhenDeactivationFails() {
    RuntimeException exception = new RuntimeException("Database unavailable");

    when(registerDeliveryPort.register(DELIVERY_ID, "installation", "deleted")).thenReturn(true);

    org.mockito.Mockito.doThrow(exception)
        .when(deactivateInstallationPort)
        .deactivate(INSTALLATION_ID);

    assertThatThrownBy(() -> service.processInstallationDeleted(DELIVERY_ID, INSTALLATION_ID))
        .isSameAs(exception);

    verify(markDeliveryProcessedPort, never()).markProcessed(DELIVERY_ID);
  }

  @Test
  void shouldPropagateFailureWhenMarkingDeliveryProcessed() {
    RuntimeException exception = new RuntimeException("Unable to update delivery");

    when(registerDeliveryPort.register(DELIVERY_ID, "installation", "deleted")).thenReturn(true);

    org.mockito.Mockito.doThrow(exception)
        .when(markDeliveryProcessedPort)
        .markProcessed(DELIVERY_ID);

    assertThatThrownBy(() -> service.processInstallationDeleted(DELIVERY_ID, INSTALLATION_ID))
        .isSameAs(exception);

    verify(deactivateInstallationPort).deactivate(INSTALLATION_ID);
  }
}
