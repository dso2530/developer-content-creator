workspace "Developer Content Creator" "Architecture C4 de la plateforme de génération de contenu technique" {

    !identifiers hierarchical

    model {
        developer = person "Développeur" "Utilise la plateforme pour générer du contenu technique à partir de dépôts GitHub."

        github = softwareSystem "GitHub" "Héberge les dépôts sources et expose les API GitHub." {
            tags "External"
        }

        llmProvider = softwareSystem "LLM Provider" "Fournisseur de modèles de langage : Ollama ou OpenAI." {
            tags "External"
        }

        platform = softwareSystem "Developer Content Creator" "Plateforme de génération de contenu technique à partir de dépôts logiciels." {

            gateway = container "Gateway" "Point d'entrée HTTP de la plateforme." "Java 21, Spring Boot"

            repositoryService = container "Repository Service" "Gère les dépôts et les interactions avec GitHub." "Java 21, Spring Boot"

            aiOrchestrator = container "AI Orchestrator" "Coordonne les traitements IA." "Java 21, Spring Boot"

            contentService = container "Content Service" "Gère le contenu technique généré." "Java 21, Spring Boot"

            publicationService = container "Publication Service" "Gère les opérations de publication du contenu." "Java 21, Spring Boot"

            postgres = container "PostgreSQL" "Stockage relationnel de la plateforme, avec pgvector si nécessaire." "PostgreSQL 17, pgvector" {
                tags "Database"
            }

            kafka = container "Kafka" "Transport asynchrone des événements entre services." "Apache Kafka" {
                tags "Queue"
            }
        }

        developer -> platform.gateway "Utilise" "HTTPS"
        developer -> github "Consulte et gère ses dépôts" "HTTPS"

        platform.gateway -> platform.repositoryService "Transmet les requêtes relatives aux dépôts" "HTTP"
        platform.gateway -> platform.contentService "Transmet les requêtes de contenu" "HTTP"

        platform.repositoryService -> github "Consulte les dépôts et leurs métadonnées" "HTTPS"
        platform.repositoryService -> platform.postgres "Lit et écrit les données des dépôts" "SQL"
        platform.repositoryService -> platform.kafka "Publie ou consomme les événements configurés" "Kafka protocol"

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
