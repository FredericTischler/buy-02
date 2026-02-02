# Guide de Préparation pour l'Audit CI/CD - SafeZone E-Commerce

## Table des Matières
1. [Vue d'Ensemble du Projet](#vue-densemble-du-projet)
2. [Infrastructure CI/CD Implémentée](#infrastructure-cicd-implémentée)
3. [Démonstration Pratique](#démonstration-pratique)
4. [Questions/Réponses Attendues](#questionsréponses-attendues)
5. [Preuves et Evidences](#preuves-et-evidences)
6. [Points Techniques à Maîtriser](#points-techniques-à-maîtriser)
7. [Checklist Pré-Audit](#checklist-pré-audit)

---

## 1. Vue d'Ensemble du Projet

### Contexte

**Projet** : SafeZone - Plateforme E-Commerce Microservices
**Formation** : Zone01 Normandie
**Objectif** : Intégration CI/CD avec analyse de code automatisée

### Architecture Technique

```
┌─────────────────────────────────────────────────────────────────┐
│                    GITHUB REPOSITORY                             │
│                         safe-zone                                │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       │ Push / Pull Request
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│                   GITHUB ACTIONS                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Backend    │  │   Frontend   │  │     Full     │          │
│  │   Workflow   │  │   Workflow   │  │   Workflow   │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                   │
│         └──────────────────┼──────────────────┘                  │
│                            │                                      │
└────────────────────────────┼──────────────────────────────────┬─┘
                             │                                   │
                             ↓                                   ↓
                  ┌──────────────────────┐           ┌─────────────────┐
                  │    SONARCLOUD        │           │  Quality Gate   │
                  │  Code Analysis       │──────────→│  PASS / FAIL    │
                  │  - Bugs              │           └────────┬────────┘
                  │  - Vulnerabilities   │                    │
                  │  - Code Smells       │                    │
                  │  - Coverage          │                    ↓
                  └──────────────────────┘         ┌────────────────────┐
                                                   │ Merge Authorized   │
                                                   │ or Blocked         │
                                                   └────────────────────┘
```

### Stack Technique

**Backend** :
- 3 microservices : user-service, product-service, media-service
- Java 17 + Spring Boot 3.2.0
- MongoDB 7.0
- Apache Kafka 7.5.0
- JaCoCo pour couverture de code

**Frontend** :
- Angular 20.3.0
- TypeScript 5.9.2
- Karma + Jasmine pour tests

**CI/CD** :
- GitHub Actions (3 workflows)
- SonarCloud pour analyse de code
- Quality Gates configurés

---

## 2. Infrastructure CI/CD Implémentée

### Workflows GitHub Actions

#### Workflow 1 : `sonarqube-backend.yml`
**Déclenchement** :
- Push sur `main` si modification dans `backend/**`
- Pull Request vers `main` si modification dans `backend/**`

**Fonctionnalités** :
- Analyse en parallèle des 3 microservices (matrix strategy)
- Build Maven + tests unitaires
- Génération rapports JaCoCo
- Upload vers SonarCloud
- Vérification Quality Gate
- Commentaire automatique sur PR avec résultats
- **Bloque le merge si Quality Gate échoue**

**Temps d'exécution estimé** : 5-8 minutes

#### Workflow 2 : `sonarqube-frontend.yml`
**Déclenchement** :
- Push sur `main` si modification dans `frontend/**`
- Pull Request vers `main` si modification dans `frontend/**`

**Fonctionnalités** :
- Installation dépendances npm
- Linting ESLint
- Tests Karma + ChromeHeadless
- Build production Angular
- Analyse SonarCloud TypeScript
- Upload coverage reports
- Vérification Quality Gate
- **Bloque le merge si Quality Gate échoue**

**Temps d'exécution estimé** : 4-6 minutes

#### Workflow 3 : `sonarqube-full.yml`
**Déclenchement** :
- Push sur `main` (tous chemins)
- Pull Request vers `main` (tous chemins)

**Fonctionnalités** :
- Exécution complète backend + frontend
- Job de summary qui agrège tous les résultats
- Tableau récapitulatif dans les commentaires PR
- Mise à jour du commentaire (pas de spam de commentaires)
- **Bloque si au moins un service échoue**

**Temps d'exécution estimé** : 6-10 minutes

### Configuration SonarCloud

**Organization** : `zone01-ecommerce`

**Projets créés** :
1. `ecommerce-user-service` - Microservice utilisateurs
2. `ecommerce-product-service` - Microservice produits
3. `ecommerce-media-service` - Microservice médias
4. `ecommerce-frontend` - Application Angular

**Quality Gates Configurés** :
- Coverage sur nouveau code ≥ 70%
- Bugs sur nouveau code = 0
- Vulnérabilités = 0
- Code Smells < 5 sur nouveau code
- Duplication < 3%

### Secrets GitHub

**Secret configuré** :
- `SONAR_TOKEN` : Token d'authentification SonarCloud
  - Format : `squ_XXXXXXXXXXXXXXXXXXXX`
  - Permissions : Analyze projects
  - Chiffré AES-256 par GitHub
  - Jamais exposé dans les logs

### Protection des Branches

**Branche `main` protégée avec** :
- ✅ Require pull request reviews (1 approbation)
- ✅ Require status checks to pass before merging
- ✅ Require branches to be up to date before merging
- ✅ Require conversation resolution before merging
- ❌ Do not allow bypassing rules
- ❌ Do not allow force pushes
- ❌ Do not allow deletions

**Status checks obligatoires** :
- Analyze User Service
- Analyze Product Service
- Analyze Media Service
- Analyze Frontend (Angular)
- Analysis Summary

---

## 3. Démonstration Pratique

### Scénario 1 : Quality Gate PASSED ✅

**Étapes** :

1. **Créer une branche de test**
   ```bash
   git checkout -b demo/quality-gate-passed
   ```

2. **Modifier un fichier (ajout simple)**
   ```bash
   # Exemple : Ajouter un commentaire dans README.md
   echo "# CI/CD Demo - Test Quality Gate" >> README.md
   ```

3. **Commit et push**
   ```bash
   git add README.md
   git commit -m "demo: test quality gate passed"
   git push origin demo/quality-gate-passed
   ```

4. **Créer Pull Request sur GitHub**
   - Aller sur GitHub → Pull Requests → New Pull Request
   - Sélectionner branche `demo/quality-gate-passed` → `main`
   - Titre : "Demo: Quality Gate Passed Test"
   - Create Pull Request

5. **Observer l'exécution**
   - GitHub Actions se déclenche automatiquement
   - Onglet "Checks" montre les workflows en cours
   - Après 2-3 minutes : tous les checks sont verts ✅

6. **Résultat attendu**
   - Commentaire automatique posté avec résultats SonarCloud
   - Quality Gate : ✅ PASSED
   - Bouton "Merge" activé et utilisable

### Scénario 2 : Quality Gate FAILED ❌

**Étapes** :

1. **Créer une branche de test**
   ```bash
   git checkout -b demo/quality-gate-failed
   ```

2. **Introduire volontairement un bug**
   ```java
   // backend/user-service/src/main/java/com/ecommerce/user/DemoClass.java
   package com.ecommerce.user;

   public class DemoClass {
       public String getPotentialNullPointer(User user) {
           // Bug volontaire : NullPointerException potentiel
           return user.getEmail().toLowerCase();  // SonarCloud détectera le bug
       }
   }
   ```

3. **Commit et push**
   ```bash
   git add backend/user-service/src/main/java/com/ecommerce/user/DemoClass.java
   git commit -m "demo: introduce bug for quality gate test"
   git push origin demo/quality-gate-failed
   ```

4. **Créer Pull Request**

5. **Observer l'exécution**
   - Workflow s'exécute
   - SonarCloud détecte le bug
   - Quality Gate échoue
   - Commentaire PR avec détails de l'échec

6. **Résultat attendu**
   - Quality Gate : ❌ FAILED
   - Message d'erreur : "1 Bug found on new code"
   - **Bouton "Merge" désactivé** (si protection branches activée)
   - Lien vers SonarCloud pour voir les détails

7. **Correction du bug**
   ```java
   public String getPotentialNullPointer(User user) {
       // Correction : vérification null
       if (user == null || user.getEmail() == null) {
           return "";
       }
       return user.getEmail().toLowerCase();
   }
   ```

8. **Push correction**
   ```bash
   git add backend/user-service/src/main/java/com/ecommerce/user/DemoClass.java
   git commit -m "fix: handle null pointer in DemoClass"
   git push
   ```

9. **Workflow re-run automatiquement**
   - Quality Gate : ✅ PASSED
   - Merge autorisé

---

## 4. Questions/Réponses Attendues

### Q1 : Expliquez votre pipeline CI/CD complet

**Réponse** :
```
Notre pipeline CI/CD est entièrement automatisé via GitHub Actions et SonarCloud.

Flux de travail :
1. Développeur crée une branche feature et commit du code
2. Push vers GitHub déclenche le workflow PR
3. GitHub Actions exécute 3 jobs en parallèle :
   - Build Maven + tests pour chaque microservice backend
   - Build npm + tests pour le frontend Angular
   - Génération des rapports de couverture (JaCoCo/Karma)
4. Résultats envoyés à SonarCloud pour analyse statique
5. SonarCloud évalue :
   - Bugs, vulnerabilities, code smells
   - Couverture de code (objectif 70%)
   - Duplication de code
6. Quality Gate calcule un statut : PASSED ou FAILED
7. GitHub reçoit le statut et :
   - Poste un commentaire détaillé dans la PR
   - Active/désactive le bouton Merge
8. Si PASSED : merge autorisé
   Si FAILED : développeur doit corriger et re-push

Tout est automatique, aucune intervention manuelle requise.
```

### Q2 : Qu'est-ce qu'un Quality Gate et pourquoi est-ce important ?

**Réponse** :
```
Un Quality Gate est un ensemble de conditions de qualité que le code doit
respecter pour être considéré comme acceptable pour la production.

Notre Quality Gate vérifie :
- Couverture de code ≥ 70% sur nouveau code
- 0 bugs dans le nouveau code
- 0 vulnérabilités de sécurité
- < 5 code smells sur nouveau code
- < 3% de duplication

Importance :
1. Prévention de la dette technique : Empêche l'accumulation de code de
   mauvaise qualité qui sera coûteux à corriger plus tard.

2. Sécurité : Détecte les vulnérabilités avant qu'elles n'atteignent la
   production.

3. Maintenabilité : Assure que le code reste compréhensible et modifiable.

4. Standards uniformes : Tous les développeurs doivent respecter les mêmes
   critères de qualité.

5. Feedback immédiat : Le développeur sait en quelques minutes si son code
   est acceptable, plutôt que d'attendre une revue manuelle.

Le Quality Gate BLOQUE physiquement le merge si les conditions ne sont pas
respectées, ce qui force la correction des problèmes immédiatement.
```

### Q3 : Comment gérez-vous les secrets de façon sécurisée ?

**Réponse** :
```
Nous utilisons GitHub Secrets pour gérer nos credentials sensibles.

Processus :
1. Création du secret :
   - GitHub → Settings → Secrets and variables → Actions
   - Créer secret SONAR_TOKEN avec le token SonarCloud

2. Chiffrement :
   - GitHub chiffre automatiquement avec AES-256
   - Stocké de manière sécurisée dans la base GitHub

3. Utilisation dans les workflows :
   env:
     SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

4. Protection :
   - Jamais affiché dans les logs (masquage automatique)
   - Impossible de récupérer la valeur via API
   - Accessible uniquement aux workflows du repo

5. Rotation :
   - Token régénéré tous les 90 jours (bonne pratique)
   - Ancien token révoqué sur SonarCloud
   - Nouveau token mis à jour dans GitHub

Bonnes pratiques appliquées :
- Jamais de credentials en clair dans le code
- Principe du moindre privilège (token avec permissions minimales)
- Documentation de tous les secrets requis
- Rotation régulière des credentials
```

### Q4 : Quelle est la différence entre SonarQube local et SonarCloud ?

**Réponse** :
```
SonarQube Local :
- Serveur auto-hébergé (Docker dans notre cas)
- Tourne sur localhost:9000
- Nécessite configuration serveur + base de données
- Idéal pour développement en local
- Problème : GitHub Actions ne peut pas y accéder (localhost inaccessible)

SonarCloud :
- Service SaaS hébergé par SonarSource
- URL : https://sonarcloud.io
- Pas de configuration serveur nécessaire
- Gratuit pour projets open source
- Intégration native avec GitHub
- Accessible depuis GitHub Actions (internet public)

Notre choix : SonarCloud
Raisons :
1. Projet école : pas de budget pour infrastructure
2. GitHub Actions nécessite un service accessible publiquement
3. Setup simple : 10 minutes vs plusieurs heures
4. Maintenance : zéro (géré par SonarSource)
5. Intégration GitHub : commentaires PR automatiques

Configuration hybrid :
- Développement local : SonarQube sur localhost (optionnel)
- CI/CD : SonarCloud pour automatisation
- Les fichiers pom.xml/sonar-project.properties supportent les deux
```

### Q5 : Montrez-moi que le Quality Gate bloque réellement les merges

**Réponse** :
```
[Démonstration pratique - Scénario 2 ci-dessus]

1. Je crée une PR avec un bug volontaire
2. [Montrer GitHub Actions en cours d'exécution]
3. [Montrer SonarCloud détectant le bug]
4. [Montrer commentaire PR : Quality Gate FAILED]
5. [Montrer bouton Merge désactivé + message d'erreur]
6. Je corrige le bug et push
7. [Montrer re-run automatique]
8. [Montrer Quality Gate PASSED]
9. [Montrer bouton Merge activé]

Preuve technique :
- Protection de branche configurée dans Settings
- Status check "Quality Gate" marqué comme obligatoire
- GitHub refuse le merge si check échoue
- Aucun bypass possible (sauf admin override désactivé)
```

---

## 5. Preuves et Evidences

### Checklist des Preuves à Préparer

- [ ] **Screenshots GitHub Actions**
  - [ ] Workflow run successful (tous checks verts)
  - [ ] Workflow run failed (Quality Gate rouge)
  - [ ] Liste des workflows configurés

- [ ] **Screenshots SonarCloud**
  - [ ] Dashboard organisation `zone01-ecommerce`
  - [ ] 4 projets visibles
  - [ ] Quality Gate configuration
  - [ ] Exemple d'analyse avec métriques (coverage, bugs, etc.)

- [ ] **Screenshots GitHub**
  - [ ] Branch protection rules sur `main`
  - [ ] Secrets configurés (noms uniquement, pas les valeurs)
  - [ ] Pull Request avec commentaires automatiques
  - [ ] Status checks obligatoires cochés

- [ ] **Screenshots Pull Request**
  - [ ] PR avec Quality Gate PASSED (merge autorisé)
  - [ ] PR avec Quality Gate FAILED (merge bloqué)
  - [ ] Commentaire automatique avec tableau récapitulatif

- [ ] **Code Source**
  - [ ] Workflows YAML (`.github/workflows/`)
  - [ ] Configuration pom.xml avec SonarCloud
  - [ ] Configuration sonar-project.properties

- [ ] **Documentation**
  - [ ] README.md avec badges SonarCloud
  - [ ] Documentation complète dans `/docs`
  - [ ] Guide de contribution avec processus PR

### Liens Utiles pour l'Audit

```
GitHub Repository :
https://github.com/VOTRE-USERNAME/safe-zone

GitHub Actions :
https://github.com/VOTRE-USERNAME/safe-zone/actions

SonarCloud Organization :
https://sonarcloud.io/organizations/zone01-ecommerce

SonarCloud Projects :
- https://sonarcloud.io/project/overview?id=ecommerce-user-service
- https://sonarcloud.io/project/overview?id=ecommerce-product-service
- https://sonarcloud.io/project/overview?id=ecommerce-media-service
- https://sonarcloud.io/project/overview?id=ecommerce-frontend

Branch Protection :
https://github.com/VOTRE-USERNAME/safe-zone/settings/branches
```

---

## 6. Points Techniques à Maîtriser

### Concepts CI/CD

1. **Continuous Integration (CI)**
   - Définition : Intégration automatique et fréquente du code
   - Notre implémentation : GitHub Actions déclenché à chaque push/PR
   - Bénéfices : Détection rapide des bugs, feedback immédiat

2. **Continuous Deployment (CD)**
   - Définition : Déploiement automatique en production
   - Notre implémentation : Pas encore (hors scope projet)
   - Évolution possible : Déploiement automatique sur merge vers `main`

3. **Quality Gate**
   - Définition : Seuil de qualité à respecter
   - Implémentation : SonarCloud avec conditions configurées
   - Rôle : Bloquer le code de mauvaise qualité

4. **Static Code Analysis**
   - Définition : Analyse du code sans l'exécuter
   - Outil : SonarCloud
   - Détecte : Bugs, vulnerabilities, code smells, complexité

5. **Code Coverage**
   - Définition : Pourcentage de code testé
   - Mesure : JaCoCo (backend) + Karma (frontend)
   - Objectif : 70% minimum sur nouveau code

### Technologies Maîtrisées

1. **GitHub Actions**
   - YAML syntax
   - Jobs, steps, actions
   - Matrix strategy (parallélisation)
   - Secrets management
   - Path filtering

2. **Maven**
   - POM.xml configuration
   - SonarQube Maven plugin
   - JaCoCo plugin
   - Profils et propriétés

3. **SonarCloud**
   - Organization et projects
   - Quality Gates configuration
   - API authentication
   - GitHub integration

4. **Docker** (bonus)
   - Docker Compose pour SonarQube local
   - Containerisation des microservices

---

## 7. Checklist Pré-Audit

### 48h Avant l'Audit

- [ ] **Vérifier que SonarCloud fonctionne**
  - [ ] Aller sur https://sonarcloud.io
  - [ ] Vérifier que les 4 projets existent
  - [ ] Vérifier qu'il y a des analyses récentes (< 7 jours)
  - [ ] Vérifier que les Quality Gates sont configurés

- [ ] **Tester les Workflows GitHub Actions**
  - [ ] Créer une branche de test
  - [ ] Push un petit changement
  - [ ] Vérifier que le workflow se déclenche
  - [ ] Vérifier que l'analyse SonarCloud fonctionne
  - [ ] Supprimer la branche de test

- [ ] **Vérifier Secrets GitHub**
  - [ ] SONAR_TOKEN existe et est valide
  - [ ] Workflow de test (test-ci-setup.yml) passe en vert

- [ ] **Protection des Branches**
  - [ ] Branche `main` protégée
  - [ ] Status checks configurés
  - [ ] Tester qu'on ne peut pas push direct sur main

- [ ] **Documentation**
  - [ ] Lire tout le dossier `/docs`
  - [ ] S'assurer de comprendre chaque workflow
  - [ ] Relire ce guide de préparation

### 24h Avant l'Audit

- [ ] **Préparer Démonstration Live**
  - [ ] Identifier quel fichier modifier pour démo (ex: README)
  - [ ] Préparer bug volontaire pour démo Quality Gate FAILED
  - [ ] Tester la démo une fois complètement

- [ ] **Préparer Screenshots**
  - [ ] Capturer tous les écrans listés dans "Preuves"
  - [ ] Organiser dans un dossier `audit-screenshots/`
  - [ ] Vérifier qu'ils sont clairs et lisibles

- [ ] **Réviser Questions/Réponses**
  - [ ] Relire section 4 de ce document
  - [ ] Préparer des réponses personnalisées
  - [ ] S'entraîner à répondre à voix haute

### Le Jour de l'Audit

- [ ] **Vérification Finale**
  - [ ] Tous les workflows passent au vert
  - [ ] SonarCloud accessible
  - [ ] GitHub accessible
  - [ ] Connexion internet stable

- [ ] **Matériel Préparé**
  - [ ] Navigateur avec onglets ouverts :
    - [ ] GitHub repo
    - [ ] GitHub Actions
    - [ ] SonarCloud dashboard
    - [ ] Documentation
  - [ ] Terminal prêt pour démo Git
  - [ ] Screenshots dans un dossier accessible

- [ ] **Mental Préparation**
  - [ ] Respirer profondément
  - [ ] Relire les concepts clés
  - [ ] Confiance : vous avez fait un excellent travail !

---

## Annexe : Script de Test Rapide

```bash
#!/bin/bash
# Script de test rapide pré-audit
# À exécuter 1h avant l'audit

echo "=========================================="
echo "  TEST PRÉ-AUDIT - SafeZone CI/CD"
echo "=========================================="

# Test 1 : Vérifier secrets GitHub
echo ""
echo "Test 1 : Vérification secrets GitHub"
gh secret list --repo VOTRE-USERNAME/safe-zone
if [ $? -eq 0 ]; then
  echo "✅ Secrets accessibles"
else
  echo "❌ Erreur accès secrets"
fi

# Test 2 : Vérifier dernière exécution workflow
echo ""
echo "Test 2 : Dernière exécution workflow"
gh run list --repo VOTRE-USERNAME/safe-zone --limit 1

# Test 3 : Vérifier SonarCloud
echo ""
echo "Test 3 : Test connexion SonarCloud"
curl -s -u "$SONAR_TOKEN:" https://sonarcloud.io/api/authentication/validate
if [ $? -eq 0 ]; then
  echo "✅ SonarCloud accessible"
else
  echo "❌ Erreur connexion SonarCloud"
fi

# Test 4 : Vérifier protection branche
echo ""
echo "Test 4 : Protection branche main"
gh api repos/VOTRE-USERNAME/safe-zone/branches/main/protection \
  --jq '.required_status_checks.contexts'

echo ""
echo "=========================================="
echo "  FIN DES TESTS"
echo "=========================================="
```

---

**Document créé le** : 2025-12-16
**Auteur** : Documentation CI/CD SafeZone
**Version** : 1.0
**Statut** : Prêt pour audit