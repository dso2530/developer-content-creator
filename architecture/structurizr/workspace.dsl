workspace "Developer Content Creator" "Architecture C4 de la plateforme de génération de contenu technique" {

    !identifiers hierarchical

    model {
        developer = person "Développeur" "Utilise la plateforme pour générer du contenu technique à partir de dépôts GitHub."

        github = softwareSystem "GitHub" "Héberge les dépôts sources, expose les API GitHub et transmet les événements de cycle de vie des installations." {
            tags "External"
        }

        llmProvider = softwareSystem "LLM Provider" "Fournisseur de modèles de langage : Ollama ou OpenAI." {
            tags "External"
        }

        platform = softwareSystem "Developer Content Creator" "Plateforme de génération de contenu technique à partir de dépôts logiciels." {

            postgres = container "PostgreSQL" "Stockage relationnel de la plateforme, avec pgvector si nécessaire." "PostgreSQL 17, pgvector" {
                tags "Database"
            }

            kafka = container "Kafka" "Transport asynchrone des événements entre services." "Apache Kafka" {
                tags "Queue"
            }

            repositoryService = container "Repository Service" "Gestion des dépôts GitHub, des installations et des webhooks GitHub." "Java 21, Spring Boot" {

                applicationEntryPoint = component "RepositoryServiceApplication" "Point d'entrée Spring Boot du service." "Spring Boot"

                applicationConfiguration = component "ApplicationServiceConfiguration" "Configure les services applicatifs et leurs dépendances." "Spring Configuration"

                healthController = component "HealthController" "Expose le endpoint de santé du service." "Spring MVC REST Controller"

                webhookController = component "GitHubWebhookController" "Reçoit les webhooks GitHub et contrôle leur authenticité avant traitement." "Spring MVC REST Controller"

                signatureValidator = component "GitHubWebhookSignatureValidator" "Vérifie la signature HMAC-SHA256 des webhooks GitHub." "Java Security"

                securityConfiguration = component "RepositorySecurityConfiguration" "Configure la sécurité HTTP, JWT et les règles d'accès aux endpoints." "Spring Security"

                registerRepositoryService = component "RegisterRepositoryService" "Enregistre un dépôt GitHub après vérification de l'installation et de l'absence de doublon." "Application Service"

                processWebhookService = component "ProcessGitHubWebhookService" "Traite les événements installation.deleted avec idempotence et transaction." "Application Service"

                deactivateInstallationService = component "DeactivateGitHubInstallationService" "Expose le cas d'utilisation de désactivation d'une installation GitHub." "Application Service"

                githubRepositoryHttpAdapter = component "GitHubRepositoryHttpAdapter" "Interroge l'API GitHub pour récupérer les informations d'un dépôt." "HTTP Adapter"

                githubRepositoryDisabledAdapter = component "GitHubRepositoryDisabledAdapter" "Fournit l'implémentation alternative du port GitHub lorsque l'intégration est désactivée." "Conditional Adapter"

                installationPersistenceAdapter = component "GitHubInstallationPersistenceAdapter" "Charge et désactive les installations GitHub." "Persistence Adapter"

                registeredRepositoryPersistenceAdapter = component "RegisteredRepositoryPersistenceAdapter" "Vérifie les doublons et persiste les dépôts enregistrés." "Persistence Adapter"

                webhookDeliveryPersistenceAdapter = component "GitHubWebhookDeliveryPersistenceAdapter" "Enregistre les livraisons webhook et garantit leur traitement idempotent." "JDBC Persistence Adapter"

                applicationEntryPoint -> applicationConfiguration "Démarre et configure le contexte applicatif"

                applicationConfiguration -> registerRepositoryService "Configure"
                applicationConfiguration -> processWebhookService "Configure"
                applicationConfiguration -> deactivateInstallationService "Configure"

                securityConfiguration -> healthController "Configure les règles d'accès HTTP"
                securityConfiguration -> webhookController "Configure les règles d'accès HTTP au webhook"

                webhookController -> signatureValidator "Vérifie la signature du webhook"
                webhookController -> processWebhookService "Transmet les événements installation.deleted"

                processWebhookService -> webhookDeliveryPersistenceAdapter "Enregistre et marque les livraisons"
                processWebhookService -> installationPersistenceAdapter "Demande la désactivation de l'installation"

                deactivateInstallationService -> installationPersistenceAdapter "Désactive l'installation"

                registerRepositoryService -> installationPersistenceAdapter "Vérifie l'installation"
                registerRepositoryService -> githubRepositoryHttpAdapter "Récupère les informations du dépôt"
                registerRepositoryService -> githubRepositoryDisabledAdapter "Utilise l'implémentation alternative lorsque GitHub est désactivé"
                registerRepositoryService -> registeredRepositoryPersistenceAdapter "Vérifie et enregistre le dépôt"

                installationPersistenceAdapter -> postgres "Persiste les installations"
                registeredRepositoryPersistenceAdapter -> postgres "Persiste les dépôts"
                webhookDeliveryPersistenceAdapter -> postgres "Persiste les livraisons webhook"

                githubRepositoryHttpAdapter -> github "Interroge l'API GitHub"
            }

            gateway = container "Gateway" "Point d'entrée HTTP de la plateforme. Structure initiale, sans implémentation métier." "Java 21, Spring Boot"

            aiOrchestrator = container "AI Orchestrator" "Orchestration des traitements IA. Structure initiale, sans implémentation métier." "Java 21, Spring Boot"

            contentService = container "Content Service" "Gestion des contenus techniques générés. Structure initiale, sans implémentation métier." "Java 21, Spring Boot"

            publicationService = container "Publication Service" "Gestion de la publication des contenus. Structure initiale, sans implémentation métier." "Java 21, Spring Boot"
        }

        developer -> platform.gateway "Utilise" "HTTPS"
        developer -> github "Consulte et gère ses dépôts" "HTTPS"

        github -> platform.repositoryService "Envoie les webhooks d'installation" "HTTPS"
        platform.repositoryService -> github "Consulte les dépôts via l'API GitHub" "HTTPS"

        platform.gateway -> platform.repositoryService "Transmet les requêtes relatives aux dépôts" "HTTP"
        platform.gateway -> platform.contentService "Transmet les requêtes de contenu" "HTTP"

        platform.repositoryService -> platform.postgres "Persiste les installations, dépôts et livraisons webhook" "SQL"

        platform.aiOrchestrator -> platform.kafka "Échange les événements de traitement" "Kafka protocol"
        platform.aiOrchestrator -> llmProvider "Demande la génération ou l'analyse de contenu" "API modèle de langage"

        platform.contentService -> platform.postgres "Stocke les contenus" "SQL"
        platform.contentService -> platform.kafka "Échange les événements de contenu" "Kafka protocol"

        platform.publicationService -> platform.kafka "Reçoit les demandes de publication" "Kafka protocol"
        platform.publicationService -> github "Interagit avec les API GitHub selon les fonctionnalités activées" "HTTPS"
    }

    views {
        systemContext platform "C1-System-Context" "C1 - Contexte système" {
            include *
            autoLayout lr
        }

        container platform "C2-Containers" "C2 - Conteneurs applicatifs et infrastructure" {
            include *
            autoLayout lr
        }

        component platform.repositoryService "C3-Repository-Service" "C3 - Composants du Repository Service" {
            include *
            autoLayout lr
        }

        styles {
            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }

            element "Software System" {
                background #1168bd
                color #ffffff
            }

            element "Container" {
                background #438dd5
                color #ffffff
            }

            element "Component" {
                background #85bbf0
                color #000000
            }

            element "External" {
                background #999999
                color #ffffff
            }

            element "Database" {
                shape cylinder
            }

            element "Queue" {
                shape pipe
            }

            relationship "Relationship" {
                color #707070
                thickness 2
            }
        }
    }
}