# Architecture C4 — Developer Content Creator

## 1. Objectif

Cette documentation décrit l'architecture de Developer Content Creator
à l'aide du modèle C4.

La plateforme analyse des dépôts GitHub et utilise l'intelligence
artificielle pour générer du contenu technique, notamment des articles,
des publications sur les réseaux sociaux, des notes de version et des
newsletters.

Le modèle Structurizr DSL constitue la source de vérité des diagrammes.

## 2. Source du modèle architectural

Le modèle est maintenu dans le fichier :

`architecture/structurizr/workspace.dsl`

Il définit les acteurs, les systèmes externes, les conteneurs applicatifs,
les relations et les vues architecturales.

Les diagrammes sont générés à partir de ce modèle. Ils ne doivent pas
être modifiés manuellement.

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

### Services applicatifs

- `gateway` : point d'entrée de la plateforme.
- `repository-service` : gestion des dépôts et intégration GitHub.
- `ai-orchestrator` : orchestration des traitements IA.
- `content-service` : gestion des contenus générés.
- `publication-service` : gestion des opérations de publication.

Les relations présentées dans les diagrammes doivent être vérifiées
par rapport au code et à la configuration réels.

## 4. Diagrammes C4 disponibles

### C1 — System Context

Fichier généré :

[`structurizr-C1-System-Context.mmd`](diagrams/structurizr-C1-System-Context.mmd)

Cette vue présente le système dans son environnement :
- les utilisateurs ;
- la plateforme ;
- les systèmes externes ;
- les principales interactions.

### C2 — Containers

Fichier généré :

[`structurizr-C2-Containers.mmd`](diagrams/structurizr-C2-Containers.mmd)

Cette vue présente les principaux services applicatifs et composants
d'infrastructure :
- le gateway ;
- les services métier ;
- PostgreSQL ;
- Apache Kafka ;
- les relations de communication.

### Évolutions possibles

Les vues suivantes pourront être ajoutées lorsque le besoin sera confirmé :

- C3 — Components : composants internes d'un service.
- Dynamic : scénarios d'interaction.
- Deployment : infrastructure et déploiement Kubernetes.

## 5. Arborescence

```text
architecture/
└── structurizr/
    └── workspace.dsl

docs/
└── architecture/
    └── c4/
        ├── README.md
        ├── diagrams/
        │   ├── structurizr-C1-System-Context.mmd
        │   └── structurizr-C2-Containers.mmd
        └── decisions/