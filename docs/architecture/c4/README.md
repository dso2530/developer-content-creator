# Architecture C4 — Developer Content Creator

## 1. Objectif

Cette documentation décrit l'architecture de **Developer Content Creator** à l'aide du modèle C4.

La plateforme analyse des dépôts GitHub et utilise l'intelligence artificielle pour générer du contenu technique, notamment :

- des articles techniques ;
- des publications pour les réseaux sociaux ;
- des notes de version ;
- des newsletters ;
- des synthèses des évolutions des dépôts.

Le modèle **Structurizr DSL** constitue la source de vérité de l'architecture et des diagrammes.

## 2. Source du modèle architectural

Le modèle architectural est maintenu dans le fichier :

```text
architecture/structurizr/workspace.dsl
```

Il définit :

- les acteurs ;
- les systèmes externes ;
- les conteneurs applicatifs ;
- les composants internes des services documentés ;
- les relations entre les éléments ;
- les vues architecturales C1, C2 et C3.

Les diagrammes Mermaid sont générés automatiquement à partir de ce modèle.

Ils ne doivent pas être modifiés manuellement : toute évolution doit être effectuée dans le fichier Structurizr DSL, puis générée à nouveau.

### Génération des diagrammes

Le script de génération est disponible ici :

```text
scripts/architecture/generate-c4.sh
```

Pour générer ou synchroniser les diagrammes :

```bash
./scripts/architecture/generate-c4.sh --write
```

Pour vérifier que les diagrammes versionnés correspondent au modèle :

```bash
./scripts/architecture/generate-c4.sh --check
```

Le mode `--check` est utilisé dans le pipeline CI afin de détecter les diagrammes qui ne seraient plus synchronisés avec le modèle architectural.

## 3. Architecture technique

| Domaine | Technologie |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot |
| Build | Maven multi-modules |
| Communication asynchrone | Apache Kafka |
| Base de données | PostgreSQL |
| Recherche vectorielle | pgvector |
| Intelligence artificielle | Ollama / OpenAI |
| Orchestration | Kubernetes |
| Déploiement | Argo CD |
| Documentation | C4 Model / Structurizr DSL |
| Diagrammes générés | Mermaid |

### Services applicatifs

| Service | Responsabilité |
|---|---|
| `gateway` | Point d'entrée HTTP de la plateforme |
| `repository-service` | Gestion des dépôts GitHub, des installations et des webhooks |
| `ai-orchestrator` | Orchestration des traitements IA |
| `content-service` | Gestion des contenus techniques générés |
| `publication-service` | Gestion des opérations de publication |

### État d'implémentation des services

Le diagramme C2 représente l'architecture globale cible. Tous les services qui y figurent ne sont pas nécessairement implémentés.

| Service | État actuel | Documentation |
|---|---|---|
| `gateway` | Structure initiale, sans implémentation métier | C2 |
| `repository-service` | Gestion des dépôts GitHub, des installations, des webhooks et de la persistance PostgreSQL | C2 + C3 |
| `ai-orchestrator` | Structure initiale, sans implémentation métier | C2 |
| `content-service` | Structure initiale, sans implémentation métier | C2 |
| `publication-service` | Structure initiale, sans implémentation métier | C2 |

Les diagrammes C3 sont ajoutés progressivement, au fur et à mesure de l'implémentation réelle des composants internes.

Les relations des vues C1 et C2 doivent être interprétées en tenant compte de cette distinction entre architecture cible et fonctionnalités effectivement disponibles.

## 4. Diagrammes C4 disponibles

### 4.1 C1 — System Context

Fichier généré :

[`structurizr-C1-System-Context.mmd`](diagrams/structurizr-C1-System-Context.mmd)

Cette vue présente le système dans son environnement :

- l'utilisateur ;
- la plateforme Developer Content Creator ;
- GitHub comme système externe ;
- le fournisseur de modèles de langage ;
- les principales interactions entre ces éléments.

### 4.2 C2 — Containers

Fichier généré :

[`structurizr-C2-Containers.mmd`](diagrams/structurizr-C2-Containers.mmd)

Cette vue présente les principaux conteneurs applicatifs et composants d'infrastructure :

- `gateway` ;
- `repository-service` ;
- `ai-orchestrator` ;
- `content-service` ;
- `publication-service` ;
- PostgreSQL ;
- Apache Kafka.

Elle représente les responsabilités des conteneurs et leurs principales relations de communication.

### 4.3 C3 — Components

Les vues C3 décrivent la structure interne d'un conteneur : ses composants, leurs responsabilités et leurs dépendances.

#### Repository Service

Fichier généré :

[`structurizr-C3-Repository-Service.mmd`](diagrams/structurizr-C3-Repository-Service.mmd)

[Consulter le diagramme C3 — Repository Service](diagrams/structurizr-C3-Repository-Service.mmd)

Cette première vue C3 documente les composants actuellement identifiés dans le `repository-service`.

**Composants d'entrée et de sécurité :**

- `RepositoryServiceApplication` : point d'entrée Spring Boot.
- `ApplicationServiceConfiguration` : configuration et enregistrement des services applicatifs.
- `HealthController` : expose le endpoint de santé.
- `GitHubWebhookController` : réception et traitement des webhooks GitHub.
- `GitHubWebhookSignatureValidator` : vérification des signatures HMAC-SHA256 avant l'analyse du payload.
- `RepositorySecurityConfiguration` : configuration de la sécurité HTTP et de l'authentification JWT.

**Composants applicatifs :**

- `RegisterRepositoryService` : enregistrement d'un dépôt GitHub après vérification de l'installation et des doublons.
- `ProcessGitHubWebhookService` : traitement transactionnel et idempotent des événements `installation.deleted`, incluant la désactivation de l'installation.
- `DeactivateGitHubInstallationService` : implémentation du cas d'utilisation autonome de désactivation d'une installation GitHub.

**Adaptateurs sortants :**

- `GitHubRepositoryHttpAdapter` : communication avec l'API GitHub pour récupérer les informations des dépôts.
- `GitHubRepositoryDisabledAdapter` : implémentation alternative du port `LoadGitHubRepositoryPort`, sélectionnée lorsque l'intégration GitHub est désactivée.
- `GitHubInstallationPersistenceAdapter` : chargement et désactivation des installations GitHub.
- `RegisteredRepositoryPersistenceAdapter` : vérification des doublons et persistance des dépôts.
- `GitHubWebhookDeliveryPersistenceAdapter` : persistance des livraisons webhook et gestion de leur idempotence.

**Sécurité des webhooks :**

La sécurité repose sur deux mécanismes complémentaires :

1. Spring Security autorise l'accès HTTP anonyme à la route webhook uniquement lorsque cette fonctionnalité est activée.
2. `GitHubWebhookController` vérifie la signature HMAC-SHA256 à partir du corps brut de la requête avant de désérialiser et de traiter le payload.

Les autres endpoints métier restent protégés par JWT lorsque la configuration correspondante est activée.

**Principes architecturaux illustrés :**

- architecture hexagonale ;
- séparation des responsabilités entre domaine, application et infrastructure ;
- utilisation de ports et d'adaptateurs ;
- validation cryptographique des webhooks avant leur traitement ;
- traitement idempotent des événements ;
- cohérence transactionnelle entre l'enregistrement d'une livraison et la désactivation d'une installation ;
- conservation des installations désactivées et des dépôts qui leur sont associés.

Les composants C3 doivent rester cohérents avec le code réellement implémenté. Toute évolution significative du service doit être répercutée dans le modèle Structurizr DSL.

### 4.4 Décisions architecturales

Les décisions structurantes du projet sont documentées dans le répertoire `decisions/`.

| ADR | Sujet |
|---|---|
| ADR-001 | Architecture hexagonale |
| ADR-002 | Communication interservices et événements |
| ADR-003 | GitHub App et sélection des dépôts |
| ADR-004 | Responsabilités des starters de sécurité |

Ces documents explicitent les choix architecturaux, leurs justifications et leurs conséquences sur l'organisation technique de la plateforme.

### 4.5 Évolutions possibles

Les vues suivantes pourront être ajoutées lorsque les fonctionnalités correspondantes seront suffisamment implémentées :

- **C3 — Components** : composants internes des autres services applicatifs.
- **Dynamic** : scénarios détaillés d'interaction entre composants.
- **Deployment** : infrastructure d'exécution et déploiement Kubernetes.

## 5. Arborescence

```text
architecture/
└── structurizr/
    └── workspace.dsl

scripts/
└── architecture/
    └── generate-c4.sh

docs/
└── architecture/
    └── c4/
        ├── README.md
        ├── diagrams/
        │   ├── structurizr-C1-System-Context.mmd
        │   ├── structurizr-C2-Containers.mmd
        │   └── structurizr-C3-Repository-Service.mmd
        └── decisions/
            ├── ADR-001-architecture-hexagonale.md
            ├── ADR-002-communication-interservices-et-evenements.md
            ├── ADR-003-github-app-repository-selection.md
            └── ADR-004-security-starter-responsibilities.md

.github/
└── workflows/
    └── architecture-docs.yml
```

## 6. Intégration continue

Le workflow GitHub Actions :

```text
.github/workflows/architecture-docs.yml
```

assure les contrôles suivants :

1. Vérification de la présence du modèle Structurizr DSL.
2. Validation syntaxique du modèle.
3. Génération des diagrammes Mermaid.
4. Comparaison entre les diagrammes générés et ceux versionnés.
5. Publication des diagrammes générés sous forme d'artifact CI.

Le workflow détecte les écarts sans modifier automatiquement les fichiers versionnés.

La synchronisation doit être effectuée localement avec :

```bash
./scripts/architecture/generate-c4.sh --write
```

Puis vérifiée avec :

```bash
./scripts/architecture/generate-c4.sh --check
```

## 7. Règles de maintenance

Pour préserver la cohérence de la documentation :

- Le fichier `workspace.dsl` est la source de vérité.
- Les diagrammes Mermaid sont des fichiers générés.
- Les composants représentés doivent correspondre au code réellement implémenté ou être clairement identifiés comme des éléments de l'architecture cible.
- Les relations doivent refléter les dépendances réelles.
- Toute modification architecturale significative doit être accompagnée d'une mise à jour du modèle.
- Les décisions structurantes doivent être documentées dans des ADR.
- La validation CI doit rester obligatoire pour les modifications du modèle ou des diagrammes.

**Objectif :** maintenir une documentation architecturale versionnée, reproductible et cohérente avec l'évolution du code source.