# Audit SafeZone – Réponses aux Critères d'Audit

## Partie 1 – Fonctionnel

### 1.1 Interface Web SonarQube
**Critère :** L'étudiant peut-il accéder à l'interface web de SonarQube qui tourne localement ?

**Réponse :** ✅ Oui, l'instance locale est fournie par Docker et reste accessible sur `http://localhost:9000`.

**Justification détaillée :**
- `docker-compose.sonarqube.yml` déclare deux services (`sonarqube` et `sonarqube-postgres`) avec les ports exposés, les volumes persistants et des healthchecks HTTP/pgsql. Dès que `docker compose -f docker-compose.sonarqube.yml up -d` est exécuté à la racine du dépôt, le conteneur `ecommerce-sonarqube` écoute sur le port 9000 et le healthcheck `wget ... /api/system/status` valide que le web UI est prêt.
- Les identifiants par défaut et les variables d'environnement nécessaires sont documentés dans `.env.example` et `SONARQUBE_INSTALLATION.md`. Les credentials locales restent donc isolés dans un `.env` ignoré par Git (`.gitignore`), ce qui permet de se connecter depuis un navigateur tout en garantissant la reproductibilité.
- La procédure de vérification est systématique : `docker compose -f docker-compose.sonarqube.yml ps` doit afficher les deux services en `running (healthy)` et `curl http://localhost:9000/api/system/status` doit renvoyer `{"status":"UP"}` avant toute démo.

### 1.2 Intégration GitHub
**Critère :** Le projet est-il relié à GitHub et à SonarCloud pour analyser automatiquement chaque modification ?

**Réponse :** ✅ Oui, trois workflows GitHub Actions publient les résultats sur SonarCloud et décorent les Pull Requests.

**Justification détaillée :**
- Les workflows `.github/workflows/sonarqube-full.yml`, `sonarqube-backend.yml` et `sonarqube-frontend.yml` déclenchent `mvn sonar:sonar` ou l'action officielle `SonarSource/sonarcloud-github-action@v2.3.0` à chaque push/PR sur `main`, avec des filtres de chemins pour limiter les analyses aux parties modifiées.
- Chaque job fournit `SONAR_TOKEN` via `secrets.SONAR_TOKEN` et renseigne les `projectKey` des projets SonarCloud (`ecommerce-user-service`, `ecommerce-product-service`, `ecommerce-media-service`, `ecommerce-frontend`) déjà créés dans l'organisation `zone01-ecommerce` (cf. `README.md`, badges Quality Gate/Coverage pointant vers SonarCloud).
- Le job `summary` du workflow `sonarqube-full.yml` agrège les statuses renvoyés par `SonarSource/sonarqube-quality-gate-action` et poste automatiquement un commentaire riche (« 🔍 SonarCloud Full Analysis Summary ») sur la PR via `actions/github-script@v7`, ce qui constitue la preuve directe d'intégration GitHub ↔ SonarCloud.

### 1.3 Configuration Docker
**Critère :** L'environnement SonarQube est-il correctement containerisé et paramétré ?

**Réponse :** ✅ Oui, l'infrastructure Docker Compose isole la base PostgreSQL, persiste les données et applique les contraintes recommandées par Sonar.

**Justification détaillée :**
- `docker-compose.sonarqube.yml` définit les volumes (`sonarqube_postgres_data`, `sonarqube_data`, `sonarqube_extensions`, `sonarqube_logs`) et réutilise le réseau `safe-zone_ecommerce-network`. Les limites `ulimits` (`nofile` à 65536, `nproc` à 4096) et `mem_limit: 2g` respectent la checklist SonarQube.
- Les variables `SONAR_DB_*` et `SONAR_PORT` sont surchargeables via `.env`, ce qui permet d'aligner l'environnement local sur le cloud sans modifier le fichier Compose versionné.
- La gestion opérationnelle est documentée (voir `SONARQUBE_INSTALLATION.md`) : démarrage `docker compose -f docker-compose.sonarqube.yml up -d`, inspection des logs (`docker compose ... logs sonarqube | grep "SonarQube is operational"`), arrêt propre (`docker compose ... down`) et réinitialisation contrôlée (`down -v` uniquement pour repartir de zéro).

### 1.4 Automatisation CI/CD
**Critère :** L'analyse de qualité est-elle exécutée automatiquement lors des builds CI/CD ?

**Réponse :** ✅ Oui, toute branche `main` est couverte par des workflows multi-jobs avec caches, artefacts et Quality Gates bloquants.

**Justification détaillée :**
- `sonarqube-full.yml` exécute une matrice Maven pour les trois microservices (checkout `fetch-depth: 0`, `setup-java@v4`, caches Maven/Sonar, `mvn clean verify`, `mvn sonar:sonar`, contrôle du Quality Gate + upload JaCoCo). En parallèle, `frontend-analysis` installe Node 20, lance `npm ci`, `npm run test -- --code-coverage`, `npm run build`, puis pousse les métriques TypeScript vers SonarCloud.
- Les workflows spécialisés `sonarqube-backend.yml` et `sonarqube-frontend.yml` utilisent des filtres `paths:` pour déclencher uniquement l’analyse nécessaire lorsqu’une PR n’impacte qu’un périmètre donné, ce qui réduit le temps de feedback.
- Les actions sont toutes épinglées sur des SHAs (`actions/checkout@34e1148…`, `actions/cache@0057852…`, `SonarSource/sonarqube-quality-gate-action@cf038b0…`) pour respecter les bonnes pratiques supply-chain et garantir la reproductibilité.
- Les rapports de couverture (`target/site/jacoco/jacoco.xml`, `frontend/coverage/frontend/lcov.info`) sont archivés via `actions/upload-artifact@v4`, ce qui permet aux auditeurs de retélécharger les preuves sans relancer les builds.

### 1.5 Processus de Code Review
**Critère :** SonarQube intervient-il réellement dans le processus de revue de code ?

**Réponse :** ✅ Oui, les Pull Requests sont bloquées tant que les Quality Gates n'ont pas le statut ✅ et le bot CI poste un rapport détaillé.

**Justification détaillée :**
- Les règles de protection décrites dans `docs/05-BRANCH-PROTECTION.md` exigent : (1) au moins une review humaine, (2) la réussite des checks `Backend - user-service`, `Backend - product-service`, `Backend - media-service`, `Frontend (Angular)` et `Analysis Summary`, (3) la résolution de toutes les conversations avant merge. GitHub empêche donc mécaniquement le merge si SonarCloud remonte un statut `FAILED`.
- Au niveau de la PR, l’onglet “Checks” montre en temps réel les statuts renvoyés par SonarCloud (`gh pr checks <numéro>` permet de lister ces checks depuis le terminal). Le commentaire généré par `actions/github-script` rappelle quels services sont bloquants et fournit les liens directs vers SonarCloud pour corriger les issues avant de relancer l’analyse.
- Le workflow `sonarqube-full.yml` se termine par `exit 1` si `overall-status=FAILED`. Cela garantit qu'aucun reviewer ne pourra valider sans s’être assuré que les problèmes de sécurité, de couverture ou de maintenabilité ont été adressés.

## Partie 2 – Compréhension

### 2.1 Explication du Setup
**Critère :** L’étudiant peut-il expliquer comment l’intégration SonarQube/SonarCloud est architecturée ?

**Réponse :** ✅ Oui, la topologie complète (microservices Java, frontend Angular, double analyse local/cloud) est documentée et justifiée.

**Justification détaillée :**
- `README.md` décrit l’architecture microservices (3 services Spring Boot + frontend Angular 20) et expose déjà les badges SonarCloud, ce qui prouve que chaque composant possède son `projectKey` dédié.
- La stratégie “Local SonarQube pour le dev” + “SonarCloud pour la CI/CD” est explicitée dans `docs/06-AUDIT-PREPARATION.md` : on développe et on vérifie rapidement via Docker (`docker-compose.sonarqube.yml`), puis on s'appuie sur SonarCloud pour analyser les PR depuis les runners GitHub (impossible avec un Sonar local).
- Les propriétés Sonar nécessaires sont embarquées dans le code : `backend/*/pom.xml` déclare `sonar.organization`, `sonar.projectKey`, `sonar.exclusions` et `sonar.coverage.jacoco.xmlReportPaths`, tandis que `frontend/sonar-project.properties` fait de même pour l’application Angular. Aucune configuration “cachée” n’est requise.

### 2.2 Processus d’Intégration
**Critère :** L’étudiant connaît-il les étapes nécessaires pour intégrer (ou réintégrer) un service dans SonarCloud ?

**Réponse :** ✅ Oui, le processus est standardisé et outillé.

**Justification détaillée :**
- Les secrets nécessaires (`SONAR_TOKEN`, `SLACK_WEBHOOK_URL` optionnel) et la marche à suivre sont décrits pas à pas dans `docs/03-GITHUB-SECRETS-SETUP.md`. L’accès se fait via Settings → Secrets and variables → Actions.
- Chaque service dispose déjà de son `projectKey` Sonar dans son `pom.xml`. Pour un nouveau service, il suffit de reprendre ce modèle, de créer le projet côté SonarCloud puis d’ajouter l’entrée correspondante dans la matrice `matrix.service` du workflow `sonarqube-full.yml` (ainsi que dans `sonarqube-backend.yml` si c’est un backend).
- Le script `scripts/configure-sonarcloud-quality-gates.sh` permet de re-créer le Quality Gate “Zone01 School Project” et de l’appliquer aux projets listés (`PROJECTS=( ... )`). Il suffit d’exécuter `SONAR_TOKEN=... ./scripts/configure-sonarcloud-quality-gates.sh` pour propager les règles.
- Une fois la configuration terminée, on déclenche un run de validation avec `gh workflow run sonarqube-full.yml -r <branch>` (ou on ouvre une PR de test) pour vérifier que SonarCloud reçoit bien l’analyse.

### 2.3 Fonctionnement Détaillé
**Critère :** L’étudiant sait-il décrire le cycle de vie complet d’une analyse ?

**Réponse :** ✅ Oui, chaque étape – du commit au statut GitHub – est tracée et vérifiable.

**Justification détaillée :**
- Lorsqu’un développeur pousse du code, GitHub Actions déclenche automatiquement le workflow concerné (`on.push` ou `on.pull_request`).
- Étapes backend : `mvn clean verify` produit les tests JaCoCo (`target/site/jacoco/jacoco.xml`), puis `mvn sonar:sonar` publie le rapport sur SonarCloud. L’action `SonarSource/sonarqube-quality-gate-action` lit `target/sonar/report-task.txt` pour connaître l’ID d’analyse et attend le verdict du Quality Gate.
- Étapes frontend : `npm run test -- --code-coverage` génère `frontend/coverage/frontend/lcov.info`, ensuite l’action SonarCloud officielle envoie les métriques et le Quality Gate est contrôlé de la même manière.
- Enfin, le job `summary` consolide les sorties (`needs.backend-analysis.outputs.user-service-status`, etc.), écrit `overall-status` et publie un commentaire. Si un service échoue, `overall-status=FAILED` → échec du workflow → checks GitHub rouges → merge impossible tant que la PR n’est pas corrigée et les tests relancés.

## Partie 3 – Sécurité

### 3.1 Gestion des Permissions
**Critère :** Les accès (GitHub, SonarCloud, secrets) sont-ils maîtrisés ?

**Réponse :** ✅ Oui, le principe du moindre privilège est appliqué.

**Justification détaillée :**
- Côté GitHub, les workflows déclarent explicitement `permissions: contents: read, pull-requests: write, checks: write`, ce qui limite l’accès aux seules opérations nécessaires (consultation du code + publication des statuts/commentaires).
- Les règles décrites dans `docs/05-BRANCH-PROTECTION.md` interdisent les push directs sur `main`, exigent la mise à jour avec la branche cible et bloquent les merges tant que les checks Sonar n’ont pas abouti. Les reviewers sont donc propriétaires du process d’approbation.
- `docs/03-GITHUB-SECRETS-SETUP.md` rappelle que seul le secret `SONAR_TOKEN` est exposé aux workflows. Sa rotation se fait depuis SonarCloud → My Account → Security, et GitHub chiffre automatiquement la valeur. Aucun token n’est commité (voir `.env.example`, `.gitignore`).

### 3.2 Règles de Sécurité
**Critère :** Les règles de sécurité Sonar et applicatives sont-elles définies et suivies ?

**Réponse :** ✅ Oui, les Quality Gates imposent un Security Rating = A et les services implémentent les contre-mesures attendues.

**Justification détaillée :**
- `scripts/configure-sonarcloud-quality-gates.sh` ajoute les conditions `new_security_rating = A`, `new_reliability_rating = A` et `new_security_hotspots_reviewed = 100%` au Quality Gate “Zone01 School Project”. Toute vulnérabilité ou hotspot non traité provoque un `FAILED`.
- Les services backend appliquent ces recommandations : `backend/user-service/security/JwtAuthenticationFilter.java` vérifie systématiquement la présence du header Bearer, valide le token via `JwtUtil` et ne laisse jamais l’`Authentication` peuplée avec un token invalide ; `UserService.java` utilise `PasswordEncoder` (BCrypt) et refuse de stocker un mot de passe en clair ; `backend/media-service/src/main/java/.../MediaService.java` restreint les types MIME, limite la taille des fichiers et supprime physiquement les médias lors d’une suppression, ce qui répond aux hotspots “File Upload should be restricted”.
- Lorsqu’un hotspot apparaît dans SonarCloud, la procédure consiste à aller sur `Project → Security Hotspots`, à analyser le snippet incriminé, puis à cliquer sur `Resolve as Fixed/Safe`. Le Quality Gate exige 100 % de hotspots revus, ce qui est vérifié automatiquement par l’action `SonarSource/sonarqube-quality-gate-action`.

### 3.3 Protection des Secrets
**Critère :** Les secrets (tokens Sonar, credentials DB, JWT) sont-ils protégés ?

**Réponse :** ✅ Oui, ils sont isolés par environnement et jamais exposés dans le dépôt.

**Justification détaillée :**
- `.env.example` contient les placeholders des secrets applicatifs (`SONAR_DB_PASSWORD`, `JWT_SECRET`, `MONGODB_ROOT_PASSWORD`, etc.) et rappelle de copier ce fichier en `.env`. `.env` figure dans `.gitignore`, évitant toute fuite.
- En CI, seul `secrets.SONAR_TOKEN` est injecté et utilisé via des variables d’environnement (`env: SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}`) sans jamais être loggé. L’action Sonar masque automatiquement sa valeur.
- Pour les notifications optionnelles (Slack), le secret `SLACK_WEBHOOK_URL` peut être ajouté et consommé par l’étape décrite dans `docs/03-GITHUB-SECRETS-SETUP.md`. Aucun webhook ni credential n’apparaît dans l’historique Git (cf. commit `fix: replace Slack webhook examples with generic format`).

## Partie 4 – Qualité

### 4.1 Quality Gates
**Critère :** Les Quality Gates sont-ils définis et appliqués automatiquement ?

**Réponse :** ✅ Oui, un Quality Gate personnalisé est appliqué à chaque projet SonarCloud et vérifié dans les workflows.

**Justification détaillée :**
- Le script `scripts/configure-sonarcloud-quality-gates.sh` crée le gate “Zone01 School Project” avec les conditions : `new_coverage ≥ 80%`, `new_duplicated_lines_density ≤ 3%`, `new_maintainability_rating = A`, `new_reliability_rating = A`, `new_security_rating = A`, `new_security_hotspots_reviewed = 100%`, puis l’assigne aux projets `ecommerce-*` listés dans `PROJECTS`.
- Chaque workflow appelle `SonarSource/sonarqube-quality-gate-action`, ce qui force l’attente du verdict côté SonarCloud avant de marquer le job comme réussi. Si une condition échoue, l’action renvoie `quality-gate-status=FAILED` et le job `summary` échoue.
- L’efficacité du gate est visible publiquement : les badges Quality Gate dans `README.md` pointent vers `https://sonarcloud.io/project/overview?id=ecommerce-user-service` (et consorts) et reflètent le statut temps réel.

### 4.2 Couverture de Code
**Critère :** La couverture est-elle mesurée et suivie (backend + frontend) ?

**Réponse :** ✅ Oui, JaCoCo, Jest/Karma et les Quality Gates garantissent ≥ 80 % de couverture sur le nouveau code.

**Justification détaillée :**
- Les `pom.xml` des trois microservices embarquent `jacoco-maven-plugin` (goal `prepare-agent` + `report`) et déclarent `sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml`. Un simple `mvn -f backend/user-service/pom.xml clean verify` produit le rapport consommé par Sonar.
- Le frontend possède `frontend/sonar-project.properties` avec `sonar.javascript.lcov.reportPaths=coverage/frontend/lcov.info`. Le workflow lance `npm run test -- --code-coverage`, ce qui génère ce fichier avant l’étape `SonarSource/sonarcloud-github-action`.
- Les artefacts `coverage-backend-<service>` et `coverage-frontend` sont uploadés (`actions/upload-artifact@v4`) pour conserver une trace. En cas d’audit, on peut les télécharger depuis la page du workflow.
- Les conditions du Quality Gate (`new_coverage ≥ 80%`) transforment la couverture en exigence bloquante : si une PR introduit du code non couvert, l’analyse passe en `FAILED` et le merge est bloqué jusqu’à ce que des tests supplémentaires soient ajoutés.

### 4.3 Améliorations Apportées
**Critère :** SonarQube a-t-il conduit à des améliorations concrètes dans le code ?

**Réponse :** ✅ Oui, plusieurs correctifs alignés sur les recommandations Sonar sont visibles dans le dépôt.

**Justification détaillée :**
- **Sécurisation de l’authentification :** `backend/user-service/src/main/java/com/ecommerce/user/service/UserService.java` ne retourne jamais le mot de passe et encode systématiquement les credentials via `PasswordEncoder`. L’analyse Sonar impose ce comportement pour obtenir un Security Rating A et éviter l’odeur “Sensitive data should be encrypted”.
- **Validation des uploads :** `backend/media-service/src/main/java/com/ecommerce/media/service/MediaService.java` vérifie les types MIME autorisés, limite la taille à 2 MB et nettoie le stockage (suppression physique + suppression MongoDB + suppression des dossiers vides). Ces garde-fous répondent aux hotspots Sonar sur l’upload de fichiers et ont été ajoutés pour satisfaire les règles “Files should be validated before being used”.
- **Tests ciblés pour effacer les code smells :** les classes de tests `backend/user-service/src/test/java/com/ecommerce/user/security/JwtAuthenticationFilterTest.java` et `.../service/UserServiceTest.java` couvrent les branches critiques (header absent, token valide, etc.). Elles ont été écrites pour faire passer la condition `Coverage on New Code` et supprimer les warnings “Add tests to this class”.
- **Kafka events centralisés :** `backend/product-service/src/main/java/com/ecommerce/product/service/ProductService.java` encapsule l’émission d’événements dans `sendProductEvent`, éliminant la duplication détectée par Sonar et améliorant la maintenabilité.

## Partie 5 – Bonus

### 5.1 Notifications et Intégrations
**Critère :** Des notifications ou intégrations additionnelles prolongent-elles l’analyse Sonar ?

**Réponse :** ✅ Oui, les développeurs reçoivent les résultats via plusieurs canaux et peuvent brancher un webhook Slack documenté.

**Justification détaillée :**
- Sur chaque PR, le commentaire généré par `actions/github-script` joue le rôle de notification “push” avec le statut global, le détail par service et les liens directs vers SonarCloud.
- Les badges dans `README.md` exposent en continu le Quality Gate, la couverture, les bugs et les vulnérabilités. Ils peuvent être intégrés dans des slides ou des dashboards externes.
- SonarCloud est relié nativement au repository GitHub (onglet “Links” du projet), ce qui ajoute l’onglet “Analysis” dans les PR GitHub.
- Pour une notification Slack/Teams, `docs/03-GITHUB-SECRETS-SETUP.md` explique comment créer le secret `SLACK_WEBHOOK_URL` et ajoute l’étape type :
  ```yaml
  - name: Notify Slack
    uses: slackapi/slack-github-action@v1
    with:
      payload: '{"text": "Sonar analysis failed on ${{ github.ref }}"}'
      webhook: ${{ secrets.SLACK_WEBHOOK_URL }}
  ```
  L’intégration est donc prête à être activée dans n’importe quel workflow CI.

### 5.2 Intégration IDE
**Critère :** L’équipe exploite-t-elle Sonar directement dans les IDE ?

**Réponse :** ✅ Oui, l’usage de SonarLint est standardisé pour VS Code et IntelliJ afin d’obtenir le feedback avant commit.

**Justification détaillée :**
- Les développeurs installent l’extension SonarLint (VS Code Marketplace ou IntelliJ Plugin) puis se connectent à l’organisation SonarCloud `zone01-ecommerce` en utilisant un token utilisateur (généré depuis SonarCloud → My Account → Security). Les mêmes `projectKey` que ceux présents dans `backend/*/pom.xml` et `frontend/sonar-project.properties` sont sélectionnés lors du binding.
- La configuration recommandée (décrite dans `docs/06-AUDIT-PREPARATION.md`, section “Points techniques à maîtriser”) consiste à activer le “Connected Mode” : SonarLint synchronise automatiquement les règles et Quality Profiles depuis SonarCloud, ce qui garantit que les alertes locales correspondent à celles de la CI.
- Les IDE sont configurés pour lancer SonarLint automatiquement à la sauvegarde : VS Code → Settings → “SonarLint › Ls: Java: Enabled”, IntelliJ → Settings → Tools → SonarLint → `Automatically trigger analysis`. Les issues apparaissent alors directement dans l’éditeur, réduisant le nombre de `Quality Gate FAILED` (statistique suivie dans `docs/07-DEMO-GUIDE.md`).
