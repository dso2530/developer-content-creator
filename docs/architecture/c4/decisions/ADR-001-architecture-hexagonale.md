# ADR-001 — Architecture hexagonale et séparation des responsabilités

- **Identifiant :** ADR-001
- **Titre :** Architecture hexagonale et séparation des responsabilités
- **Date :** 2026-10-31
- **Statut :** Accepted
- **Décideurs :** Équipe Developer Content Creator

## 1. Contexte

Developer Content Creator est une plateforme composée de plusieurs services applicatifs destinés à analyser des dépôts GitHub et à générer automatiquement du contenu technique à l'aide de l'intelligence artificielle.

La plateforme repose notamment sur :

- Java 21 ;
- Spring Boot 4.1.2 ;
- Maven multi-modules ;
- PostgreSQL et pgvector ;
- Apache Kafka ;
- Ollama / OpenAI ;
- Kubernetes ;
- Argo CD.

L'évolution progressive de la plateforme nécessite une architecture permettant de :

- séparer les règles métier des frameworks techniques ;
- limiter le couplage entre les composants ;
- faciliter les tests unitaires ;
- remplacer les fournisseurs externes sans modifier le domaine ;
- maintenir une organisation homogène entre les microservices.

Une architecture hexagonale est proposée pour répondre à ces exigences.

## 2. Décision proposée

Il est proposé que la plateforme adopte progressivement une architecture hexagonale (Ports and Adapters).

Chaque service pourra séparer ses responsabilités en trois couches principales :

1. Domaine (Domain).
2. Application (Application).
3. Infrastructure (Infrastructure).

Les dépendances de code doivent être orientées vers les couches internes :
l'Infrastructure dépend de l'Application et du Domaine ; l'Application dépend du Domaine.

Les couches internes ne doivent pas dépendre des technologies d'infrastructure.

Cette organisation sera appliquée progressivement, en fonction de l'apparition des véritables cas d'utilisation métier.

## 3. Organisation logique

### 3.1 Domaine

Le domaine contient les règles métier indépendantes des technologies.

Responsabilités :

- entités métier ;
- objets valeur ;
- agrégats ;
- services métier ;
- règles et invariants métier ;
- événements métier.

Contraintes :

- aucune dépendance vers Spring Boot ;
- aucune dépendance vers JPA ;
- aucune dépendance vers Kafka ;
- aucune dépendance vers les SDK externes ;
- aucune dépendance vers les adaptateurs techniques.

### 3.2 Application

La couche Application orchestre les cas d'utilisation.

Responsabilités :

- cas d'utilisation ;
- commandes et requêtes ;
- ports entrants ;
- ports sortants ;
- orchestration des opérations métier ;
- gestion des transactions applicatives lorsque nécessaire.

Exemples :

- Analyser un dépôt GitHub.
- Déclencher une génération de contenu.
- Enregistrer un contenu généré.
- Publier un contenu.

La couche Application dépend du domaine et définit les interfaces nécessaires à ses interactions avec l'extérieur.

### 3.3 Infrastructure

La couche Infrastructure fournit les implémentations techniques des ports.

Responsabilités :

- contrôleurs REST ;
- adaptateurs PostgreSQL ;
- implémentations des repositories ;
- intégration GitHub ;
- producteurs et consommateurs Kafka ;
- intégration Ollama / OpenAI ;
- configuration Spring Boot ;
- clients HTTP externes.

Les dépendances techniques doivent rester dans cette couche ou dans les modules transverses appropriés.

## 4. Architecture logique

```text
                    MONDE EXTERIEUR
                           |
                           v
              +------------------------+
              |  ADAPTATEURS ENTRANTS  |
              |   REST / Kafka / Batch |
              +------------------------+
                           |
                           v
              +------------------------+
              |     PORTS ENTRANTS     |
              +------------------------+
                           |
                           v
              +------------------------+
              |      APPLICATION       |
              |                        |
              |      Use Cases         |
              |   Orchestration        |
              +------------------------+
                    |             |
                    v             v
              +-----------+  +------------------+
              |  DOMAINE  |  |  PORTS SORTANTS  |
              |           |  +------------------+
              | Entities  |           ^
              | Value Obj.|           |
              | Services  |           |
              | Invariants|  +------------------+
              +-----------+  | ADAPTATEURS      |
                             | SORTANTS         |
                             |                  |
                             | PostgreSQL       |
                             | GitHub API       |
                             | Kafka            |
                             | Ollama / OpenAI  |
                             +------------------+
```

Les adaptateurs entrants invoquent les cas d'utilisation.

Les adaptateurs sortants implémentent les ports définis par les couches internes.

Le domaine ne connaît ni les adaptateurs ni les fournisseurs techniques.

## 5. Organisation des modules Maven

Chaque microservice devra progressivement adopter une organisation cohérente.

Exemple de structure cible :

```text
repository-service/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/developercontentcreator/repository/
│   │   │       ├── domain/
│   │   │       │   ├── model/
│   │   │       │   ├── service/
│   │   │       │   └── event/
│   │   │       │
│   │   │       ├── application/
│   │   │       │   ├── port/
│   │   │       │   │   ├── in/
│   │   │       │   │   └── out/
│   │   │       │   └── usecase/
│   │   │       │
│   │   │       └── infrastructure/
│   │   │           ├── adapter/
│   │   │           │   ├── in/
│   │   │           │   │   ├── rest/
│   │   │           │   │   └── messaging/
│   │   │           │   └── out/
│   │   │           │       ├── persistence/
│   │   │           │       ├── github/
│   │   │           │       └── messaging/
│   │   │           └── configuration/
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │
│   └── test/
│       └── java/
```

Cette organisation est une convention cible. Elle pourra être adaptée à la complexité réelle de chaque service.

Il n'est pas obligatoire de créer des classes ou packages vides.

## 6. Règles de dépendances

Les règles suivantes devront être respectées :

| Couche | Peut dépendre de | Ne doit pas dépendre de |
|---|---|---|
| Domain | Java standard | Spring, JPA, Kafka, SDK externes |
| Application | Domain | Adaptateurs et implémentations techniques |
| Infrastructure | Application, Domain, bibliothèques techniques | — |

Les bibliothèques transverses du projet (`common`, `github-sdk`, `ai-client`, starters techniques) devront être utilisées selon leurs responsabilités.

Elles ne doivent pas introduire de dépendance technique dans le domaine.

## 7. Stratégie de tests

La stratégie de tests sera organisée selon les couches.

### Tests unitaires du domaine

- Vérification des invariants métier.
- Vérification des règles métier.
- Tests des entités et objets valeur.

Ces tests ne doivent pas nécessiter de contexte Spring.

### Tests applicatifs

- Vérification des cas d'utilisation.
- Simulation des ports sortants.
- Vérification des interactions entre composants.

### Tests d'intégration

- PostgreSQL via Testcontainers.
- Kafka via Testcontainers.
- Tests des adaptateurs HTTP.
- Vérification des transactions.
- Vérification des contrats d'événements.

### Tests d'architecture

Des tests automatisés devront progressivement vérifier :

- l'absence de dépendances Spring dans le domaine ;
- le respect des frontières entre couches ;
- l'absence de dépendances circulaires ;
- le respect des règles de dépendances Maven.

ArchUnit pourra être étudié pour automatiser ces contrôles.

## 8. Conséquences

### Conséquences positives

- Meilleure séparation des responsabilités.
- Domaine indépendant des frameworks.
- Tests unitaires simplifiés.
- Remplacement facilité des fournisseurs externes.
- Réduction du couplage technique.
- Cohérence architecturale entre les microservices.

### Conséquences négatives

- Structure initiale plus élaborée.
- Multiplication possible des interfaces et classes d'adaptation.
- Nécessité de maintenir des conventions communes.
- Risque de sur-ingénierie pour les fonctionnalités simples.

L'architecture devra rester proportionnée à la complexité réelle des fonctionnalités.

## 9. Alternatives étudiées

### Architecture en couches classique

Avantage : simplicité initiale.

Inconvénient : risque de dépendances directes du métier vers les frameworks et les composants techniques.

### Architecture microservices sans convention interne

Avantage : liberté de conception pour chaque service.

Inconvénient : risque d'incohérences structurelles et de couplage entre les composants.

### Architecture hexagonale

Avantage : isolation du domaine et inversion des dépendances.

Cette approche est proposée comme convention architecturale de référence
pour Developer Content Creator.

## 10. Plan d'adoption

L'adoption sera progressive :

1. Définir les conventions communes.
2. Appliquer la structure au `repository-service`.
3. Introduire les ports et adaptateurs nécessaires aux cas d'utilisation réels.
4. Appliquer les conventions au `content-service`.
5. Étendre progressivement aux autres services.
6. Ajouter les contrôles ArchUnit dans le CI.
7. Documenter les éventuelles exceptions architecturales.

Les services encore sans code métier ne doivent pas recevoir artificiellement des classes ou composants uniquement pour respecter cette structure.

## 11. Références

- Architecture hexagonale — Alistair Cockburn.
- Domain-Driven Design — Eric Evans.
- Clean Architecture — Robert C. Martin.
- C4 Model — Simon Brown.
- Structurizr DSL.

---

**Fin de ADR-001**
