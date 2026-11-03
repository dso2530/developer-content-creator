package io.developercontentcreator.repository.application.config;

import io.developercontentcreator.repository.application.port.out.DeactivateGitHubInstallationPort;
import io.developercontentcreator.repository.application.port.out.ExistsRegisteredRepositoryPort;
import io.developercontentcreator.repository.application.port.out.LoadGitHubInstallationPort;
import io.developercontentcreator.repository.application.port.out.LoadGitHubRepositoryPort;
import io.developercontentcreator.repository.application.port.out.MarkGitHubWebhookDeliveryProcessedPort;
import io.developercontentcreator.repository.application.port.out.RegisterGitHubWebhookDeliveryPort;
import io.developercontentcreator.repository.application.port.out.SaveRegisteredRepositoryPort;
import io.developercontentcreator.repository.application.service.DeactivateGitHubInstallationService;
import io.developercontentcreator.repository.application.service.ProcessGitHubWebhookService;
import io.developercontentcreator.repository.application.service.RegisterRepositoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationServiceConfiguration {

  @Bean
  RegisterRepositoryService registerRepositoryService(
      LoadGitHubInstallationPort installationPort,
      LoadGitHubRepositoryPort repositoryPort,
      ExistsRegisteredRepositoryPort existsPort,
      SaveRegisteredRepositoryPort savePort) {

    return new RegisterRepositoryService(installationPort, repositoryPort, existsPort, savePort);
  }

  @Bean
  DeactivateGitHubInstallationService deactivateGitHubInstallationService(
      DeactivateGitHubInstallationPort deactivatePort) {

    return new DeactivateGitHubInstallationService(deactivatePort);
  }

  @Bean
  ProcessGitHubWebhookService processGitHubWebhookService(
      RegisterGitHubWebhookDeliveryPort registerDeliveryPort,
      DeactivateGitHubInstallationPort deactivateInstallationPort,
      MarkGitHubWebhookDeliveryProcessedPort markDeliveryProcessedPort) {

    return new ProcessGitHubWebhookService(
        registerDeliveryPort, deactivateInstallationPort, markDeliveryProcessedPort);
  }
}
