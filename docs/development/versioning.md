# Convention de versionnement

## 1. Objectifs

Le projet utilise Conventional Commits pour faciliter la lecture de l'historique Git et SemVer pour déterminer les versions des services.

Le versionnement des releases est indépendant par service. La première implémentation concerne `repository-service`.

## 2. Conventional Commits

Format :

`<type>(<scope>)?(!)?: <description>`

Types autorisés :

- `feat` : nouvelle fonctionnalité.
- `fix` : correction d'un défaut.
- `docs` : documentation.
- `style` : modifications de formatage sans changement fonctionnel.
- `refactor` : restructuration du code sans changement fonctionnel attendu.
- `perf` : amélioration des performances.
- `test` : ajout ou modification des tests.
- `build` : changements liés au système de build ou aux dépendances.
- `ci` : changements liés à l'intégration continue.
- `chore` : maintenance générale.
- `revert` : annulation d'un changement précédent.

Exemples :

- `feat(repository-service): ajoute l'analyse d'un dépôt`
- `fix(repository-service): corrige la pagination GitHub`
- `docs: documente le versionnement`
- `feat(repository-service)!: modifie le contrat de l'API`

Un changement incompatible peut également être signalé dans le corps du commit par `BREAKING CHANGE:`, conformément à Conventional Commits.

## 3. Règles SemVer

Une version suit le format `MAJOR.MINOR.PATCH`.

- **MAJOR** : changement incompatible avec les contrats précédents.
- **MINOR** : nouvelle fonctionnalité compatible.
- **PATCH** : correction compatible.

Correspondance indicative avec les commits :

| Changement | Incrément |
|---|---|
| `fix:` | PATCH |
| `feat:` | MINOR |
| `!` ou `BREAKING CHANGE:` | MAJOR |
| `docs:`, `test:`, `chore:` | Pas nécessairement de release |

Cette correspondance sert de règle de décision ; une analyse de l'ensemble des changements reste nécessaire avant une release.

## 4. Convention des tags Git

Chaque service utilise un préfixe de tag distinct :

- `repository-service-v1.0.0`
- `ai-orchestrator-v1.0.0`
- `content-service-v1.0.0`
- `publication-service-v1.0.0`
- `gateway-v1.0.0`

Une version donnée ne peut être publiée deux fois pour un même service.

## 5. Workflow de validation

Le workflow `.github/workflows/release-repository-service.yml` :

1. Est déclenché manuellement.
2. Vérifie que l'exécution part de `main`.
3. Valide le format de la version.
4. Vérifie que le tag proposé n'existe pas déjà.
5. Exécute le build Maven du service et de ses dépendances.
6. Conserve les rapports disponibles.

Ce workflow est actuellement un workflow de validation : il ne crée pas de tag et ne publie aucun artefact.

## 6. Limites actuelles

Le versionnement Git indépendant ne signifie pas que chaque service possède déjà une version Maven indépendante.

Les modules partagent actuellement une version Maven `1.0.0-SNAPSHOT`. La publication autonome des artefacts nécessitera une évolution ultérieure de la gestion des versions Maven et des dépendances intermodules.