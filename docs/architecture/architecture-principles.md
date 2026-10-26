# Architecture Principles — Developer Content Creator

## 1. Objectif

Ce document définit les principes d'architecture du projet Developer Content Creator.

La plateforme analyse des dépôts GitHub et utilise des agents IA spécialisés pour
générer des contenus techniques : publications pour les réseaux sociaux, articles
de blog, newsletters et notes de version.

L'architecture doit favoriser :
- l'isolation du domaine métier des technologies ;
- la testabilité des cas d'utilisation ;
- la séparation des responsabilités ;
- l'évolution indépendante des contextes métier ;
- la fiabilité des échanges asynchrones ;
- la maintenabilité du système.

## 2. Principes directeurs

Le projet combine trois approches complémentaires :

- Domain-Driven Design (DDD) pour modéliser le métier et définir ses frontières ;
- architecture hexagonale pour isoler le domaine et les cas d'utilisation des
  systèmes externes ;
- Command Query Responsibility Segregation (CQRS) pour distinguer les opérations
  de modification des opérations de consultation.

L'architecture événementielle complète ces approches lorsque des échanges
asynchrones sont nécessaires.

Ces principes ne justifient pas, à eux seuls, l'introduction de complexité
technique supplémentaire. Toute abstraction doit répondre à un besoin identifié.

## 3. Architecture hexagonale

### 3.1. Domaine

Le domaine contient les concepts, les invariants et les règles métier.

Il peut inclure :
- des entités métier ;
- des objets-valeurs ;
- des agrégats et leurs racines lorsque cela est justifié ;
- des services de domaine ;
- des exceptions métier ;
- des événements de domaine.

Le domaine ne dépend pas de Spring, de JPA, de Kafka, du SDK GitHub ni d'un
fournisseur de modèle IA.

### 3.2. Application

La couche application implémente les cas d'utilisation.

Elle orchestre les opérations métier et utilise des ports pour accéder aux
capacités externes.

Elle peut contenir :
- des cas d'utilisation ;
- des commandes et leurs handlers ;
- des requêtes et leurs handlers ;
- des ports entrants ;
- des ports sortants ;
- la coordination transactionnelle des cas d'utilisation.

Elle ne doit pas contenir les détails d'implémentation des adaptateurs.

### 3.3. Adaptateurs entrants

Les adaptateurs entrants traduisent les interactions externes en appels
applicatifs.

Exemples :
- contrôleurs REST ;
- consommateurs Kafka ;
- déclencheurs techniques planifiés.

Ils ne doivent pas porter les règles métier.

### 3.4. Adaptateurs sortants

Les adaptateurs sortants implémentent les ports définis par l'application.

Exemples :
- persistance PostgreSQL/JPA ;
- intégration à l'API GitHub ;
- publication et consommation technique de messages ;
- appels à des fournisseurs de modèles IA.

Les DTO externes et les entités de persistance ne doivent pas devenir
automatiquement des objets du domaine.

### 3.5. Configuration

La configuration assemble les ports et leurs implémentations.

Les dépendances techniques sont injectées à cette frontière.

### 3.6. Règle de dépendance

Les dépendances du code doivent pointer vers le domaine et les abstractions
applicatives.

Le domaine ne doit pas importer les adaptateurs. L'application ne doit pas
dépendre des implémentations concrètes des adaptateurs.

## 4. Domain-Driven Design

### 4.1. Contextes métier initiaux

Les contextes suivants constituent une hypothèse initiale à valider :

| Contexte | Responsabilité | Module existant |
|---|---|---|
| Repository Management | Gestion des dépôts GitHub enregistrés | repository-service |
| AI Orchestration | Coordination des analyses et des agents IA | ai-orchestrator |
| Content Management | Gestion des contenus générés | content-service |
| Publication | Gestion du cycle de vie des publications | publication-service |
| API Access | Exposition et routage des API | gateway |

Ces correspondances ne signifient pas que chaque module est déjà un contexte
métier complètement défini. Les frontières devront être précisées à partir des
cas d'utilisation et du langage métier.

### 4.2. Modèle métier

Chaque contexte est responsable de son modèle métier.

Les entités, objets-valeurs, agrégats et événements sont introduits lorsqu'ils
répondent à un besoin métier réel.

Les invariants métier sont protégés par le domaine et ne doivent pas dépendre
uniquement de validations HTTP ou de contraintes de persistance.

### 4.3. Objets partagés

Le module common ne doit pas devenir un dépôt général de classes métier.

Un concept reste dans son contexte propriétaire tant qu'un besoin de partage
n'est pas démontré.

Les types partagés doivent avoir une sémantique stable et une responsabilité
claire. Les modèles internes d'un contexte ne doivent pas être exposés sans
nécessité aux autres contextes.

### 4.4. Intégrations externes

Le SDK GitHub encapsule les détails techniques de communication avec GitHub.

Les ports applicatifs définissent les besoins du cas d'utilisation. Les
adaptateurs traduisent ces besoins vers le SDK et traduisent les réponses
externes vers les modèles internes appropriés.

Les mêmes principes s'appliquent aux fournisseurs de modèles IA et aux autres
systèmes externes.

## 5. CQRS

### 5.1. Commandes

Une commande représente une intention de modifier l'état du système.

Exemples :
- RegisterRepository ;
- UpdateRepositoryMetadata ;
- StartRepositoryAnalysis ;
- GenerateContent ;
- ApproveContent ;
- SchedulePublication.

Une commande est traitée par un cas d'utilisation qui applique les règles
métier nécessaires.

### 5.2. Requêtes

Une requête demande des informations sans modifier l'état métier.

Exemples :
- GetRepositoryById ;
- ListRepositories ;
- GetAnalysisStatus ;
- GetGeneratedContent ;
- GetPublicationHistory.

Les requêtes peuvent utiliser des projections adaptées au besoin de lecture,
sans imposer systématiquement le chargement d'un agrégat complet.

### 5.3. Niveau d'application

CQRS est d'abord appliqué au niveau du code, au sein de chaque service.

Le modèle de lecture et le modèle d'écriture peuvent utiliser la même base
PostgreSQL.

La séparation des bases de données, les projections asynchrones et les
infrastructures spécialisées ne seront introduites que si des besoins mesurables
les justifient.

CQRS n'implique pas Event Sourcing.

## 6. Architecture événementielle

Kafka est utilisé lorsque les besoins métier ou techniques justifient une
communication asynchrone.

Les événements doivent représenter un fait métier accompli, avec un nom et un
contrat explicites.

Les commandes envoyées à un composant ne doivent pas être confondues avec les
événements décrivant un fait déjà survenu.

Les consommateurs doivent tenir compte des livraisons répétées et des risques
de traitement en double.

Lorsqu'une modification de base de données doit entraîner une publication
fiable d'événement, le Transactional Outbox sera évalué afin d'éviter une
incohérence entre l'écriture en base et la publication Kafka.

Les contrats et les versions d'événements doivent être gérés explicitement.

## 7. Transactions et cohérence

Les transactions locales protègent les modifications relevant d'une même
frontière transactionnelle.

Les opérations distribuées entre plusieurs services ne doivent pas dépendre
d'une transaction SQL globale.

Les workflows asynchrones devront définir :
- les états métier et leurs transitions autorisées ;
- les règles de reprise ;
- l'idempotence ;
- la gestion des échecs ;
- les éventuelles compensations métier.

Les mécanismes de cohérence éventuelle doivent être documentés lorsqu'ils
s'appliquent.

## 8. Organisation du code

Les modules Maven existants sont conservés à ce stade.

Les services métier organisent leur code selon les responsabilités suivantes :

- domain ;
- application ;
- adapter.in ;
- adapter.out ;
- configuration.

Cette organisation logique n'impose pas de créer immédiatement un module Maven
distinct pour chaque couche.

Les modules partagés restent dédiés à des responsabilités transversales ou à
des abstractions réellement communes.

## 9. Stratégie de tests

Les tests doivent couvrir les différents niveaux de l'architecture.

### Domaine
- invariants des entités et objets-valeurs ;
- règles métier ;
- transitions d'état ;
- absence de dépendances techniques inutiles.

### Application
- exécution des cas d'utilisation ;
- succès et échecs métier ;
- interactions avec les ports ;
- comportement transactionnel attendu.

### Adaptateurs
- contrats HTTP ;
- validation et traduction des erreurs ;
- mapping de persistance ;
- intégration avec les systèmes externes.

### Échanges asynchrones
- compatibilité des contrats ;
- idempotence des consommateurs ;
- gestion des échecs et des reprises ;
- cohérence de la publication des événements.

Les tests unitaires du domaine ne doivent pas nécessiter le démarrage de Spring
ou d'une infrastructure externe.

## 10. Contrôles d'architecture dans le CI

L'industrialisation devra progressivement vérifier :

- la compilation et les tests ;
- le formatage et les conventions de code ;
- les dépendances entre modules ;
- l'absence de dépendances interdites du domaine vers les technologies ;
- les contrats et la compatibilité des événements ;
- la couverture des tests, selon des seuils définis et justifiés.

Les contrôles doivent être exécutables de manière reproductible dans le CI.

## 11. Décisions reportées

Les décisions suivantes restent ouvertes et nécessitent une analyse dédiée :

- frontières définitives des contextes métier ;
- règles de versionnement indépendant des services et bibliothèques ;
- stratégie exacte de publication des événements ;
- adoption éventuelle de projections CQRS asynchrones ;
- nécessité éventuelle d'Event Sourcing ;
- choix des outils de tests d'architecture ;
- stratégie de versionnement et de compatibilité des contrats.

Aucune de ces décisions ne doit être considérée comme acquise avant validation.

## 12. Principe d'évolution

L'architecture est appliquée progressivement, à partir de cas d'utilisation réels.

Chaque nouvelle abstraction doit avoir une justification métier ou technique.
Les décisions structurantes doivent être documentées, idéalement sous la forme
d'Architecture Decision Records (ADR) lorsqu'elles méritent une trace dédiée.
