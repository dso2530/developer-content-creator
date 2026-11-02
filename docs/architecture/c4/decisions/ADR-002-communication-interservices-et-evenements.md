# ADR-002 — Communication interservices et contrats événementiels

- **Statut :** Accepted
- **Date :** 2026-10-31
- **Décideurs :** Équipe architecture
- **Concerne :** Tous les microservices du backend
- **Dépendances :** ADR-001 — Architecture hexagonale

## 1. Contexte

Le projet `developer-content-creator` adopte une architecture composée de plusieurs microservices indépendants :

- `repository-service`
- `ai-orchestrator`
- `content-service`
- `publication-service`
- `gateway`

Les échanges entre ces services doivent permettre :

- des interactions synchrones lorsque le résultat est immédiatement nécessaire ;
- des traitements asynchrones pour les opérations longues ;
- le découplage des services ;
- la traçabilité des traitements distribués ;
- la fiabilité des événements malgré les interruptions et les redémarrages ;
- l'évolution contrôlée des contrats d'intégration.

Les dépendances Spring Kafka et Apache Avro sont déjà déclarées dans `messaging-starter`.

En revanche, aucun producteur, consommateur, contrat Avro, Schema Registry ou mécanisme Outbox n'est encore implémenté.

Cette ADR définit les conventions qui devront guider leur implémentation.

## 2. Décision générale

L'architecture adopte une communication hybride :

- REST pour les interactions synchrones ;
- Apache Kafka pour les échanges asynchrones ;
- Apache Avro pour la sérialisation des événements ;
- Apicurio Registry pour la gouvernance des schémas ;
- Transactional Outbox et Inbox pour la fiabilité des échanges.

Les microservices conservent leur autonomie métier et leurs propres modèles internes.

Les événements Kafka constituent des contrats d'intégration indépendants des entités de persistance et des modèles métier internes.

## 3. Communication interservices

### 3.1 REST — Communication synchrone

REST est utilisé lorsqu'un service a besoin d'une réponse immédiate d'un autre service.

Cas d'utilisation :

- consultation de données ;
- récupération d'informations nécessaires à une commande ;
- opérations nécessitant une réponse immédiate ;
- API exposées aux clients via le Gateway.

Conventions :

- HTTP/JSON ;
- API documentées avec OpenAPI ;
- délais d'attente explicites ;
- propagation du correlation ID ;
- gestion homogène des erreurs ;
- absence de dépendance directe aux entités internes des autres services.

Les appels synchrones en cascade doivent être limités afin d'éviter le couplage temporel et la propagation des indisponibilités.

### 3.2 Kafka — Communication asynchrone

Apache Kafka est retenu pour les événements d'intégration et les traitements distribués.

Exemples :

- analyse d'un dépôt GitHub terminée ;
- génération d'un contenu terminée ;
- publication d'un contenu terminée ;
- échec définitif d'une opération métier.

Les producteurs ne connaissent pas les implémentations internes des consommateurs.

Les événements sont publiés dans des topics dédiés aux domaines fonctionnels.

Convention de nommage :

`<domain>.<event-name>`

Exemples :

- `repository.analysis.completed`
- `content.generation.completed`
- `publication.completed`

La version du contrat est gérée par le Schema Registry et le versionnage du module Maven, plutôt que par une multiplication systématique des topics.

La clé Kafka doit permettre de préserver l'ordre des événements concernant une même ressource métier lorsque cet ordre est nécessaire.

Exemples :

- identifiant du dépôt GitHub ;
- identifiant du contenu ;
- identifiant de la publication.

## 4. Sérialisation et contrats Avro

### 4.1 Choix d'Apache Avro

Apache Avro est retenu comme format de sérialisation des événements Kafka.

Motivations :

- format binaire compact ;
- contrats explicites ;
- génération de classes Java ;
- prise en charge de l'évolution des schémas ;
- intégration avec Apicurio Registry.

Les événements ne doivent pas exposer :

- des entités JPA ;
- des agrégats métier ;
- des objets internes de Spring ;
- des objets de configuration ;
- des informations techniques propres à l'implémentation d'un service.

### 4.2 Centralisation des contrats

Les contrats Avro sont centralisés dans le module Maven :

`backend/shared/event-contracts`

Structure cible :

```text
backend/shared/event-contracts/
├── pom.xml
└── src/
    └── main/
        └── avro/
            ├── common/
            │   └── EventMetadata.avsc
            ├── repository/
            │   └── RepositoryAnalyzed.avsc
            ├── content/
            │   └── ContentGenerated.avsc
            └── publication/
                └── ContentPublished.avsc
```

Ce module :

- contient exclusivement les contrats d'intégration ;
- ne contient aucune logique métier ;
- ne contient aucune entité de persistance ;
- ne contient aucun producteur ou consommateur Kafka ;
- génère les classes Java à partir des schémas `.avsc`.

Le plugin `avro-maven-plugin` assure la génération des classes Java.

Les services qui produisent ou consomment un événement déclarent une dépendance vers ce module.

Le versionnage du module doit être gouverné afin de ne pas imposer une mise à jour simultanée de tous les microservices.

### 4.3 Métadonnées communes

Les événements doivent transporter les informations nécessaires à leur identification et à leur traçabilité.

Convention minimale :

| Champ | Type logique | Description |
|---|---|---|
| `eventId` | UUID | Identifiant unique de l'événement |
| `eventType` | String | Type fonctionnel de l'événement |
| `occurredAt` | Timestamp | Date de production de l'événement |
| `producer` | String | Service producteur |
| `subject` | String | Ressource métier concernée |
| `correlationId` | String | Identifiant de corrélation |
| `causationId` | String nullable | Événement ou commande à l'origine |
| `data` | Record Avro | Données métier exposées par le contrat |

Les métadonnées doivent être communes à tous les événements.

Les champs métier restent propres à chaque contrat.

Les données personnelles ou sensibles ne doivent pas être incluses dans les événements sauf nécessité métier documentée.

### 4.4 Évolution des schémas

La politique de compatibilité retenue est `BACKWARD_TRANSITIVE`.

Toute évolution doit préserver la capacité des consommateurs utilisant le schéma courant à lire les messages produits avec les versions précédentes.

Conventions :

- ajouter les nouveaux champs avec des valeurs par défaut compatibles ;
- éviter la suppression ou le renommage des champs existants ;
- ne pas modifier arbitrairement le type d'un champ ;
- ne pas réutiliser un champ supprimé pour une autre signification ;
- tester la compatibilité avant l'enregistrement d'une nouvelle version.

Les changements incompatibles nécessitent une stratégie de migration explicitement documentée.

## 5. Schema Registry

### 5.1 Choix d'Apicurio Registry

Apicurio Registry 3.x est retenu comme référentiel central des schémas.

L'API native Registry v3 sera utilisée :

`/apis/registry/v3`

Le mode de compatibilité Confluent ne sera utilisé que si un besoin d'interopérabilité l'impose.

Apicurio Registry assure :

- le stockage des schémas ;
- leur versionnage ;
- leur distribution aux producteurs et consommateurs ;
- la validation des règles de compatibilité ;
- la résolution des schémas nécessaires à la désérialisation.

### 5.2 Intégration Maven

Deux responsabilités doivent rester distinctes :

- `avro-maven-plugin` : génération des classes Java depuis les fichiers `.avsc` ;
- `apicurio-registry-maven-plugin` : gestion et enregistrement des artefacts dans Apicurio Registry.

L'enregistrement dans le Registry ne doit pas être obligatoire pendant chaque compilation locale.

Il sera exécuté dans une étape CI/CD dédiée, après validation des contrats.

Une compilation Maven locale doit rester possible sans disponibilité d'Apicurio Registry.

## 6. Fiabilité des événements

### 6.1 Garantie de livraison

Le système adopte une garantie de livraison **at-least-once**.

Un événement peut être livré plusieurs fois.

Les producteurs et consommateurs doivent donc être conçus pour supporter les doublons.

La garantie exactly-once de Kafka ne sera pas considérée comme une garantie transactionnelle couvrant simultanément PostgreSQL et Kafka.

### 6.2 Transactional Outbox

Le pattern Transactional Outbox est obligatoire pour publier un événement correspondant à une modification persistée dans PostgreSQL.

Principe :

1. Le service exécute une opération métier.
2. La modification métier et l'enregistrement de l'événement Outbox sont effectués dans la même transaction PostgreSQL.
3. La transaction est validée.
4. Un publisher récupère les événements prêts à être publiés.
5. Le publisher transmet l'événement à Kafka.
6. Après confirmation de publication, l'événement Outbox est marqué comme publié.

États minimaux :

- `READY_TO_PUBLISH`
- `MARKED_AS_PUBLISHED`

Le publisher doit supporter plusieurs instances et éviter les traitements concurrents du même événement.

La stratégie de réservation des événements doit permettre l'utilisation de mécanismes PostgreSQL tels que `FOR UPDATE SKIP LOCKED`.

En cas d'arrêt après l'accusé de réception Kafka mais avant la mise à jour PostgreSQL, l'événement peut être publié une seconde fois.

Ce comportement est attendu et doit être compensé par l'idempotence des consommateurs.

### 6.3 Idempotence des consommateurs

Chaque consommateur doit garantir qu'un même événement ne produit pas plusieurs fois les mêmes effets métier.

Une Inbox persistée en base de données sera utilisée.

Elle conserve notamment :

- `event_id`
- `consumer_name`
- `processed_at`

Une contrainte d'unicité sur le couple `(event_id, consumer_name)` empêche le retraitement d'un événement déjà appliqué par ce consommateur.

L'enregistrement Inbox et les modifications métier doivent appartenir à la même transaction PostgreSQL.

L'offset Kafka ne doit être validé qu'après la réussite du traitement applicatif et de sa transaction de persistance.

## 7. Gestion des erreurs et reprise

### 7.1 Classification des erreurs

Les erreurs sont réparties en deux catégories.

**Erreurs temporaires :**

- indisponibilité momentanée de PostgreSQL ;
- timeout d'un service externe ;
- indisponibilité temporaire d'une dépendance ;
- erreur réseau transitoire.

Ces erreurs peuvent faire l'objet d'une nouvelle tentative.

**Erreurs définitives :**

- contrat invalide ;
- données métier non conformes ;
- événement impossible à traiter ;
- erreur de désérialisation ;
- incompatibilité de schéma.

Ces erreurs ne doivent pas être répétées indéfiniment.

### 7.2 Politique de retry

La première version utilisera :

- un nombre maximal de tentatives configurable ;
- un backoff exponentiel ;
- un délai maximal configurable ;
- une classification des exceptions ;
- une journalisation des tentatives.

Les tentatives seront réalisées avant de considérer le message comme définitivement en échec.

Le traitement d'un message en retry bloquant temporairement sa partition est accepté pour la première version afin de conserver une sémantique d'ordre plus simple.

Les conséquences sur le débit devront être mesurées.

### 7.3 Dead Letter Topic

Après épuisement des tentatives, le message est transféré dans un Dead Letter Topic.

Convention :

`<topic>.dlt`

Exemples :

- `repository.analysis.completed.dlt`
- `content.generation.completed.dlt`

Les informations suivantes doivent être conservées :

- événement original ;
- topic d'origine ;
- partition et offset d'origine ;
- date de l'échec ;
- exception et cause ;
- nombre de tentatives ;
- identifiant de corrélation.

### 7.4 Rejeu des événements

Le rejeu d'un message depuis un DLT doit être contrôlé.

Il nécessite :

- l'identification de la cause de l'échec ;
- la correction du problème ;
- la validation du contrat ;
- une autorisation de rejeu ;
- la conservation de l'identifiant d'événement d'origine ;
- une traçabilité de l'opération.

Le rejeu ne doit pas provoquer de double effet métier.

L'Inbox doit donc rester active pendant les opérations de replay.

## 8. Configuration des producteurs Kafka

Les producteurs doivent appliquer les paramètres suivants :

| Paramètre | Décision |
|---|---|
| `acks` | `all` |
| `enable.idempotence` | `true` |
| `retries` | Activés |
| `key.serializer` | String |
| `value.serializer` | Apicurio Avro SerDe |
| `client.id` | Identifiant du service |
| `delivery.timeout.ms` | Valeur configurable |
| `request.timeout.ms` | Valeur configurable |

La clé Kafka doit être déterminée selon le besoin d'ordre métier.

L'idempotence native du producteur complète le mécanisme Outbox, mais ne le remplace pas.

## 9. Configuration des consommateurs Kafka

Les consommateurs doivent appliquer les conventions suivantes :

- `enable.auto.commit=false` ;
- validation de l'événement avant traitement ;
- traitement métier transactionnel ;
- enregistrement Inbox ;
- commit de l'offset après réussite du traitement ;
- gestion explicite des erreurs ;
- identification claire du consumer group.

Chaque service dispose de consumer groups indépendants.

Le nombre de partitions doit être dimensionné selon le débit attendu et les besoins de parallélisation.

L'ordre est garanti au sein d'une partition, et non globalement entre toutes les partitions.

## 10. Sécurité Kafka

### 10.1 Chiffrement

TLS est obligatoire pour les communications Kafka en environnement partagé et en production.

Les certificats doivent être gérés et renouvelés de manière centralisée.

### 10.2 Authentification

Le mécanisme retenu est :

`SASL_SSL` avec `SCRAM-SHA-512`.

Chaque microservice possède une identité technique dédiée.

Les identifiants ne doivent jamais être stockés dans les fichiers YAML versionnés ou dans les images de conteneurs.

Ils doivent être injectés au runtime depuis un gestionnaire de secrets.

### 10.3 Autorisation

Les ACL Kafka appliquent le principe du moindre privilège.

Exemple :

- `repository-service` peut publier sur les topics du domaine repository ;
- `ai-orchestrator` peut consommer les événements repository autorisés ;
- `content-service` peut publier les événements content ;
- `publication-service` peut consommer les événements content autorisés et publier les événements publication.

Aucun microservice ne doit disposer de droits administrateur Kafka en fonctionnement normal.

Les droits de lecture, écriture et gestion des consumer groups doivent être séparés.

### 10.4 Schema Registry

L'accès à Apicurio Registry doit également être protégé.

Les applications disposent uniquement des droits nécessaires à la consultation des schémas.

L'enregistrement et la modification des contrats sont réservés aux pipelines autorisés.

## 11. Traçabilité et observabilité

Chaque événement doit permettre de retrouver son origine et son parcours.

Les champs suivants sont obligatoires :

- `eventId`
- `correlationId`
- `producer`
- `occurredAt`

`causationId` est renseigné lorsqu'un événement est déclenché par un autre événement.

Les traces distribuées doivent être propagées entre les appels REST et les échanges Kafka lorsque cela est possible.

Les métriques minimales comprennent :

- nombre d'événements publiés ;
- nombre d'événements consommés ;
- erreurs de publication ;
- erreurs de consommation ;
- nombre de retries ;
- taille et ancienneté de l'Outbox ;
- nombre de messages dans les DLT ;
- retard des consumer groups.

Les payloads complets ne doivent pas être inscrits systématiquement dans les logs.

## 12. Organisation des modules Maven

Structure cible :

```text
backend/
├── shared/
│   ├── common/
│   ├── event-contracts/
│   ├── messaging-starter/
│   └── observability-starter/
│
├── repository-service/
├── ai-orchestrator/
├── content-service/
├── publication-service/
└── gateway/
```

Responsabilités :

**event-contracts**
- contrats Avro ;
- génération des classes Java ;
- versionnage des contrats.

**messaging-starter**
- configuration Spring Kafka ;
- configuration des SerDes ;
- conventions producteur/consommateur ;
- gestion des erreurs ;
- configuration des retries et DLT ;
- intégration des mécanismes communs de messagerie.

**Chaque microservice**
- ses cas d'utilisation ;
- ses ports applicatifs ;
- ses adaptateurs Kafka ;
- ses événements métier internes ;
- son Outbox et son Inbox lorsque nécessaires.

Le starter ne doit pas contenir de logique métier propre à un service.

## 13. Conséquences

### Avantages

- découplage temporel des microservices ;
- contrats d'intégration explicites ;
- évolution maîtrisée des événements ;
- résilience face aux indisponibilités temporaires ;
- prévention des doubles traitements ;
- traçabilité des échanges distribués ;
- centralisation des conventions techniques.

### Inconvénients et coûts

- infrastructure Kafka et Apicurio à maintenir ;
- complexité supplémentaire liée à Outbox et Inbox ;
- nécessité de gouverner les contrats ;
- gestion des doublons et des événements en échec ;
- supervision des DLT et des retards de consommation ;
- coût de maintenance des versions de contrats.

## 14. Plan d'implémentation

L'implémentation sera progressive.

**Étape 1 — Contrats**
- créer `event-contracts` ;
- configurer le plugin Avro ;
- créer les premiers schémas ;
- générer les classes Java ;
- ajouter les tests de compatibilité.

**Étape 2 — Registry**
- déployer Apicurio Registry ;
- configurer la persistance ;
- définir les règles de compatibilité ;
- intégrer l'enregistrement des schémas dans la CI.

**Étape 3 — Messaging Starter**
- configurer les producteurs ;
- configurer les consommateurs ;
- intégrer les SerDes Avro ;
- configurer les retries et DLT ;
- définir les conventions de sécurité.

**Étape 4 — Fiabilité**
- implémenter Outbox ;
- implémenter Inbox ;
- tester les doublons ;
- tester les interruptions et les reprises ;
- tester les erreurs après accusé de réception Kafka.

**Étape 5 — Service pilote**
- intégrer le premier événement dans `repository-service` ;
- connecter `ai-orchestrator` comme consommateur ;
- vérifier le contrat, la livraison, l'idempotence et la traçabilité.

**Étape 6 — Généralisation**
- étendre progressivement les échanges aux autres services ;
- ajouter les dashboards et alertes ;
- documenter les procédures de rejeu.

## 15. Tests d'architecture et de fiabilité

Les tests suivants sont obligatoires avant généralisation :

- génération des classes Java depuis les schémas Avro ;
- validation de compatibilité des contrats ;
- sérialisation et désérialisation ;
- refus d'un événement non conforme ;
- publication d'un événement après commit de la transaction métier ;
- absence d'événement Outbox après rollback métier ;
- récupération des événements Outbox après redémarrage ;
- prise en charge du crash après accusé de réception Kafka ;
- absence de double effet métier lors d'une livraison répétée ;
- application correcte du backoff ;
- transfert vers le DLT après épuisement des retries ;
- rejeu contrôlé d'un message DLT ;
- respect des ACL ;
- propagation du correlation ID.

## 16. Alternatives étudiées

### REST uniquement

Non retenu comme mécanisme unique.

Il crée un couplage temporel entre les services et complique les traitements longs et les reprises.

### JSON pour tous les événements

Non retenu comme format principal.

Avro apporte des contrats formels et une gestion structurée de l'évolution des schémas.

JSON reste adapté aux API REST.

### Contrats Avro indépendants dans chaque microservice

Non retenu.

Le choix de centralisation dans `event-contracts` facilite la gouvernance et la génération homogène des contrats.

### Livraison exactly-once comme garantie globale

Non retenue.

Elle ne garantit pas à elle seule une transaction atomique entre PostgreSQL et Kafka.

Outbox, Inbox et idempotence apportent une garantie adaptée aux contraintes du système.

### Enregistrement des schémas à chaque compilation

Non retenu.

Une compilation locale ne doit pas dépendre de la disponibilité du Registry ni modifier implicitement les contrats partagés.

## 17. Conditions d'acceptation

Cette ADR pourra passer au statut `Accepted` après validation des points suivants :

- [ ] REST et Kafka ont des responsabilités clairement séparées.
- [ ] Le module Maven `event-contracts` est retenu.
- [ ] Avro est retenu comme format des événements.
- [ ] Apicurio Registry 3.x est retenu.
- [ ] La politique de compatibilité est définie.
- [ ] Outbox et Inbox sont obligatoires pour les flux persistés.
- [ ] La livraison at-least-once est acceptée.
- [ ] La politique de retry et DLT est définie.
- [ ] La sécurité Kafka est définie.
- [ ] Les tests de fiabilité sont documentés.
- [ ] Le plan d'implémentation est validé.