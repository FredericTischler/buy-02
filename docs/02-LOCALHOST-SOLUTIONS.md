# Solutions au Problème SonarQube Localhost

## Table des Matières
1. [Contexte du Problème](#contexte-du-problème)
2. [Solution 1 : SonarCloud (Recommandée)](#solution-1--sonarcloud-recommandée)
3. [Solution 2 : Self-Hosted GitHub Runner](#solution-2--self-hosted-github-runner)
4. [Solution 3 : Serveur Cloud SonarQube](#solution-3--serveur-cloud-sonarqube)
5. [Solution 4 : Tunnel Ngrok/Cloudflare](#solution-4--tunnel-ngrokcloudflare)
6. [Tableau Comparatif](#tableau-comparatif)
7. [Recommandation Finale](#recommandation-finale)

---

## 1. Contexte du Problème

### Situation Actuelle

Vous avez configuré SonarQube localement sur `http://localhost:9000` pour analyser la qualité de votre code. Cela fonctionne parfaitement en développement local, mais pose un **problème majeur** pour le CI/CD avec GitHub Actions.

### Le Problème

```yaml
# Dans votre workflow GitHub Actions
- name: SonarQube Scan
  run: mvn sonar:sonar -Dsonar.host.url=http://localhost:9000

# ❌ ERREUR
[ERROR] Failed to connect to http://localhost:9000
[ERROR] Connection refused (Connection refused)
```

### Pourquoi ça ne Fonctionne Pas ?

**Architecture du problème :**

```
┌─────────────────────────────────────────────────────────────┐
│  Votre Machine Locale                                       │
│  ┌──────────────────────────────────────────┐              │
│  │  SonarQube Server                        │              │
│  │  http://localhost:9000                   │              │
│  │  (accessible uniquement localement)      │              │
│  └──────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────┘
                          ❌ Pas accessible
┌─────────────────────────────────────────────────────────────┐
│  GitHub Actions Runner (ubuntu-latest)                      │
│  ┌──────────────────────────────────────────┐              │
│  │  Workflow CI/CD                          │              │
│  │  Essaie de se connecter à localhost:9000 │              │
│  │  → Mais localhost ici = le runner lui-même│             │
│  │  → Pas votre machine !                   │              │
│  └──────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────┘
```

**Explications :**
- `localhost` signifie "cette machine"
- Sur votre PC : `localhost:9000` = votre SonarQube local
- Sur le runner GitHub : `localhost:9000` = rien (pas de SonarQube)
- Les runners GitHub sont dans le cloud, isolés de votre réseau local

### Conséquences

Sans solution, vous ne pouvez pas :
- Analyser automatiquement le code lors des pull requests
- Obtenir des rapports de qualité dans GitHub
- Bloquer les merges avec des Quality Gates
- Suivre l'évolution de la qualité du code dans le temps

**Vous devez choisir l'une des 4 solutions ci-dessous.**

---

## 2. Solution 1 : SonarCloud (Recommandée)

### Description Technique

**SonarCloud** est la version cloud hébergée de SonarQube, gérée par Sonar (la société derrière SonarQube). C'est un service SaaS (Software as a Service) qui offre toutes les fonctionnalités de SonarQube sans avoir à gérer un serveur.

**Architecture :**
```
GitHub Repository
       ↓ (push/PR)
GitHub Actions Runner
       ↓ (mvn sonar:sonar)
SonarCloud.io (https://sonarcloud.io)
       ↓ (résultats)
Tableau de bord web + PR checks
```

### Avantages

#### 1. Gratuit pour Projets Open Source
- **100% gratuit** pour les repositories publics GitHub
- Pas de limite de nombre de projets
- Pas de limite de lignes de code analysées
- Toutes les fonctionnalités premium incluses

#### 2. Setup Ultra-Rapide
- **Authentification OAuth** : Connexion avec votre compte GitHub en 1 clic
- **Import automatique** : Détecte automatiquement vos repos GitHub
- **Configuration minimale** : 3-4 propriétés dans pom.xml
- **Pas de serveur à gérer** : Zéro infrastructure

#### 3. Intégration GitHub Native
- **GitHub Checks** : Résultats directement dans les Pull Requests
- **Status checks** : Bloque le merge si Quality Gate échoue
- **Commentaires automatiques** : Annotations sur le code problématique
- **Badge README** : Badge de qualité pour votre README.md

#### 4. Performance et Fiabilité
- **Uptime 99.9%** : Infrastructure enterprise-grade
- **Scalabilité** : Analyse en parallèle de tous vos projets
- **Rapidité** : Scan optimisé (plus rapide que self-hosted)
- **CDN global** : Dashboard rapide partout dans le monde

#### 5. Fonctionnalités Avancées
- **Historique illimité** : Conservation de toutes les analyses
- **Détection de duplications cross-projects** : Détecte le code dupliqué entre repos
- **Analyse de branches** : Supporte feature branches et PR branches
- **Notifications** : Email, webhook, Slack intégrations
- **API complète** : Automatisation possible

#### 6. Maintenance Zéro
- **Mises à jour automatiques** : Toujours la dernière version
- **Backups automatiques** : Données sauvegardées
- **Sécurité** : Patching automatique des vulnérabilités
- **Support** : Documentation complète + communauté active

#### 7. Collaboration
- **Accès multi-utilisateurs** : Toute l'équipe peut consulter
- **Permissions granulaires** : Admin, maintainer, viewer roles
- **Commentaires** : Discussion sur les issues détectées
- **Dashboard unifié** : Vue d'ensemble de tous vos projets

### Inconvénients

#### 1. Données Hébergées Externellement
- **Préoccupation** : Code analysé envoyé à SonarCloud
- **Mitigation** :
  - Seul le code source est analysé (pas les secrets)
  - Sonar est conforme GDPR, SOC 2, ISO 27001
  - Code supprimé après analyse
  - Pour les projets open source, c'est déjà public sur GitHub

#### 2. Limité aux Projets Publics (Gratuit)
- **Contrainte** : Gratuit seulement si votre repo GitHub est public
- **Repos privés** : Payant (10€/mois pour petite équipe)
- **Projet Zone01** : Pas un problème si votre repo est public

#### 3. Connexion Internet Requise
- **Contrainte** : Impossible d'analyser offline
- **Impact** : Négligeable (GitHub Actions est déjà en ligne)

#### 4. Moins de Contrôle sur les Plugins
- **Limitation** : Impossible d'installer des plugins custom
- **Impact** : Plugins officiels largement suffisants (Java, TypeScript, etc.)

### Coût

- **Projets publics** : **GRATUIT** (0€/mois)
- **Projets privés** :
  - Jusqu'à 100k LOC : 10€/mois
  - Jusqu'à 250k LOC : 75€/mois
  - Enterprise : Sur devis

**Pour votre projet Zone01 (repo public) : 0€**

### Temps de Setup

**Estimation totale : 10-15 minutes**

| Étape | Durée |
|-------|-------|
| Créer compte SonarCloud | 2 min |
| Connecter GitHub | 1 min |
| Importer organisation/projet | 2 min |
| Configurer pom.xml (3 services) | 5 min |
| Créer GitHub Secret SONAR_TOKEN | 2 min |
| Tester premier scan | 3 min |

### Complexité

**Niveau : FACILE**

- Pas de serveur à installer
- Pas de réseau à configurer
- Pas de base de données à gérer
- Interface web intuitive
- Documentation excellente

**Compétences requises :**
- Savoir se connecter à GitHub (OAuth)
- Éditer un fichier XML (pom.xml)
- Créer un secret GitHub
- Lire une documentation

### Recommandation pour Projet École

**FORTEMENT RECOMMANDÉ** pour les raisons suivantes :

#### Alignement avec le Contexte Éducatif
1. **Temps limité** : Setup en 15 minutes vs 2-3 heures pour self-hosted
2. **Focus sur l'apprentissage** : Temps dédié au code, pas à l'infra
3. **Gratuit** : Budget étudiant = 0€
4. **Expérience professionnelle** : SonarCloud largement utilisé en entreprise

#### Avantages Pédagogiques
- **Visualisation** : Dashboard interactif pour comprendre les metrics
- **Best practices** : Apprendre les standards de qualité du code
- **Feedback immédiat** : Résultats dans les PR
- **Portfolio** : Badge de qualité pour impressionner les recruteurs

### Étapes d'Implémentation

#### Étape 1 : Créer un Compte SonarCloud (2 minutes)

1. **Aller sur SonarCloud**
   - URL : https://sonarcloud.io
   - Cliquer sur "Start now" ou "Log in"

2. **S'authentifier avec GitHub**
   ```
   Cliquer sur "Log in with GitHub"
   → Autoriser SonarCloud à accéder à votre compte GitHub
   → Accepter les permissions (lecture des repos, etc.)
   ```

3. **Vérifier la connexion**
   - Vous devriez voir votre nom d'utilisateur GitHub
   - Dashboard SonarCloud s'affiche

#### Étape 2 : Importer votre Organisation GitHub (2 minutes)

1. **Créer une organisation SonarCloud**
   ```
   Dashboard → "+" → "Analyze new project"
   → "Import an organization from GitHub"
   → Sélectionner votre organisation GitHub (ex: Zone01Normandie)
   → "Install SonarCloud GitHub App"
   ```

2. **Autoriser l'accès au repository**
   ```
   Sur GitHub (page d'autorisation) :
   → Sélectionner "Only select repositories"
   → Choisir "safe-zone"
   → Cliquer "Install"
   ```

3. **Configurer l'organisation dans SonarCloud**
   ```
   Nom : zone01-normandie (ou votre choix)
   Key : zone01-normandie (utilisé dans les configs)
   → "Continue"
   ```

#### Étape 3 : Importer les Projets (3 minutes)

1. **Sélectionner les projets à analyser**
   ```
   Liste des repos détectés :
   ☑ safe-zone
   → "Set Up"
   ```

2. **Choisir la méthode d'analyse**
   ```
   → Sélectionner "With GitHub Actions"
   (SonarCloud génère les instructions)
   ```

3. **Créer les Project Keys**

   Vous devez créer **4 projets** dans SonarCloud (un par service) :

   **Projet 1 : User Service**
   ```
   Project Key: zone01-normandie_safe-zone_user-service
   Display Name: E-Commerce User Service
   ```

   **Projet 2 : Product Service**
   ```
   Project Key: zone01-normandie_safe-zone_product-service
   Display Name: E-Commerce Product Service
   ```

   **Projet 3 : Media Service**
   ```
   Project Key: zone01-normandie_safe-zone_media-service
   Display Name: E-Commerce Media Service
   ```

   **Projet 4 : Frontend**
   ```
   Project Key: zone01-normandie_safe-zone_frontend
   Display Name: E-Commerce Frontend
   ```

#### Étape 4 : Générer le Token (2 minutes)

1. **Créer un token d'authentification**
   ```
   SonarCloud Dashboard
   → Cliquer sur votre avatar (haut droite)
   → "My Account"
   → "Security" tab
   → "Generate Tokens"
   ```

2. **Configuration du token**
   ```
   Name: GitHub Actions Safe-Zone
   Type: Project Analysis Token
   Scope: Analyse des projets
   Expiration: No expiration (ou 90 days pour plus de sécurité)
   → "Generate"
   ```

3. **Copier le token**
   ```
   Token généré: squ_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6

   ⚠️ IMPORTANT : Copiez-le immédiatement !
   Il ne sera plus affiché après avoir quitté la page.
   ```

#### Étape 5 : Configurer GitHub Secrets (2 minutes)

1. **Aller dans les settings du repository GitHub**
   ```
   GitHub → safe-zone repository
   → "Settings" tab
   → "Secrets and variables" → "Actions"
   → "New repository secret"
   ```

2. **Créer le secret SONAR_TOKEN**
   ```
   Name: SONAR_TOKEN
   Value: squ_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
   (coller le token copié à l'étape 4)
   → "Add secret"
   ```

3. **Vérification**
   ```
   Le secret SONAR_TOKEN doit apparaître dans la liste
   (la valeur est masquée par GitHub)
   ```

#### Étape 6 : Modifier les pom.xml (5 minutes)

**Fichier : backend/user-service/pom.xml**

Remplacer la section `<properties>` SonarQube par :

```xml
<properties>
    <java.version>17</java.version>
    <jjwt.version>0.11.5</jjwt.version>

    <!-- SonarCloud Configuration -->
    <sonar.organization>zone01-normandie</sonar.organization>
    <sonar.host.url>https://sonarcloud.io</sonar.host.url>
    <sonar.projectKey>zone01-normandie_safe-zone_user-service</sonar.projectKey>
    <sonar.projectName>E-Commerce User Service</sonar.projectName>

    <!-- Coverage -->
    <sonar.java.binaries>target/classes</sonar.java.binaries>
    <sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>

    <!-- Exclusions -->
    <sonar.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.exclusions>
    <sonar.coverage.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.coverage.exclusions>
</properties>
```

**Fichier : backend/product-service/pom.xml**

```xml
<properties>
    <java.version>17</java.version>

    <!-- SonarCloud Configuration -->
    <sonar.organization>zone01-normandie</sonar.organization>
    <sonar.host.url>https://sonarcloud.io</sonar.host.url>
    <sonar.projectKey>zone01-normandie_safe-zone_product-service</sonar.projectKey>
    <sonar.projectName>E-Commerce Product Service</sonar.projectName>

    <!-- Coverage -->
    <sonar.java.binaries>target/classes</sonar.java.binaries>
    <sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>

    <!-- Exclusions -->
    <sonar.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.exclusions>
    <sonar.coverage.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.coverage.exclusions>
</properties>
```

**Fichier : backend/media-service/pom.xml**

```xml
<properties>
    <java.version>17</java.version>

    <!-- SonarCloud Configuration -->
    <sonar.organization>zone01-normandie</sonar.organization>
    <sonar.host.url>https://sonarcloud.io</sonar.host.url>
    <sonar.projectKey>zone01-normandie_safe-zone_media-service</sonar.projectKey>
    <sonar.projectName>E-Commerce Media Service</sonar.projectName>

    <!-- Coverage -->
    <sonar.java.binaries>target/classes</sonar.java.binaries>
    <sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>

    <!-- Exclusions -->
    <sonar.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.exclusions>
    <sonar.coverage.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.coverage.exclusions>
</properties>
```

**Note importante :**
- Remplacez `zone01-normandie` par votre organisation SonarCloud
- Les `projectKey` doivent correspondre à ceux créés à l'étape 3

#### Étape 7 : Créer le Workflow GitHub Actions (8 minutes)

**Fichier : .github/workflows/ci-cd.yml**

```yaml
name: CI/CD Pipeline with SonarCloud

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  # =============================================
  # BACKEND : User Service
  # =============================================
  build-user-service:
    name: Build & Analyze User Service
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for SonarCloud

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2/repository
          key: maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: maven-

      - name: Build and Test
        working-directory: backend/user-service
        run: mvn clean verify

      - name: SonarCloud Scan
        working-directory: backend/user-service
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar

  # =============================================
  # BACKEND : Product Service
  # =============================================
  build-product-service:
    name: Build & Analyze Product Service
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2/repository
          key: maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: maven-

      - name: Build and Test
        working-directory: backend/product-service
        run: mvn clean verify

      - name: SonarCloud Scan
        working-directory: backend/product-service
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar

  # =============================================
  # BACKEND : Media Service
  # =============================================
  build-media-service:
    name: Build & Analyze Media Service
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2/repository
          key: maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: maven-

      - name: Build and Test
        working-directory: backend/media-service
        run: mvn clean verify

      - name: SonarCloud Scan
        working-directory: backend/media-service
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar

  # =============================================
  # FRONTEND : Angular
  # =============================================
  build-frontend:
    name: Build & Analyze Frontend
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup Node.js 18
        uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Run tests with coverage
        working-directory: frontend
        run: npm run test -- --watch=false --code-coverage

      - name: Build
        working-directory: frontend
        run: npm run build

      - name: SonarCloud Scan
        uses: SonarSource/sonarcloud-github-action@v2.3.0
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        with:
          projectBaseDir: frontend
```

**Pour le frontend, créer aussi : frontend/sonar-project.properties**

```properties
sonar.organization=zone01-normandie
sonar.projectKey=zone01-normandie_safe-zone_frontend
sonar.projectName=E-Commerce Frontend

# Source files
sonar.sources=src
sonar.exclusions=**/node_modules/**,**/*.spec.ts,**/*.test.ts

# Tests
sonar.tests=src
sonar.test.inclusions=**/*.spec.ts

# Coverage
sonar.javascript.lcov.reportPaths=coverage/lcov.info
```

#### Étape 8 : Tester le Pipeline (3 minutes)

1. **Commit et push des changements**
   ```bash
   git add .
   git commit -m "ci: configure SonarCloud integration"
   git push origin main
   ```

2. **Vérifier l'exécution sur GitHub**
   ```
   GitHub → Repository "safe-zone"
   → Onglet "Actions"
   → Voir le workflow "CI/CD Pipeline with SonarCloud" en cours
   ```

3. **Attendre les résultats**
   ```
   Durée estimée : 3-5 minutes

   Vous devriez voir :
   ✅ build-user-service : Success
   ✅ build-product-service : Success
   ✅ build-media-service : Success
   ✅ build-frontend : Success
   ```

4. **Consulter les résultats sur SonarCloud**
   ```
   Aller sur https://sonarcloud.io
   → Dashboard
   → Voir les 4 projets avec leurs métriques
   ```

#### Étape 9 : Configurer le Quality Gate (optionnel)

Voir le document détaillé : **`docs/04-QUALITY-GATES-SETUP.md`**

```
SonarCloud → Votre projet
→ "Quality Gates" tab
→ Configurer les seuils (coverage > 70%, 0 bugs, etc.)
```

#### Étape 10 : Ajouter des Badges au README (bonus)

**Dans README.md, ajouter :**

```markdown
# E-Commerce Platform

![Build Status](https://github.com/votre-username/safe-zone/actions/workflows/ci-cd.yml/badge.svg)

[![Quality Gate User Service](https://sonarcloud.io/api/project_badges/measure?project=zone01-normandie_safe-zone_user-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=zone01-normandie_safe-zone_user-service)
[![Coverage User Service](https://sonarcloud.io/api/project_badges/measure?project=zone01-normandie_safe-zone_user-service&metric=coverage)](https://sonarcloud.io/dashboard?id=zone01-normandie_safe-zone_user-service)

[![Quality Gate Product Service](https://sonarcloud.io/api/project_badges/measure?project=zone01-normandie_safe-zone_product-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=zone01-normandie_safe-zone_product-service)

[![Quality Gate Media Service](https://sonarcloud.io/api/project_badges/measure?project=zone01-normandie_safe-zone_media-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=zone01-normandie_safe-zone_media-service)

[![Quality Gate Frontend](https://sonarcloud.io/api/project_badges/measure?project=zone01-normandie_safe-zone_frontend&metric=alert_status)](https://sonarcloud.io/dashboard?id=zone01-normandie_safe-zone_frontend)
```

### Résultat Final

Après ces 10 étapes, vous aurez :
- ✅ Pipeline CI/CD fonctionnel sur GitHub Actions
- ✅ Analyse automatique du code sur chaque push/PR
- ✅ Dashboard SonarCloud avec métriques de qualité
- ✅ Quality Gate bloquant les merges (si configuré)
- ✅ Badges de qualité dans votre README
- ✅ Historique de qualité du code dans le temps

**Temps total : 15 minutes**
**Coût : 0€**
**Maintenance : 0 minutes/mois**

---

## 3. Solution 2 : Self-Hosted GitHub Runner

### Description Technique

Un **self-hosted runner** est un serveur que vous configurez pour exécuter les workflows GitHub Actions, au lieu d'utiliser les runners hébergés par GitHub. Dans ce cas, vous installez le runner sur votre propre machine (celle qui a SonarQube local), ce qui lui permet d'accéder à `localhost:9000`.

**Architecture :**
```
┌───────────────────────────────────────────────────────────────┐
│  Votre Machine Locale                                         │
│  ┌──────────────────────┐       ┌───────────────────────┐    │
│  │ SonarQube Server     │←──────│ GitHub Actions Runner │    │
│  │ localhost:9000       │       │ (self-hosted)         │    │
│  └──────────────────────┘       └───────────────────────┘    │
│                                           ↑                    │
└───────────────────────────────────────────┼────────────────────┘
                                            │ (polling)
                                            ↓
                                  ┌──────────────────┐
                                  │ GitHub.com       │
                                  │ (workflows)      │
                                  └──────────────────┘
```

### Avantages

#### 1. Accès Direct au SonarQube Local
- **Connexion directe** : Le runner tourne sur la même machine que SonarQube
- **Pas de configuration réseau** : `localhost:9000` fonctionne directement
- **Performance** : Communication ultra-rapide (localhost)

#### 2. Contrôle Total
- **Personnalisation** : Installer n'importe quel logiciel/outil
- **Ressources dédiées** : CPU/RAM de votre choix (pas limité aux runners GitHub)
- **Cache local** : Maven/npm cache persiste entre builds
- **Environnement custom** : Variables d'environnement, outils spécifiques

#### 3. Gratuit
- **0€** : Utilise vos ressources matérielles existantes
- **Pas de minutes GitHub Actions facturées** : Les runners self-hosted ne consomment pas de quota

#### 4. Données Privées
- **Code reste local** : Ne quitte jamais votre machine
- **SonarQube local** : Analyses stockées localement
- **Conformité** : Pas de transfert de données vers le cloud

#### 5. Debugging Facile
- **Accès direct** : Vous pouvez SSH sur le runner
- **Logs détaillés** : Accès aux logs du runner
- **Tests locaux** : Reproduire les erreurs facilement

### Inconvénients

#### 1. Machine Toujours Allumée
- **Contrainte** : Votre PC doit rester allumé 24/7 pour les builds
- **Consommation électrique** : ~100W × 24h × 30j = ~72 kWh/mois (~12€)
- **Bruit** : Ventilateurs actifs en permanence
- **Usure matérielle** : Réduction de la durée de vie des composants

#### 2. Configuration Complexe
- **Setup initial** : 1-2 heures de configuration
- **Sécurité** : Exposer votre machine à GitHub (risque)
- **Firewall** : Configuration nécessaire
- **Updates** : Maintenance régulière du runner

#### 3. Pas de Scalabilité
- **1 seul runner** : Builds séquentiels (pas de parallélisation)
- **Ressources limitées** : Limité par votre CPU/RAM
- **Pas de failover** : Si votre PC crash, plus de CI/CD

#### 4. Sécurité
- **Token GitHub** : Stocké sur votre machine (risque si compromise)
- **Accès au code** : Le runner a accès à tous vos repos
- **Réseau** : Votre machine communique avec GitHub (firewall à configurer)
- **Risque** : Si votre GitHub est compromis, attaquant peut exécuter du code sur votre PC

#### 5. Disponibilité
- **Dépend de votre connexion Internet** : Pas de builds si coupure
- **Maintenance** : Si vous redémarrez votre PC, runner s'arrête
- **Mobilité** : Impossible de travailler ailleurs avec votre laptop

#### 6. Pas de Backup
- **Pas de redondance** : Un seul runner (SPOF = Single Point of Failure)
- **Pas de monitoring** : Pas d'alertes si le runner tombe

### Coût

**Matériel :** 0€ (utilise votre machine existante)

**Électricité :**
- PC de bureau (100W) allumé 24/7 : ~72 kWh/mois
- Coût électricité France (0.17€/kWh) : ~12€/mois
- Laptop (30W) : ~4€/mois

**Total estimé : 4-12€/mois** (coût électricité uniquement)

### Temps de Setup

**Estimation totale : 1h30 - 2h00**

| Étape | Durée |
|-------|-------|
| Télécharger et installer le runner | 10 min |
| Configurer le runner (token GitHub) | 5 min |
| Configurer le service (démarrage auto) | 10 min |
| Tester la connexion | 5 min |
| Modifier le workflow (labels self-hosted) | 10 min |
| Configurer firewall/antivirus | 20 min |
| Debugging et ajustements | 30 min |
| Tests complets | 10 min |

### Complexité

**Niveau : MOYEN à DIFFICILE**

**Compétences requises :**
- **Linux/Windows administration** : Installation de services
- **Réseaux** : Comprendre ports, firewall, proxies
- **Sécurité** : Gérer des tokens, permissions
- **CI/CD** : Comprendre GitHub Actions (labels, runners)
- **Debugging** : Analyser des logs, résoudre des erreurs

**Obstacles potentiels :**
- Problèmes de permissions (Linux)
- Antivirus bloquant le runner (Windows)
- Firewall corporate bloquant GitHub
- Runner ne se connecte pas (erreurs réseau)

### Recommandation pour Projet École

**NON RECOMMANDÉ** pour les raisons suivantes :

#### Raisons Techniques
1. **Complexité disproportionnée** : 2h de setup vs 15 min pour SonarCloud
2. **Maintenance continue** : Runner à surveiller, mettre à jour
3. **Dépendance matérielle** : Votre PC doit rester allumé

#### Raisons Pédagogiques
- **Pas le focus** : Temps perdu sur l'infra au lieu du code
- **Pas réaliste** : Aucune entreprise ne fait ça (runners cloud ou serveurs dédiés)
- **Risque d'échec** : Si ça casse, projet bloqué

#### Raisons Pratiques
- **Coût électricité** : 12€/mois vs 0€ pour SonarCloud
- **Mobilité** : Impossible de travailler ailleurs (café, bibliothèque)
- **Collaboration** : Si vous êtes en équipe, un seul a le runner

**Exceptions où ça peut avoir du sens :**
- Vous avez déjà un serveur Linux 24/7 chez vous
- Vous voulez apprendre l'administration système
- Vous avez des contraintes de confidentialité strictes

### Étapes d'Implémentation

#### Pré-requis
- Machine Linux (Ubuntu 20.04+) ou Windows 10+
- Connexion Internet stable
- Compte GitHub avec droits Admin sur le repo
- SonarQube déjà installé et fonctionnel sur localhost:9000

#### Étape 1 : Créer un Self-Hosted Runner sur GitHub (5 minutes)

1. **Aller dans les settings du repository**
   ```
   GitHub → safe-zone repository
   → "Settings" tab
   → "Actions" → "Runners"
   → "New self-hosted runner"
   ```

2. **Sélectionner l'OS**
   ```
   Runner image: Linux / macOS / Windows
   Architecture: X64
   ```

3. **Copier les commandes d'installation**

   GitHub affiche des commandes personnalisées, exemple pour Linux :

   ```bash
   # Create a folder
   mkdir actions-runner && cd actions-runner

   # Download the latest runner package
   curl -o actions-runner-linux-x64-2.311.0.tar.gz -L \
     https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz

   # Extract the installer
   tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz
   ```

#### Étape 2 : Installer le Runner (10 minutes)

**Sur Linux (Ubuntu) :**

```bash
# 1. Créer un répertoire pour le runner
mkdir ~/actions-runner
cd ~/actions-runner

# 2. Télécharger le runner (commandes fournies par GitHub)
curl -o actions-runner-linux-x64-2.311.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz

# 3. Extraire
tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz

# 4. Configurer le runner
./config.sh --url https://github.com/VOTRE-USERNAME/safe-zone \
  --token GITHUB_FOURNIT_CE_TOKEN

# Questions interactives :
# Enter the name of the runner: [safe-zone-runner]
# Enter any additional labels (comma separated): [self-hosted,linux,x64]
# Enter name of work folder: [_work]

# 5. Installer les dépendances (si demandé)
sudo ./bin/installdependencies.sh
```

**Sur Windows :**

```powershell
# 1. Créer un répertoire
New-Item -Path C:\actions-runner -ItemType Directory
Set-Location C:\actions-runner

# 2. Télécharger (PowerShell)
Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-win-x64-2.311.0.zip `
  -OutFile actions-runner-win-x64-2.311.0.zip

# 3. Extraire
Expand-Archive -Path .\actions-runner-win-x64-2.311.0.zip -DestinationPath .

# 4. Configurer
.\config.cmd --url https://github.com/VOTRE-USERNAME/safe-zone `
  --token GITHUB_TOKEN_ICI
```

#### Étape 3 : Configurer le Runner comme Service (15 minutes)

**Linux (systemd) :**

```bash
# 1. Installer le service
cd ~/actions-runner
sudo ./svc.sh install

# 2. Démarrer le service
sudo ./svc.sh start

# 3. Vérifier le statut
sudo ./svc.sh status

# 4. Activer au démarrage
sudo systemctl enable actions.runner.VOTRE-USERNAME-safe-zone.safe-zone-runner.service

# 5. Vérifier les logs
sudo journalctl -u actions.runner.* -f
```

**Windows (Service) :**

```powershell
# 1. Ouvrir PowerShell en Administrateur
cd C:\actions-runner

# 2. Installer le service
.\svc.sh install

# 3. Démarrer le service
.\svc.sh start

# 4. Vérifier
Get-Service actions.runner.*
```

#### Étape 4 : Configurer le Firewall (10 minutes)

**Linux (UFW) :**

```bash
# Autoriser la connexion sortante vers GitHub (HTTPS)
sudo ufw allow out 443/tcp comment 'GitHub Actions Runner'

# Vérifier
sudo ufw status
```

**Windows Firewall :**

```
Panneau de configuration
→ Système et sécurité
→ Pare-feu Windows Defender
→ Paramètres avancés
→ Règles de sortie
→ Nouvelle règle...
   - Type : Port
   - Protocole : TCP
   - Port distant : 443
   - Action : Autoriser
   - Nom : GitHub Actions Runner
```

#### Étape 5 : Modifier le Workflow GitHub Actions (10 minutes)

**Fichier : .github/workflows/ci-cd.yml**

Changer `runs-on: ubuntu-latest` par `runs-on: self-hosted` :

```yaml
name: CI/CD with Self-Hosted Runner

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-user-service:
    name: Build User Service
    runs-on: self-hosted  # ← Utilise votre runner
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Maven
        working-directory: backend/user-service
        run: mvn clean install

      - name: SonarQube Scan
        working-directory: backend/user-service
        run: mvn sonar:sonar -Dsonar.host.url=http://localhost:9000
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

  # Répéter pour les autres services...
```

**Note importante :**
- Le runner self-hosted a accès à `localhost:9000` directement
- Pas besoin de changer `sonar.host.url`
- Les secrets GitHub fonctionnent normalement

#### Étape 6 : Tester (10 minutes)

```bash
# 1. Vérifier que le runner est en ligne
# Sur GitHub → Settings → Actions → Runners
# Status: Idle (vert) ✅

# 2. Faire un push pour déclencher le workflow
git add .
git commit -m "test: configure self-hosted runner"
git push origin main

# 3. Surveiller les logs du runner
# Linux :
sudo journalctl -u actions.runner.* -f

# Windows :
# Gestionnaire des tâches → Services → Actions Runner

# 4. Vérifier sur GitHub
# Actions tab → Voir le workflow en cours
# L'icône du runner doit être différente (pas "ubuntu-latest")
```

#### Étape 7 : Sécuriser (15 minutes)

**Bonnes pratiques de sécurité :**

1. **Limiter les repos autorisés**
   ```
   GitHub → Settings → Actions → Runners
   → safe-zone-runner → Edit
   → Restrict to specific repositories
   ```

2. **Créer un utilisateur dédié (Linux)**
   ```bash
   sudo useradd -m -s /bin/bash github-runner
   sudo passwd github-runner

   # Installer le runner sous cet utilisateur
   sudo su - github-runner
   mkdir ~/actions-runner
   # ... répéter l'installation
   ```

3. **Ne jamais exposer le runner publiquement**
   ```
   ⚠️ NE JAMAIS autoriser les workflows de forks à utiliser votre runner
   GitHub → Settings → Actions → General
   → Fork pull request workflows from outside collaborators
   → "Require approval for first-time contributors"
   ```

4. **Monitoring**
   ```bash
   # Ajouter une alerte si le runner s'arrête
   # Script à exécuter en cron toutes les 5 minutes
   #!/bin/bash
   if ! systemctl is-active --quiet actions.runner.*; then
     echo "Runner is down!" | mail -s "Alert" votre@email.com
   fi
   ```

#### Étape 8 : Maintenance (régulier)

**Mises à jour du runner :**

```bash
# 1. Arrêter le runner
sudo ./svc.sh stop

# 2. Télécharger la nouvelle version
cd ~/actions-runner
curl -o actions-runner-linux-x64-NEW.tar.gz -L \
  https://github.com/actions/runner/releases/download/vNEW/actions-runner-linux-x64-NEW.tar.gz

# 3. Extraire (écrase les anciens fichiers)
tar xzf ./actions-runner-linux-x64-NEW.tar.gz

# 4. Redémarrer
sudo ./svc.sh start

# 5. Vérifier
sudo ./svc.sh status
```

**Nettoyage du cache Maven :**

```bash
# Le cache peut grossir (plusieurs GB)
cd ~/.m2/repository
du -sh .
# Si > 5GB :
rm -rf ~/.m2/repository/*
```

### Dépannage

#### Problème 1 : Runner ne se connecte pas

**Symptôme :**
```
Failed to connect. Retrying...
```

**Solutions :**
```bash
# Vérifier la connectivité GitHub
curl -I https://api.github.com
# Doit retourner HTTP/2 200

# Vérifier le token (peut avoir expiré)
# Regénérer un nouveau token sur GitHub
# Re-configurer le runner
./config.sh remove
./config.sh --url ... --token NEW_TOKEN

# Vérifier le firewall
sudo ufw status
# Doit autoriser 443/tcp sortant
```

#### Problème 2 : Permission denied sur Maven

**Symptôme :**
```
[ERROR] Failed to execute goal: Permission denied
```

**Solutions :**
```bash
# Vérifier les permissions du répertoire de travail
cd ~/actions-runner/_work
ls -la

# Changer le propriétaire si nécessaire
sudo chown -R $USER:$USER ~/actions-runner/_work

# Vérifier les permissions Maven
chmod -R 755 ~/.m2
```

#### Problème 3 : SonarQube inaccessible

**Symptôme :**
```
[ERROR] Unable to connect to http://localhost:9000
```

**Solutions :**
```bash
# Vérifier que SonarQube tourne
curl http://localhost:9000/api/system/status
# Doit retourner {"status":"UP"}

# Si runner tourne dans un container/VM
# Utiliser l'IP de l'hôte au lieu de localhost
ip addr show
# Exemple : http://192.168.1.100:9000

# Modifier sonar.host.url dans le workflow
```

---

## 4. Solution 3 : Serveur Cloud SonarQube

### Description Technique

Installer SonarQube sur un serveur cloud (VPS, VM) accessible publiquement via une IP ou un nom de domaine. Les runners GitHub Actions peuvent alors se connecter à `https://sonarqube.votredomaine.com` au lieu de `localhost:9000`.

**Architecture :**
```
GitHub Actions Runner (ubuntu-latest)
       ↓ (HTTPS via Internet)
Serveur Cloud (VPS/EC2/DigitalOcean)
       ↓
SonarQube Server
(ex: https://sonarqube.votredomaine.com)
```

**Technologies :**
- VPS : DigitalOcean, AWS EC2, Hetzner, OVH, etc.
- OS : Ubuntu Server 22.04 LTS
- SonarQube : Version Community (gratuite)
- Base de données : PostgreSQL 14
- Reverse Proxy : Nginx + Let's Encrypt (SSL)

### Avantages

#### 1. Accessible de Partout
- **URL publique** : Accessible via Internet (https://sonarqube.votre-domaine.com)
- **GitHub Actions compatible** : Runners peuvent se connecter directement
- **Collaboration** : Toute l'équipe peut accéder au dashboard
- **Mobile** : Consultation sur smartphone

#### 2. Contrôle Total
- **Plugins** : Installation de plugins custom SonarQube
- **Configuration** : Personnalisation complète (Quality Gates, règles)
- **Rétention de données** : Conservation illimitée des analyses
- **Données privées** : Votre serveur, vos données (pas de cloud tiers)

#### 3. Performance Prévisible
- **Ressources dédiées** : CPU/RAM garantis (pas partagé)
- **Scalabilité** : Upgrade RAM/CPU selon besoin
- **Pas de quotas** : Analyses illimitées

#### 4. Expérience Professionnelle
- **Setup réaliste** : Similaire à une entreprise
- **DevOps skills** : Apprentissage Linux, Nginx, SSL, databases
- **Portfolio** : Démontre des compétences sysadmin

### Inconvénients

#### 1. Coût Récurrent
- **VPS** : 5-20€/mois minimum
- **Nom de domaine** : 10-15€/an (optionnel si vous utilisez l'IP)
- **Backup** : Coût additionnel si service géré

**Coût total : 60-250€/an**

#### 2. Configuration Complexe
- **Setup initial** : 3-5 heures (Linux, PostgreSQL, Nginx, SSL)
- **Compétences requises** : Administration système Linux
- **Debugging** : Résoudre des problèmes serveur

#### 3. Maintenance Lourde
- **Mises à jour** : SonarQube, PostgreSQL, OS, Nginx
- **Sécurité** : Patcher les vulnérabilités
- **Monitoring** : Surveiller uptime, espace disque
- **Backups** : Configurer et tester les sauvegardes

**Temps de maintenance : 2-4 heures/mois**

#### 4. Sécurité à Gérer
- **Firewall** : Configuration iptables/ufw
- **SSL/TLS** : Certificats Let's Encrypt à renouveler (auto avec certbot)
- **Authentification** : Gérer les comptes utilisateurs SonarQube
- **Exposition Internet** : Risque d'attaques (DDoS, brute-force)

#### 5. Ressources Minimales Requises
- **RAM** : 4GB minimum (SonarQube + PostgreSQL + OS)
- **CPU** : 2 vCPUs minimum
- **Stockage** : 20GB minimum (plus pour historique analyses)

Un VPS avec ces specs coûte ~10€/mois.

#### 6. Disponibilité
- **Pas de SLA** : Si le VPS tombe, SonarQube inaccessible
- **Redémarrage nécessaire** : Après updates OS/SonarQube
- **Dépendance fournisseur** : Si DigitalOcean a un outage, votre CI/CD stop

### Coût

**VPS / Cloud Provider :**

| Provider | Specs | Prix/mois | Note |
|----------|-------|-----------|------|
| DigitalOcean Droplet | 2 vCPU, 4GB RAM, 80GB SSD | 24 USD (~22€) | Recommandé |
| Hetzner Cloud | 2 vCPU, 4GB RAM, 40GB SSD | 5.39€ | Excellent rapport qualité/prix |
| AWS EC2 t3.medium | 2 vCPU, 4GB RAM | ~33 USD (~30€) | Cher mais fiable |
| OVH VPS | 2 vCPU, 4GB RAM, 80GB SSD | 7€ | Bon prix |
| Scaleway DEV1-M | 3 vCPU, 4GB RAM, 40GB SSD | ~12€ | Bonne option |

**Recommandation : Hetzner Cloud (5.39€/mois) ou OVH (7€/mois)**

**Coûts additionnels :**
- **Nom de domaine** : 10-15€/an (ex: Namecheap, Gandi)
  - Optionnel : Vous pouvez utiliser directement l'IP du VPS
- **Backups automatiques** : +1-2€/mois (optionnel mais recommandé)

**Total estimé : 65-280€/an**

Pour un projet étudiant : **Minimum 5.39€/mois × 12 mois = 64.68€/an**

### Temps de Setup

**Estimation totale : 3h00 - 5h00** (première fois)

| Étape | Durée |
|-------|-------|
| Créer et configurer le VPS | 10 min |
| Sécuriser le serveur (SSH, firewall) | 30 min |
| Installer PostgreSQL | 15 min |
| Installer SonarQube | 20 min |
| Configurer Nginx reverse proxy | 20 min |
| Installer SSL (Let's Encrypt) | 15 min |
| Configurer le domaine (DNS) | 10 min |
| Tester SonarQube | 10 min |
| Configurer les pom.xml | 10 min |
| Créer les projets SonarQube | 15 min |
| Tester avec GitHub Actions | 10 min |
| Debugging et ajustements | 60 min |

**Si vous avez déjà de l'expérience Linux : 2h00**

### Complexité

**Niveau : DIFFICILE**

**Compétences requises :**
- **Linux avancé** : CLI, systemd, permissions, logs
- **Base de données** : Installation et configuration PostgreSQL
- **Réseaux** : DNS, firewall, ports, reverse proxy
- **Sécurité** : SSH keys, SSL/TLS, authentification
- **DevOps** : Monitoring, backups, troubleshooting

**Obstacles potentiels :**
- SonarQube ne démarre pas (erreurs JVM, RAM insuffisante)
- PostgreSQL mal configuré
- Nginx mal configuré (502 Bad Gateway)
- Certificat SSL non émis (problème DNS)
- Firewall bloque les connexions
- Performances insuffisantes (serveur trop petit)

### Recommandation pour Projet École

**NON RECOMMANDÉ** pour les raisons suivantes :

#### Raisons Financières
- **Coût élevé** : 65-280€/an pour un projet éducatif
- **SonarCloud gratuit** : Alternative à 0€ disponible
- **Budget étudiant limité** : Argent mieux investi ailleurs

#### Raisons Techniques
- **Complexité disproportionnée** : 4h de setup vs 15 min pour SonarCloud
- **Maintenance lourde** : 3-4h/mois (updates, monitoring)
- **Risque d'échec** : Beaucoup de points de défaillance possibles

#### Raisons Pédagogiques
- **Hors sujet** : Focus sur sysadmin au lieu de développement
- **Temps perdu** : 4h setup + 3h/mois maintenance = temps non dédié au projet
- **Pas nécessaire** : SonarCloud fait le même job sans effort

**Exceptions où ça peut avoir du sens :**
- Vous voulez **apprendre le DevOps** (objectif explicite du projet)
- Vous avez **déjà un VPS** payé (coût marginal = 0€)
- Contraintes de **confidentialité stricte** (données sensibles)
- Vous êtes en **formation DevOps/SysAdmin** (pertinent pédagogiquement)

**Si vous choisissez quand même cette solution : Budget 10-15h pour le setup + maintenance**

### Étapes d'Implémentation

#### Pré-requis
- Compte chez un provider cloud (Hetzner, DigitalOcean, OVH)
- Nom de domaine (optionnel) : ex. `votredomaine.com`
- Carte bancaire pour paiement VPS
- Connaissance de base Linux (SSH, apt, systemd)

#### Étape 1 : Créer le VPS (10 minutes)

**Exemple avec Hetzner Cloud :**

1. **Créer un compte sur hetzner.com**
   ```
   Aller sur https://www.hetzner.com/cloud
   → "Sign Up"
   → Créer un compte (email + mot de passe)
   → Vérifier l'email
   ```

2. **Créer un projet**
   ```
   Dashboard → "New Project"
   Name: SonarQube-SafeZone
   → "Create Project"
   ```

3. **Créer un serveur**
   ```
   → "Add Server"

   Location: Nuremberg (Allemagne) ou Helsinki (Finlande)
   Image: Ubuntu 22.04
   Type: CPX21 (2 vCPU, 4GB RAM, 40GB SSD) - 5.39€/mois

   SSH Key:
   - Si première fois : Générer une clé SSH sur votre PC
     Linux/Mac: ssh-keygen -t ed25519 -C "votre@email.com"
     Windows: Utiliser PuTTYgen
   - Copier la clé publique (~/.ssh/id_ed25519.pub)
   - Coller dans Hetzner

   Firewall: (on configure après)
   Backups: Optionnel (+20% du prix)

   Server name: sonarqube-server

   → "Create & Buy now"
   ```

4. **Noter l'IP publique**
   ```
   Le serveur est créé en ~1 minute
   IP publique: 78.47.123.45 (exemple)
   → Copier cette IP
   ```

#### Étape 2 : Sécuriser le Serveur (30 minutes)

1. **Se connecter en SSH**
   ```bash
   ssh root@78.47.123.45
   # Accepter la fingerprint
   ```

2. **Mettre à jour le système**
   ```bash
   apt update && apt upgrade -y
   ```

3. **Créer un utilisateur non-root**
   ```bash
   adduser sonarqube
   # Entrer un mot de passe fort

   # Ajouter aux sudoers
   usermod -aG sudo sonarqube

   # Copier la clé SSH
   rsync --archive --chown=sonarqube:sonarqube ~/.ssh /home/sonarqube
   ```

4. **Configurer le firewall (UFW)**
   ```bash
   # Installer UFW
   apt install ufw -y

   # Règles de base
   ufw default deny incoming
   ufw default allow outgoing

   # Autoriser SSH (important!)
   ufw allow 22/tcp

   # Autoriser HTTP et HTTPS (pour SonarQube)
   ufw allow 80/tcp
   ufw allow 443/tcp

   # Activer le firewall
   ufw enable

   # Vérifier
   ufw status
   ```

5. **Désactiver le login root par SSH**
   ```bash
   nano /etc/ssh/sshd_config

   # Changer ces lignes:
   PermitRootLogin no
   PasswordAuthentication no

   # Sauvegarder (Ctrl+X, Y, Enter)

   # Redémarrer SSH
   systemctl restart sshd
   ```

6. **Se reconnecter avec le nouvel utilisateur**
   ```bash
   # Quitter la session root
   exit

   # Se reconnecter
   ssh sonarqube@78.47.123.45
   ```

#### Étape 3 : Installer PostgreSQL (15 minutes)

```bash
# 1. Installer PostgreSQL 14
sudo apt install postgresql postgresql-contrib -y

# 2. Vérifier que ça tourne
sudo systemctl status postgresql

# 3. Se connecter à PostgreSQL
sudo -u postgres psql

# 4. Créer la base de données et l'utilisateur pour SonarQube
-- Dans le prompt psql :
CREATE DATABASE sonarqube;
CREATE USER sonarqube WITH ENCRYPTED PASSWORD 'MotDePasseFort123!';
GRANT ALL PRIVILEGES ON DATABASE sonarqube TO sonarqube;
ALTER DATABASE sonarqube OWNER TO sonarqube;
\q

# 5. Configurer PostgreSQL pour SonarQube
sudo nano /etc/postgresql/14/main/postgresql.conf

# Trouver et modifier ces lignes :
max_connections = 300
shared_buffers = 1GB
effective_cache_size = 3GB

# Sauvegarder et redémarrer
sudo systemctl restart postgresql
```

#### Étape 4 : Installer SonarQube (20 minutes)

```bash
# 1. Installer Java 17 (requis par SonarQube)
sudo apt install openjdk-17-jdk -y
java -version
# Doit afficher: openjdk version "17.0.x"

# 2. Télécharger SonarQube Community Edition
cd /opt
sudo wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.3.0.82913.zip

# 3. Installer unzip
sudo apt install unzip -y

# 4. Extraire
sudo unzip sonarqube-10.3.0.82913.zip
sudo mv sonarqube-10.3.0.82913 sonarqube

# 5. Créer un utilisateur système pour SonarQube
sudo useradd -r -s /bin/bash sonarqube-system

# 6. Donner les permissions
sudo chown -R sonarqube-system:sonarqube-system /opt/sonarqube

# 7. Configurer la connexion à PostgreSQL
sudo nano /opt/sonarqube/conf/sonar.properties

# Ajouter/modifier ces lignes :
sonar.jdbc.username=sonarqube
sonar.jdbc.password=MotDePasseFort123!
sonar.jdbc.url=jdbc:postgresql://localhost/sonarqube
sonar.web.host=127.0.0.1
sonar.web.port=9000

# Sauvegarder (Ctrl+X, Y, Enter)

# 8. Créer un service systemd
sudo nano /etc/systemd/system/sonarqube.service

# Contenu du fichier :
[Unit]
Description=SonarQube service
After=syslog.target network.target postgresql.service

[Service]
Type=forking
ExecStart=/opt/sonarqube/bin/linux-x86-64/sonar.sh start
ExecStop=/opt/sonarqube/bin/linux-x86-64/sonar.sh stop
User=sonarqube-system
Group=sonarqube-system
Restart=always
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target

# Sauvegarder

# 9. Démarrer SonarQube
sudo systemctl daemon-reload
sudo systemctl start sonarqube
sudo systemctl enable sonarqube

# 10. Vérifier les logs
sudo tail -f /opt/sonarqube/logs/sonar.log
# Attendre de voir: "SonarQube is up"

# 11. Tester localement
curl http://localhost:9000
# Doit retourner du HTML
```

#### Étape 5 : Configurer Nginx Reverse Proxy (20 minutes)

```bash
# 1. Installer Nginx
sudo apt install nginx -y

# 2. Créer la configuration SonarQube
sudo nano /etc/nginx/sites-available/sonarqube

# Contenu du fichier :
server {
    listen 80;
    server_name sonarqube.votredomaine.com;  # Ou votre IP

    location / {
        proxy_pass http://127.0.0.1:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts pour analyses longues
        proxy_connect_timeout 600;
        proxy_send_timeout 600;
        proxy_read_timeout 600;
        send_timeout 600;
    }
}

# Sauvegarder

# 3. Activer la configuration
sudo ln -s /etc/nginx/sites-available/sonarqube /etc/nginx/sites-enabled/

# 4. Tester la config Nginx
sudo nginx -t
# Doit afficher: "syntax is okay"

# 5. Redémarrer Nginx
sudo systemctl restart nginx

# 6. Tester depuis votre PC
# Ouvrir un navigateur: http://78.47.123.45
# Vous devriez voir la page de login SonarQube
```

#### Étape 6 : Installer SSL avec Let's Encrypt (15 minutes)

**Pré-requis : Avoir un nom de domaine pointant vers l'IP du serveur**

```bash
# 1. Installer Certbot
sudo apt install certbot python3-certbot-nginx -y

# 2. Obtenir le certificat SSL
sudo certbot --nginx -d sonarqube.votredomaine.com

# Questions interactives :
# Email: votre@email.com
# Accepter les conditions: Y
# Partager email avec EFF: N (optionnel)
# Redirect HTTP to HTTPS: 2 (Redirect)

# 3. Vérifier le certificat
sudo certbot certificates

# 4. Tester le renouvellement automatique
sudo certbot renew --dry-run

# 5. Vérifier dans un navigateur
# https://sonarqube.votredomaine.com
# Doit afficher le cadenas vert (certificat valide)
```

**Si vous n'avez pas de domaine (utilisation de l'IP) :**

Vous devrez utiliser HTTP (pas HTTPS) ou un certificat auto-signé.

```bash
# Certificat auto-signé (non recommandé pour production)
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/ssl/private/sonarqube-selfsigned.key \
  -out /etc/ssl/certs/sonarqube-selfsigned.crt

# Modifier la config Nginx pour utiliser ce certificat
sudo nano /etc/nginx/sites-available/sonarqube
# Ajouter les lignes SSL
```

#### Étape 7 : Configurer SonarQube (20 minutes)

```bash
# 1. Accéder à SonarQube
# Navigateur: https://sonarqube.votredomaine.com

# 2. Login initial
# Username: admin
# Password: admin

# 3. Changer le mot de passe admin
# SonarQube va forcer le changement
# Nouveau password: MotDePasseFortSonar123!

# 4. Créer les 4 projets
# Projects → Create Project → Manually

# Projet 1 : User Service
Project Key: safe-zone_user-service
Display Name: E-Commerce User Service

# Projet 2 : Product Service
Project Key: safe-zone_product-service
Display Name: E-Commerce Product Service

# Projet 3 : Media Service
Project Key: safe-zone_media-service
Display Name: E-Commerce Media Service

# Projet 4 : Frontend
Project Key: safe-zone_frontend
Display Name: E-Commerce Frontend

# 5. Générer un token global
# My Account → Security → Generate Tokens
Token Name: GitHub-Actions-Token
Type: Global Analysis Token
Expires in: 90 days (ou No expiration)
→ Generate

# Copier le token: squ_abc123def456ghi789jkl012mno345pqr678stu
```

#### Étape 8 : Configurer les pom.xml (10 minutes)

**Modifier les 3 fichiers pom.xml (user-service, product-service, media-service) :**

```xml
<properties>
    <java.version>17</java.version>

    <!-- SonarQube Self-Hosted -->
    <sonar.host.url>https://sonarqube.votredomaine.com</sonar.host.url>
    <sonar.projectKey>safe-zone_user-service</sonar.projectKey>
    <sonar.projectName>E-Commerce User Service</sonar.projectName>

    <!-- Coverage -->
    <sonar.java.binaries>target/classes</sonar.java.binaries>
    <sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>

    <!-- Exclusions -->
    <sonar.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.exclusions>
    <sonar.coverage.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.coverage.exclusions>
</properties>
```

**Créer `frontend/sonar-project.properties` :**

```properties
sonar.host.url=https://sonarqube.votredomaine.com
sonar.projectKey=safe-zone_frontend
sonar.projectName=E-Commerce Frontend

sonar.sources=src
sonar.exclusions=**/node_modules/**,**/*.spec.ts

sonar.tests=src
sonar.test.inclusions=**/*.spec.ts

sonar.javascript.lcov.reportPaths=coverage/lcov.info
```

#### Étape 9 : Configurer GitHub Actions (10 minutes)

**Ajouter le token dans GitHub Secrets :**

```
GitHub → safe-zone repository
→ Settings → Secrets and variables → Actions
→ New repository secret
Name: SONAR_TOKEN
Value: squ_abc123def456ghi789jkl012mno345pqr678stu
→ Add secret
```

**Créer `.github/workflows/ci-cd.yml` :**

```yaml
name: CI/CD with Self-Hosted SonarQube

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-user-service:
    name: Build & Analyze User Service
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven
        uses: actions/cache@v3
        with:
          path: ~/.m2/repository
          key: maven-${{ hashFiles('**/pom.xml') }}

      - name: Build and Test
        working-directory: backend/user-service
        run: mvn clean verify

      - name: SonarQube Scan
        working-directory: backend/user-service
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar

  # Répéter pour product-service et media-service...

  build-frontend:
    name: Build & Analyze Frontend
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Run tests
        working-directory: frontend
        run: npm run test -- --watch=false --code-coverage

      - name: Build
        working-directory: frontend
        run: npm run build

      - name: SonarQube Scan
        uses: SonarSource/sonarcloud-github-action@v2.3.0
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: https://sonarqube.votredomaine.com
        with:
          projectBaseDir: frontend
```

#### Étape 10 : Tester (10 minutes)

```bash
# 1. Commit et push
git add .
git commit -m "ci: configure self-hosted SonarQube"
git push origin main

# 2. Surveiller GitHub Actions
# GitHub → Actions tab → Voir le workflow

# 3. Vérifier SonarQube
# https://sonarqube.votredomaine.com
# → Projects → Voir les 4 projets avec résultats

# 4. Si erreurs, vérifier les logs
# GitHub Actions : Cliquer sur le job failed
# SonarQube Server:
ssh sonarqube@78.47.123.45
sudo tail -f /opt/sonarqube/logs/web.log
```

#### Étape 11 : Maintenance (régulier)

**Mises à jour mensuelles :**

```bash
# 1. Se connecter au serveur
ssh sonarqube@78.47.123.45

# 2. Mettre à jour l'OS
sudo apt update && sudo apt upgrade -y

# 3. Mettre à jour SonarQube (tous les 2-3 mois)
# Télécharger la nouvelle version
cd /opt
sudo wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-NEW-VERSION.zip

# Arrêter SonarQube
sudo systemctl stop sonarqube

# Backup de la config
sudo cp -r /opt/sonarqube/conf /opt/sonarqube-conf-backup

# Extraire la nouvelle version
sudo unzip sonarqube-NEW-VERSION.zip
sudo rm -rf /opt/sonarqube
sudo mv sonarqube-NEW-VERSION /opt/sonarqube

# Restaurer la config
sudo cp /opt/sonarqube-conf-backup/* /opt/sonarqube/conf/

# Permissions
sudo chown -R sonarqube-system:sonarqube-system /opt/sonarqube

# Redémarrer
sudo systemctl start sonarqube

# Vérifier
sudo systemctl status sonarqube
```

**Monitoring de l'espace disque :**

```bash
df -h
# Si /opt est > 80% :
sudo du -sh /opt/sonarqube/data/*
# Supprimer les anciennes analyses si nécessaire
```

---

## 5. Solution 4 : Tunnel Ngrok/Cloudflare

### Description Technique

Créer un **tunnel** qui expose votre SonarQube local (`localhost:9000`) temporairement sur Internet via une URL publique. Les GitHub Actions peuvent alors se connecter à cette URL pour envoyer les analyses.

**Architecture :**
```
Votre Machine Locale
  ↓
SonarQube (localhost:9000)
  ↓
Tunnel Client (Ngrok/Cloudflare)
  ↓ (connexion sortante chiffrée)
Serveur Tunnel Cloud (ngrok.io / cloudflare.com)
  ↓
URL publique (https://abc123.ngrok.io)
  ↓
GitHub Actions Runner
```

**Technologies disponibles :**
- **Ngrok** : Service le plus populaire pour tunneling
- **Cloudflare Tunnel** : Service gratuit de Cloudflare (ex-Argo Tunnel)
- **LocalTunnel** : Alternative open source
- **Serveo** : Service SSH-based

### Avantages

#### 1. Setup Très Rapide
- **5-10 minutes** : Télécharger, installer, lancer une commande
- **Pas de configuration serveur** : Pas de VPS, pas de DNS
- **Pas de connaissances réseau** : Le tunnel gère tout

#### 2. Gratuit (avec limitations)
- **Ngrok Free Tier** : 1 tunnel gratuit (suffisant pour SonarQube)
- **Cloudflare Tunnel** : Complètement gratuit, illimité
- **Pas de coût infrastructure** : Utilise votre machine locale

#### 3. Flexibilité
- **Démarrage/arrêt à la demande** : Tunnel actif seulement quand nécessaire
- **Pas de machine dédiée** : Utilise votre PC de dev
- **Test facile** : Tester une config avant de la déployer

#### 4. Sécurité Relative
- **Chiffrement TLS** : Connexion sécurisée via HTTPS
- **URL aléatoire** : Difficile à deviner (ex: `abc-123-xyz.ngrok.io`)
- **Authentification possible** : Ngrok supporte l'auth HTTP basic

### Inconvénients

#### 1. Disponibilité Limitée
- **Machine doit être allumée** : Votre PC doit tourner 24/7 pour les builds
- **Connexion Internet requise** : Builds impossibles si coupure Internet
- **Tunnel doit être actif** : Si vous oubliez de lancer le tunnel, CI/CD échoue

#### 2. URL Dynamique (Plan Gratuit)
- **Ngrok Free** : URL change à chaque redémarrage (`https://abc123.ngrok.io` → `https://xyz789.ngrok.io`)
- **Impact** : Nécessite de mettre à jour les configs à chaque fois
- **Solution** : Plan payant Ngrok (8$/mois) pour domaine fixe

#### 3. Performance Imprévisible
- **Latence** : Ajout de ~50-200ms (passage par serveurs tunnel)
- **Bande passante limitée** : Plan gratuit souvent throttled
- **Stabilité** : Déconnexions possibles (tunnel doit reconnect)

#### 4. Limitations du Plan Gratuit

**Ngrok Free :**
- 1 tunnel simultané seulement
- URL aléatoire à chaque redémarrage
- 40 connexions/minute max
- Session expire après inactivité

**Cloudflare Tunnel Free :**
- Gratuit mais nécessite un domaine Cloudflare
- Configuration plus complexe que Ngrok

#### 5. Sécurité Discutable
- **Exposition publique** : Votre SonarQube local accessible depuis Internet
- **Risque d'attaque** : Brute-force sur le login SonarQube
- **Logs d'accès** : Ngrok/Cloudflare peuvent logger les requêtes
- **Confiance tier-party** : Dépendance à un service externe

#### 6. Pas Professionnel
- **Hacky** : Pas une solution "production-ready"
- **Non recommandé officiellement** : Ni par SonarQube ni par GitHub
- **Maintenance** : Tunnel peut casser, URL peut changer

### Coût

**Ngrok :**
- **Free Plan** : 0€ (1 tunnel, URL dynamique)
- **Personal Plan** : 8 USD/mois (~7€) (domaine fixe, 3 tunnels)
- **Pro Plan** : 20 USD/mois (~18€) (plus de features)

**Cloudflare Tunnel :**
- **Gratuit** : 0€ (mais nécessite un domaine chez Cloudflare)
- **Domaine** : 10-15€/an si vous n'en avez pas

**LocalTunnel / Serveo :**
- **Gratuit** : 0€

**Recommandation : Cloudflare Tunnel (gratuit) ou Ngrok Free (0€, accepter URL dynamique)**

**Coût total : 0-10€/an**

### Temps de Setup

**Estimation totale : 30 minutes - 1 heure**

| Étape | Durée |
|-------|-------|
| Créer compte Ngrok/Cloudflare | 5 min |
| Installer le client tunnel | 3 min |
| Lancer le tunnel | 2 min |
| Tester l'accès via URL publique | 5 min |
| Configurer pom.xml avec URL tunnel | 5 min |
| Modifier workflow GitHub Actions | 5 min |
| Automatiser le démarrage du tunnel | 15 min |
| Tester un build complet | 10 min |

**Temps de maintenance : ~10 min/semaine**
- Relancer le tunnel si crash
- Mettre à jour l'URL si elle change (Ngrok free)

### Complexité

**Niveau : FACILE à MOYEN**

**Compétences requises :**
- **CLI basique** : Lancer des commandes
- **Édition de configs** : Modifier pom.xml
- **Compréhension réseaux** : Comprendre ce qu'est un tunnel
- **Scripting (optionnel)** : Automatiser le démarrage du tunnel

**Obstacles potentiels :**
- Tunnel se déconnecte régulièrement
- URL Ngrok change → configs à mettre à jour
- Firewall/antivirus bloque Ngrok
- Latence élevée → builds lents

### Recommandation pour Projet École

**DÉCONSEILLÉ** mais **acceptable comme solution temporaire**

#### Quand ça peut avoir du Sens

**Situation 1 : Tests Rapides**
- Vous voulez tester SonarQube en CI/CD **maintenant**
- Vous hésitez encore entre localhost et SonarCloud
- Vous voulez valider que ça fonctionne avant de migrer

**Situation 2 : Contraintes Temporaires**
- Projet de 1-2 semaines seulement
- Pas le temps de setup SonarCloud
- Besoin d'une démo rapide

**Situation 3 : Apprentissage**
- Vous voulez comprendre comment marchent les tunnels
- Objectif pédagogique : explorer différentes solutions

#### Pourquoi ce n'est Pas Idéal

1. **Disponibilité** : Votre PC doit être allumé pendant les builds
2. **Maintenance** : URL peut changer (Ngrok free)
3. **Performance** : Latence ajoutée (~100ms)
4. **Professionnalisme** : Pas une solution "propre"

#### Recommandation Finale

**Si vous choisissez cette solution :**
- Utilisez **Cloudflare Tunnel** (gratuit, URL fixe avec domaine)
- Ou utilisez **Ngrok** mais **passez à SonarCloud rapidement** (migration facile)
- Considérez ça comme une solution **temporaire** (1-2 semaines max)

**Meilleure alternative :** Passer **15 minutes à setup SonarCloud** au lieu de 30 min sur Ngrok

### Étapes d'Implémentation

#### Option A : Ngrok (plus simple)

##### Étape 1 : Créer un Compte Ngrok (5 minutes)

```
1. Aller sur https://ngrok.com
2. Cliquer sur "Sign up"
3. S'inscrire avec GitHub ou email
4. Vérifier l'email
5. Se connecter au dashboard
```

##### Étape 2 : Installer Ngrok (3 minutes)

**Linux :**
```bash
# Télécharger
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz

# Extraire
tar xvzf ngrok-v3-stable-linux-amd64.tgz

# Déplacer vers /usr/local/bin
sudo mv ngrok /usr/local/bin/

# Vérifier
ngrok version
```

**macOS :**
```bash
# Avec Homebrew
brew install ngrok/ngrok/ngrok

# Vérifier
ngrok version
```

**Windows :**
```
1. Télécharger depuis https://ngrok.com/download
2. Extraire le ZIP
3. Déplacer ngrok.exe dans C:\Windows\System32\
4. Ouvrir CMD : ngrok version
```

##### Étape 3 : Authentifier Ngrok (2 minutes)

```bash
# Copier votre authtoken depuis le dashboard Ngrok
# https://dashboard.ngrok.com/get-started/your-authtoken

ngrok config add-authtoken VOTRE_AUTHTOKEN_ICI
# Exemple: ngrok config add-authtoken 2abc123DEF456ghi789JKL012mno345PQR678
```

##### Étape 4 : Lancer le Tunnel (2 minutes)

**Démarrer SonarQube local d'abord :**
```bash
# Vérifier que SonarQube tourne
curl http://localhost:9000/api/system/status
# Doit retourner: {"status":"UP"}
```

**Lancer Ngrok :**
```bash
ngrok http 9000
```

**Résultat :**
```
ngrok

Session Status                online
Account                       Votre Nom (Plan: Free)
Version                       3.x.x
Region                        United States (us)
Latency                       45ms
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://abc-123-xyz.ngrok-free.app -> http://localhost:9000

Connections                   ttl     opn     rt1     rt5     p50     p90
                              0       0       0.00    0.00    0.00    0.00
```

**Noter l'URL publique :** `https://abc-123-xyz.ngrok-free.app`

##### Étape 5 : Tester l'Accès (5 minutes)

```bash
# Depuis votre PC
curl https://abc-123-xyz.ngrok-free.app/api/system/status

# Depuis un navigateur
# Ouvrir https://abc-123-xyz.ngrok-free.app
# Vous devez voir la page de login SonarQube

# Note : Ngrok free affiche parfois une page d'avertissement
# Cliquer sur "Visit Site" pour continuer
```

##### Étape 6 : Configurer les pom.xml (5 minutes)

**Modifier les 3 pom.xml (user-service, product-service, media-service) :**

```xml
<properties>
    <java.version>17</java.version>

    <!-- SonarQube via Ngrok Tunnel -->
    <sonar.host.url>https://abc-123-xyz.ngrok-free.app</sonar.host.url>
    <sonar.projectKey>ecommerce-user-service</sonar.projectKey>
    <sonar.projectName>E-Commerce User Service</sonar.projectName>

    <!-- Coverage -->
    <sonar.java.binaries>target/classes</sonar.java.binaries>
    <sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>

    <!-- Exclusions -->
    <sonar.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.exclusions>
    <sonar.coverage.exclusions>**/config/**,**/dto/**,**/entity/**,**/*Application.java</sonar.coverage.exclusions>
</properties>
```

**IMPORTANT : L'URL Ngrok change à chaque redémarrage (plan gratuit)**

##### Étape 7 : Créer un Script de Démarrage Automatique (15 minutes)

**Fichier : `start-ngrok-tunnel.sh` (Linux/Mac)**

```bash
#!/bin/bash

# Script pour démarrer automatiquement le tunnel Ngrok

echo "Starting SonarQube..."
# Démarrer SonarQube si pas déjà lancé
# (Adapter selon votre méthode de démarrage)

# Attendre que SonarQube soit prêt
echo "Waiting for SonarQube to be ready..."
until curl -s http://localhost:9000/api/system/status | grep -q '"status":"UP"'; do
  echo "SonarQube not ready yet, waiting..."
  sleep 5
done

echo "SonarQube is ready!"

# Lancer Ngrok en arrière-plan
echo "Starting Ngrok tunnel..."
ngrok http 9000 > /dev/null &

# Attendre que le tunnel soit établi
sleep 5

# Récupérer l'URL publique via l'API Ngrok
NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | grep -oP '"public_url":"https://[^"]+' | grep -oP 'https://[^"]+')

echo "========================================="
echo "Tunnel Ngrok actif !"
echo "URL publique : $NGROK_URL"
echo "========================================="

# Mettre à jour les pom.xml automatiquement
echo "Updating pom.xml files..."
find backend -name "pom.xml" -exec sed -i "s|<sonar.host.url>.*</sonar.host.url>|<sonar.host.url>$NGROK_URL</sonar.host.url>|" {} \;

echo "pom.xml files updated!"
echo "You can now run GitHub Actions builds."
```

**Rendre le script exécutable :**
```bash
chmod +x start-ngrok-tunnel.sh
```

**Lancer le script :**
```bash
./start-ngrok-tunnel.sh
```

##### Étape 8 : Configurer GitHub Actions (5 minutes)

**Créer `.github/workflows/ci-cd.yml` :**

```yaml
name: CI/CD with Ngrok Tunnel

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-user-service:
    name: Build & Analyze User Service
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven
        uses: actions/cache@v3
        with:
          path: ~/.m2/repository
          key: maven-${{ hashFiles('**/pom.xml') }}

      - name: Build and Test
        working-directory: backend/user-service
        run: mvn clean verify

      - name: SonarQube Scan
        working-directory: backend/user-service
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar

  # Répéter pour les autres services...
```

**Note :** L'URL Ngrok est déjà dans les pom.xml (mise à jour par le script)

##### Étape 9 : Tester (10 minutes)

```bash
# 1. Vérifier que le tunnel est actif
curl https://abc-123-xyz.ngrok-free.app/api/system/status

# 2. Commit et push
git add .
git commit -m "ci: configure Ngrok tunnel for SonarQube"
git push origin main

# 3. Surveiller GitHub Actions
# GitHub → Actions tab → Voir le workflow

# 4. Surveiller Ngrok
# Ouvrir http://localhost:4040 (Web Interface Ngrok)
# Voir les requêtes en temps réel
```

#### Option B : Cloudflare Tunnel (plus stable, gratuit)

##### Étape 1 : Installer Cloudflared (5 minutes)

**Linux :**
```bash
# Télécharger
wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb

# Installer
sudo dpkg -i cloudflared-linux-amd64.deb

# Vérifier
cloudflared version
```

**macOS :**
```bash
brew install cloudflare/cloudflare/cloudflared
```

**Windows :**
```
Télécharger depuis https://github.com/cloudflare/cloudflared/releases
Extraire et ajouter au PATH
```

##### Étape 2 : Authentifier Cloudflare (3 minutes)

```bash
cloudflared tunnel login
```

Cela ouvre un navigateur pour vous connecter à votre compte Cloudflare et autoriser le tunnel.

##### Étape 3 : Créer un Tunnel (5 minutes)

```bash
# Créer un tunnel nommé
cloudflared tunnel create sonarqube-tunnel

# Noter le Tunnel ID affiché
# Exemple: abc123-def456-ghi789-jkl012
```

##### Étape 4 : Configurer le Tunnel (10 minutes)

**Créer `~/.cloudflared/config.yml` :**

```yaml
url: http://localhost:9000
tunnel: abc123-def456-ghi789-jkl012
credentials-file: /home/votre-user/.cloudflared/abc123-def456-ghi789-jkl012.json
```

##### Étape 5 : Router le Tunnel vers un Domaine (optionnel)

Si vous avez un domaine chez Cloudflare :

```bash
cloudflared tunnel route dns sonarqube-tunnel sonarqube.votredomaine.com
```

Sinon, utiliser l'URL temporaire fournie par Cloudflare.

##### Étape 6 : Lancer le Tunnel (2 minutes)

```bash
cloudflared tunnel run sonarqube-tunnel
```

**Résultat :**
```
INFO  Starting tunnel connection
INFO  Registered tunnel connection
INFO  Connection established: https://abc123.cfargotunnel.com
```

**URL publique :** `https://abc123.cfargotunnel.com`

##### Étape 7 : Configurer pour Démarrage Automatique (10 minutes)

**Linux (systemd) :**

```bash
# Installer comme service
sudo cloudflared service install

# Démarrer
sudo systemctl start cloudflared

# Activer au démarrage
sudo systemctl enable cloudflared
```

**Vérifier :**
```bash
sudo systemctl status cloudflared
```

Le reste des étapes (configurer pom.xml, GitHub Actions) est identique à Ngrok.

---

## 6. Tableau Comparatif

| Critère | SonarCloud | Self-Hosted Runner | Serveur Cloud SonarQube | Tunnel Ngrok/Cloudflare |
|---------|------------|--------------------|-----------------------|------------------------|
| **Coût** | 0€ (gratuit pour projets publics) | 4-12€/mois (électricité) | 65-280€/an (VPS) | 0-10€/an |
| **Temps de Setup** | 10-15 min | 1h30-2h | 3-5h | 30 min - 1h |
| **Complexité** | Facile | Moyen-Difficile | Difficile | Facile-Moyen |
| **Maintenance** | 0 min/mois | 1-2h/mois | 3-4h/mois | ~40 min/mois |
| **Disponibilité** | 99.9% (SLA) | Dépend de votre PC (60-90%) | 95-99% (dépend VPS) | Dépend de votre PC (60-90%) |
| **Performance** | Excellente | Très bonne (local) | Bonne | Moyenne (latence tunnel) |
| **Sécurité** | Excellente (managed) | Moyenne (risque si compromise) | Bonne (si bien configuré) | Faible (exposition publique) |
| **Scalabilité** | Illimitée | Limitée (1 runner) | Moyenne (selon VPS) | Limitée (1 tunnel) |
| **Données privées** | Non (cloud Sonar) | Oui (local) | Oui (votre serveur) | Oui (local, mais transit tunnel) |
| **Intégration GitHub** | Native (PR checks) | Bonne | Bonne | Bonne |
| **Historique analyses** | Illimité | Selon espace disque local | Selon espace disque VPS | Selon espace disque local |
| **Support** | Officiel Sonar | Communauté GitHub | Auto-géré | Communauté Ngrok/CF |
| **Professionnalisme** | Très professionnel | Peu professionnel | Professionnel | Hacky (pas recommandé prod) |
| **Learning curve** | Très faible | Moyenne | Élevée | Faible |
| **URL publique** | Oui (sonarcloud.io) | Non | Oui (votre domaine) | Oui (dynamique/fixe selon plan) |
| **Adapté projet école** | ✅ OUI | ⚠️ Non recommandé | ❌ Non (trop cher) | ⚠️ Temporaire seulement |

### Scores Globaux (sur 10)

| Solution | Facilité | Coût | Fiabilité | Pro-ness | **TOTAL** |
|----------|----------|------|-----------|----------|-----------|
| **SonarCloud** | 10/10 | 10/10 | 10/10 | 10/10 | **40/40** |
| Self-Hosted Runner | 5/10 | 7/10 | 6/10 | 5/10 | **23/40** |
| Serveur Cloud SonarQube | 4/10 | 4/10 | 8/10 | 9/10 | **25/40** |
| Tunnel Ngrok/Cloudflare | 7/10 | 9/10 | 5/10 | 3/10 | **24/40** |

---

## 7. Recommandation Finale

### Pour un Projet École Zone01 : SONARCLOUD

#### Justifications Détaillées

##### 1. Alignement avec le Contexte Éducatif

**Temps limité :**
- **SonarCloud** : 15 minutes de setup
- **Alternatives** : 1h30 à 5h de setup
- **Impact** : 2-5 heures économisées = plus de temps pour coder

**Budget étudiant :**
- **SonarCloud** : 0€
- **Serveur Cloud** : 65-280€/an
- **Conclusion** : Économie de 65-280€

**Focus sur l'apprentissage :**
- SonarCloud permet de se concentrer sur :
  - Qualité du code (métriques, best practices)
  - CI/CD (workflows GitHub Actions)
  - Tests automatisés
- Pas de temps perdu sur :
  - Administration système (SSH, firewall, Nginx)
  - Debugging d'infrastructure (pourquoi le serveur ne répond pas ?)
  - Maintenance (updates, monitoring)

##### 2. Expérience Professionnelle Réaliste

**Utilisé en entreprise :**
- 40%+ des entreprises utilisent SonarCloud (ou SonarQube cloud-hosted)
- Compétence valorisée sur le CV : "CI/CD with SonarCloud integration"
- Expérience transférable (SonarCloud ≈ SonarQube Enterprise)

**Comparaison :**
- Self-hosted runner : Aucune entreprise ne fait ça (anti-pattern)
- Serveur Cloud SonarQube : Oui, mais c'est le job d'un DevOps, pas d'un dev junior
- Tunnel Ngrok : Non utilisé en production (solution de test uniquement)

##### 3. Avantages Techniques Décisifs

**Intégration GitHub native :**
```
Pull Request créée
  ↓
SonarCloud analyse automatiquement
  ↓
Résultats affichés dans la PR (GitHub Checks)
  ↓
Quality Gate FAILED → Merge bloqué
  ↓
Développeur corrige le code
  ↓
Nouveau commit → Nouvelle analyse
  ↓
Quality Gate PASSED → Merge autorisé
```

**Autres solutions :** Résultats visibles uniquement en allant sur SonarQube

**Dashboard interactif :**
- Métriques visuelles (coverage, bugs, code smells)
- Graphiques d'évolution dans le temps
- Comparaison entre branches
- Détection de duplications cross-projects

**Performance :**
- Scan plus rapide que self-hosted (infrastructure optimisée)
- Pas de latence tunnel (Ngrok ajoute ~100ms)
- Pas de dépendance à votre connexion Internet locale

##### 4. Maintenance Zéro

**SonarCloud :**
- Mises à jour automatiques (toujours la dernière version)
- Backups automatiques (données sauvegardées)
- Monitoring 24/7 (équipe Sonar surveille l'uptime)
- Support officiel (documentation + forum)

**Autres solutions :**
- Self-hosted runner : Maintenance régulière (updates, monitoring)
- Serveur Cloud : 3-4h/mois de maintenance
- Tunnel Ngrok : Redémarrer si crash, mettre à jour URL

**Temps économisé sur 3 mois de projet :**
- SonarCloud : 0h de maintenance
- Alternatives : 9-36h de maintenance

**Ce temps peut être investi dans :**
- Développer de nouvelles features
- Écrire des tests
- Améliorer la documentation
- Apprendre de nouvelles technos

##### 5. Scalabilité et Collaboration

**Projet en équipe :**
- Tous les membres ont accès au dashboard SonarCloud
- Notifications par email (nouveaux bugs détectés)
- Commentaires sur les issues de code
- Historique partagé

**Alternatives :**
- Self-hosted runner : Un seul membre a le runner
- Serveur Cloud : OK pour la collaboration (mais coûteux)
- Tunnel Ngrok : Dépend de la machine d'un seul membre

##### 6. Portfolio et Visibilité

**Badges dans README.md :**
```markdown
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=key&metric=alert_status)](https://sonarcloud.io/dashboard?id=key)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=key&metric=coverage)](https://sonarcloud.io/dashboard?id=key)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=key&metric=bugs)](https://sonarcloud.io/dashboard?id=key)
```

**Résultat visuel :**

![Quality Gate Passed](https://img.shields.io/badge/Quality%20Gate-passed-brightgreen)
![Coverage 75%](https://img.shields.io/badge/Coverage-75%25-green)
![0 Bugs](https://img.shields.io/badge/Bugs-0-brightgreen)

**Impact :**
- Recruteurs voient immédiatement la qualité du code
- Différenciation par rapport aux autres candidats
- Démontre une approche professionnelle

### Quand Choisir une Autre Solution ?

#### Choisir Self-Hosted Runner Si :
- Vous avez un serveur Linux 24/7 déjà disponible (Raspberry Pi, NAS)
- Objectif explicite : apprendre l'administration système
- Projet avec contraintes de confidentialité extrêmes

#### Choisir Serveur Cloud SonarQube Si :
- Budget disponible (65-280€/an)
- Équipe de 5+ personnes (coût réparti)
- Projet long terme (6+ mois)
- Formation DevOps/SysAdmin (pertinence pédagogique)

#### Choisir Tunnel Ngrok/Cloudflare Si :
- Solution temporaire (1-2 semaines) avant migration SonarCloud
- Tests rapides de configuration SonarQube
- Demo/POC (pas pour production)

### Migration Facile

**Si vous commencez avec Ngrok/self-hosted et voulez migrer vers SonarCloud :**

```bash
# 1. Créer les projets sur SonarCloud (5 min)
# 2. Mettre à jour les pom.xml (5 min)
#    Remplacer sonar.host.url par https://sonarcloud.io
#    Ajouter sonar.organization
# 3. Créer le token SonarCloud (2 min)
# 4. Mettre à jour le GitHub Secret SONAR_TOKEN (1 min)
# 5. Push → Build automatique
```

**Temps total de migration : 15 minutes**

**Pas de perte de données : Historique SonarCloud commence à la migration**

---

## Conclusion

**Pour un projet école Zone01 :**

1. **Recommandé :** SonarCloud (0€, 15 min setup, maintenance zéro)
2. **Acceptable temporairement :** Tunnel Ngrok (tester rapidement)
3. **Déconseillé :** Self-hosted runner (trop de maintenance)
4. **Non adapté :** Serveur Cloud (coût trop élevé pour projet école)

**Action recommandée : Suivre le guide de setup SonarCloud (Solution 1)**

**Prochains documents :**
- `docs/03-GITHUB-SECRETS-SETUP.md` : Configuration des secrets GitHub
- `docs/04-QUALITY-GATES-SETUP.md` : Configuration des Quality Gates SonarQube/SonarCloud

---

**Document créé le** : 2025-12-15
**Auteur** : Documentation CI/CD Zone01
**Version** : 1.0
**Statut** : Complet