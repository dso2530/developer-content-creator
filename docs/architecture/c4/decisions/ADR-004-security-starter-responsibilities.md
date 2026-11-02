# ADR-004 — Responsabilités du security-starter

- **Statut :** Accepté
- **Date :** 2026-11-02
- **Décideurs :** Architecture du projet Developer Content Creator

## 1. Contexte

Les différents microservices de Developer Content Creator doivent partager un socle commun d'authentification et d'identification des utilisateurs.

Le projet utilise Spring Security et OAuth2 Resource Server avec des jetons JWT.

Cependant, les besoins d'autorisation HTTP varient selon les services : le gateway, le repository-service et les autres microservices n'exposent pas les mêmes ressources et ne nécessitent pas les mêmes règles d'accès.

Un starter commun ne doit donc pas imposer une politique de sécurité HTTP uniforme à l'ensemble des applications.

## 2. Décision

Le module `security-starter` fournit un socle commun d'authentification et d'identité utilisateur.

Il ne prend pas en charge la définition des règles d'autorisation HTTP propres à chaque microservice.

### 2.1 Responsabilités du security-starter

Le starter fournit :

- l'intégration avec Spring Security et OAuth2 Resource Server ;
- l'accès aux informations de l'utilisateur authentifié ;
- l'extraction du subject (`sub`) du JWT ;
- l'extraction de l'identifiant utilisateur interne DCC depuis un claim configurable ;
- l'extraction du nom d'utilisateur depuis un claim configurable ;
- l'accès aux autorités accordées à l'utilisateur ;
- un fournisseur commun `CurrentUserProvider` ;
- une configuration centralisée sous `developer-content-creator.security`.

Les claims utilisés sont configurables :

| Propriété | Valeur par défaut |
|---|---|
| `user-id-claim` | `dcc_user_id` |
| `username-claim` | `preferred_username` |
| `authorities-claim` | `roles` |

Le starter ne suppose pas que le claim `sub` contient un UUID.

L'identifiant interne DCC doit provenir explicitement du claim configuré.

L'absence ou l'invalidité de l'identifiant interne doit produire une erreur explicite lorsqu'une opération exige cet identifiant.

### 2.2 Responsabilités des microservices

Chaque microservice reste responsable :

- de la déclaration de sa `SecurityFilterChain` ;
- de ses règles `authorizeHttpRequests` ;
- de ses routes publiques et protégées ;
- de ses règles d'accès par rôle ou autorité ;
- de ses éventuelles règles métier d'autorisation.

Le starter ne crée donc aucune `SecurityFilterChain` par défaut.

Il peut exposer des composants réutilisables, notamment un convertisseur JWT vers les autorités Spring Security, que chaque application pourra utiliser dans sa propre configuration.

### 2.3 Validation des JWT

La validation cryptographique et la validation de l'émetteur du JWT restent déléguées à Spring Security et à l'auto-configuration Spring Boot.

Le starter ne crée pas de `JwtDecoder` concurrent lorsque Spring Boot en configure déjà un à partir de `issuer-uri` ou `jwk-set-uri`.

Aucun couplage direct à Keycloak n'est introduit.

### 2.4 Activation

Le starter utilise le préfixe :

`developer-content-creator.security`

Il est activé par défaut et peut être désactivé explicitement :

```yaml
developer-content-creator:
  security:
    enabled: false
```

Les applications conservent la possibilité de fournir leurs propres beans et configurations de sécurité.

## 3. Alternatives étudiées

### A. Starter limité à l'identité et aux composants communs

Le starter fournit l'identité utilisateur et les composants réutilisables. Chaque microservice configure ses propres règles HTTP.

**Avantages :**

- faible couplage entre les services ;
- politiques d'accès adaptées à chaque application ;
- intégration progressive ;
- absence de configuration HTTP imposée.

### B. Starter de sécurité HTTP complet

Le starter fournit également une `SecurityFilterChain` et des règles d'autorisation communes.

**Inconvénients :**

- risque d'imposer des règles inadaptées à certains services ;
- personnalisation plus complexe ;
- risque de conflit avec les configurations de sécurité applicatives.

### Décision retenue

L'alternative A est retenue.

## 4. Conséquences

### Positives

- Les services partagent une représentation cohérente de l'utilisateur courant.
- Les règles d'accès HTTP restent indépendantes.
- Le projet ne dépend pas d'un fournisseur d'identité particulier.
- Les composants communs peuvent évoluer sans imposer une politique d'autorisation globale.

### Contraintes

- Chaque microservice doit déclarer sa propre configuration HTTP de sécurité.
- Les claims JWT doivent être documentés et configurés.
- La conversion des claims en autorités Spring Security doit rester configurable et testée.
- Les tests doivent couvrir l'absence de claims obligatoires et les JWT invalides.

## 5. Conséquences sur l'architecture

Le module `security-starter` dépend de Spring Security et des abstractions OAuth2 Resource Server.

Il ne dépend pas de `github-sdk`.

Le module `github-sdk` conserve la responsabilité de l'authentification spécifique à GitHub App, notamment la génération des JWT d'application et la gestion des tokens d'installation.

Les deux modules sont indépendants.

## 6. Historique

| Version | Date | Description |
|---|---|---|
| 1.0 | 2026-11-02 | Création et acceptation de la décision |