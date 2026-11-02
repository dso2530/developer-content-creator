# ADR-003 — Sélection explicite des dépôts GitHub via GitHub App

- **Statut :** Accepté
- **Date :** 2026-11-02
- **Décideurs :** Équipe Developer Content Creator
- **Périmètre :** Sprint 2 — GitHub Integration

## 1. Contexte

Developer Content Creator permet aux développeurs de connecter leurs dépôts GitHub afin de les analyser et de générer automatiquement du contenu technique.

L'intégration repose sur une GitHub App.

Lors de son installation, GitHub permet de définir les dépôts auxquels l'application peut accéder. Deux stratégies fonctionnelles sont envisagées pour intégrer ces dépôts dans Developer Content Creator.

### Parcours A — Sélection explicite des dépôts

1. L'utilisateur connecte son compte GitHub.
2. Il installe la GitHub App sur son compte personnel ou son organisation.
3. Il sélectionne les dépôts auxquels l'application est autorisée à accéder.
4. Developer Content Creator récupère les dépôts accessibles par cette installation.
5. L'utilisateur sélectionne les dépôts qu'il souhaite enregistrer dans la plateforme.
6. Seuls les dépôts sélectionnés sont enregistrés et intégrés au processus d'analyse.

### Parcours B — Synchronisation automatique

1. L'utilisateur installe la GitHub App.
2. Il autorise l'accès aux dépôts souhaités, éventuellement à tous les dépôts.
3. Developer Content Creator synchronise automatiquement les dépôts accessibles.
4. Les dépôts autorisés sont intégrés sans sélection individuelle supplémentaire dans la plateforme.

## 2. Décision

Nous retenons le **parcours A : sélection explicite des dépôts**.

L'installation de la GitHub App autorise techniquement l'accès à certains dépôts, mais cette autorisation ne vaut pas automatiquement inscription de ces dépôts dans Developer Content Creator.

L'utilisateur doit effectuer une sélection explicite dans notre plateforme.

Nous distinguons donc deux notions :

- **Autorisation GitHub :** périmètre des dépôts auxquels notre GitHub App peut accéder.
- **Enregistrement métier :** dépôts que l'utilisateur souhaite effectivement exploiter dans Developer Content Creator.

Un dépôt ne peut être enregistré que si l'installation GitHub concernée dispose effectivement des permissions nécessaires pour y accéder.

## 3. Motivations

### 3.1 Sécurité et moindre privilège

La sélection explicite permet de limiter les traitements aux dépôts réellement utilisés.

Elle évite d'analyser inutilement des dépôts auxquels l'utilisateur a autorisé l'accès sans souhaiter les exploiter dans notre plateforme.

### 3.2 Maîtrise fonctionnelle

L'utilisateur conserve le contrôle sur les projets qu'il souhaite connecter.

Il peut distinguer ses dépôts personnels, professionnels, expérimentaux ou confidentiels.

### 3.3 Maîtrise des ressources

L'analyse de dépôts peut entraîner :
- Des appels à l'API GitHub.
- Des traitements asynchrones.
- Des opérations d'indexation.
- Des appels à des modèles d'intelligence artificielle.
- Du stockage de métadonnées et de contenu.

La sélection explicite évite de déclencher ces traitements pour tous les dépôts accessibles.

### 3.4 Évolutivité

Cette approche permet d'ajouter ultérieurement :
- Une sélection multiple.
- Une synchronisation périodique des dépôts enregistrés.
- Une détection des nouveaux dépôts autorisés.
- Une gestion des dépôts désactivés ou supprimés.
- Une activation explicite de l'analyse automatique.

## 4. Conséquences architecturales

### 4.1 Modèle métier

Les concepts suivants sont conservés :

| Concept | Responsabilité |
|---|---|
| GitHubAccount | Représenter le compte GitHub utilisateur ou organisation |
| GitHubInstallation | Représenter l'installation de notre GitHub App |
| RegisteredRepository | Représenter un dépôt enregistré dans Developer Content Creator |

Une installation peut donner accès à plusieurs dépôts.

Un dépôt enregistré est associé à une installation GitHub.

La relation entre installation et dépôt est donc de type 1:N.

### 4.2 Cas d'utilisation

Le service `repository-service` doit notamment prendre en charge :

- La récupération des installations GitHub.
- La récupération des dépôts accessibles par une installation.
- L'enregistrement explicite d'un dépôt.
- La vérification des doublons.
- La suppression ou désactivation d'un dépôt enregistré.
- La gestion des changements de permissions GitHub.

### 4.3 Séparation des responsabilités

Le domaine ne dépend pas du SDK GitHub.

L'application utilise des ports sortants pour accéder aux installations, aux dépôts GitHub et à la persistance.

Les adaptateurs d'infrastructure implémentent ces ports.

### 4.4 Sécurité applicative

Chaque demande d'enregistrement doit vérifier :

1. L'identité de l'utilisateur connecté.
2. Son autorisation à utiliser l'installation concernée.
3. L'existence de l'installation.
4. L'accessibilité effective du dépôt via cette installation.
5. L'absence d'un enregistrement déjà existant.

L'identifiant `requestedBy` ne doit jamais être considéré comme une preuve d'identité : il doit être établi ou vérifié à partir du contexte d'authentification de la plateforme.

### 4.5 Intégrité et persistance des dépôts enregistrés

Un dépôt GitHub ne peut être enregistré qu'une seule fois pour une installation GitHub donnée.

La base de données garantit cette règle au moyen d'une contrainte d'unicité portant sur les colonnes :

- `installation_id`
- `github_repository_id`

Cette contrainte constitue la garantie d'intégrité définitive, y compris lorsque plusieurs requêtes concurrentes tentent d'enregistrer le même dépôt.

La vérification préalable de l'existence d'un enregistrement dans le service applicatif améliore la lisibilité du traitement, mais ne remplace pas la contrainte SQL.

Une violation de cette contrainte doit être traduite par l'adaptateur de persistance en une exception métier `RepositoryAlreadyRegisteredException`.

Aucune clé étrangère vers une table d'installations GitHub n'est créée à ce stade.

Le modèle de persistance des installations et leur association avec les utilisateurs Developer Content Creator seront définis dans une décision ultérieure.

Cette séparation évite de coupler prématurément deux agrégats dont les responsabilités et les règles de cycle de vie restent distinctes.

## 5. Gestion des événements GitHub

Le système devra prendre en compte les événements liés au cycle de vie de la GitHub App :

- Installation créée.
- Installation supprimée.
- Installation suspendue ou réactivée.
- Dépôt ajouté à une installation.
- Dépôt retiré d'une installation.
- Permissions modifiées.

Ces événements devront permettre de maintenir la cohérence entre les autorisations GitHub et les enregistrements locaux.

Un dépôt retiré de l'installation ne doit plus être considéré comme accessible, même si son enregistrement métier existe encore.

La politique de conservation des données et de désactivation de l'enregistrement sera définie séparément.

## 6. Alternatives considérées

| Critère | Parcours A | Parcours B |
|---|---|---|
| Contrôle utilisateur | Élevé | Plus limité dans la plateforme |
| Simplicité du MVP | Bonne | Moyenne |
| Volume de synchronisation | Maîtrisé | Potentiellement élevé |
| Consommation API GitHub | Limitée aux besoins | Potentiellement importante |
| Risque de traitement inutile | Faible | Plus élevé |
| Automatisation | Progressive | Forte |

Le parcours B n'est pas retenu pour le MVP.

Il pourra être réévalué si les besoins fonctionnels évoluent.

## 7. Risques et points de vigilance

- Une installation GitHub peut être modifiée indépendamment de Developer Content Creator.
- Un dépôt enregistré peut devenir inaccessible.
- Les webhooks peuvent être reçus plusieurs fois ou dans un ordre différent.
- Une installation peut être suspendue ou supprimée.
- Les permissions de l'application peuvent être modifiées.
- Les opérations de synchronisation doivent être idempotentes.
- Les contraintes d'intégrité en base doivent protéger les données contre les opérations concurrentes.

Ces situations devront être traitées dans les cas d'utilisation et les adaptateurs correspondants.

## 8. Conséquences de la décision

### Positives

- Meilleur contrôle utilisateur.
- Réduction du périmètre de traitement.
- Respect du principe de moindre privilège.
- Maîtrise des coûts d'analyse et d'indexation.
- Architecture compatible avec une automatisation ultérieure.
- Garantie d'unicité des dépôts enregistrés pour une installation donnée.
- Séparation des responsabilités entre les modèles de persistance des installations et des dépôts.

### Négatives

- Une étape supplémentaire de sélection dans l'interface.
- Une gestion plus précise des états d'autorisation et d'enregistrement.
- La nécessité de maintenir la cohérence entre GitHub et notre base de données.
- La nécessité de traduire les violations de contraintes SQL en erreurs métier compréhensibles.

## 9. Références

- GitHub Apps — Documentation officielle : https://docs.github.com/en/apps
- GitHub Apps — Installation et gestion des installations : https://docs.github.com/en/apps/using-github-apps/installing-your-own-github-app
- Architecture hexagonale du projet.
- Sprint 2 — GitHub Integration.

## 10. Historique

| Date | Version | Modification |
|---|---|---|
| 2026-11-02 | 1.0 | Décision initiale : sélection explicite des dépôts |
| 2026-11-02 | 1.1 | Formalisation des règles d'intégrité et de persistance des dépôts enregistrés |