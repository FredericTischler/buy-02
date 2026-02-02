# 05 - Configuration de la Protection des Branches GitHub

## Table des Matières
1. [Introduction](#introduction)
2. [Pourquoi Protéger les Branches](#pourquoi-protéger-les-branches)
3. [Configuration pour la Branche Main](#configuration-pour-la-branche-main)
4. [Configuration pour la Branche Develop](#configuration-pour-la-branche-develop)
5. [Stratégie de Branches (Git Flow Adapté)](#stratégie-de-branches)
6. [Configuration via Interface GitHub](#configuration-via-interface-github)
7. [Automatisation avec GitHub CLI](#automatisation-avec-github-cli)
8. [Tests de Validation](#tests-de-validation)
9. [Troubleshooting](#troubleshooting)
10. [Bonnes Pratiques](#bonnes-pratiques)

---

## Introduction

La protection des branches est un mécanisme essentiel pour maintenir la qualité et la stabilité du code dans un environnement de développement collaboratif. Ce guide vous explique comment configurer les règles de protection pour votre dépôt GitHub afin d'assurer que tout le code fusionné respecte vos standards de qualité.

**Objectifs de ce guide :**
- Configurer la protection de la branche `main` pour éviter les commits directs
- Exiger des Pull Requests approuvées avant fusion
- Intégrer les vérifications SonarCloud obligatoires
- Automatiser la configuration avec GitHub CLI
- Valider la configuration avec des tests pratiques

---

## Pourquoi Protéger les Branches

### Avantages de la Protection des Branches

#### 1. **Prévention des Erreurs Humaines**
Sans protection, n'importe quel développeur peut accidentellement pousser du code non testé directement sur `main`, ce qui peut :
- Casser la production
- Introduire des bugs critiques
- Ignorer les processus de revue de code

#### 2. **Application Systématique des Standards de Qualité**
Avec les protections activées :
- Toutes les modifications passent par une Pull Request
- Les vérifications automatiques (CI/CD, SonarCloud) doivent réussir
- Au moins un autre développeur doit approuver les changements
- Le code est systématiquement analysé avant fusion

#### 3. **Traçabilité et Historique**
- Chaque modification est documentée dans une PR
- Les discussions et décisions sont préservées
- L'historique Git reste propre et compréhensible

#### 4. **Conformité et Audit**
- Démonstration du respect des processus de développement
- Traçabilité pour les audits de sécurité et de qualité
- Documentation automatique des changements

#### 5. **Collaboration Améliorée**
- Facilite les revues de code
- Encourage les discussions techniques
- Partage des connaissances entre développeurs

---

## Configuration pour la Branche Main

La branche `main` est votre branche de production. Elle doit être protégée avec les règles les plus strictes.

### Règles de Protection Recommandées

#### 1. **Require a Pull Request Before Merging**
**Configuration :** ✅ Activé

Cette règle force tous les changements à passer par une Pull Request. Aucun commit direct n'est autorisé.

**Sous-options à configurer :**

##### a) **Require Approvals**
- **Valeur recommandée :** `1` approbation minimum
- **Pour les équipes plus grandes :** `2` approbations

**Pourquoi ?**
- Garantit qu'au moins un autre développeur a revu le code
- Détecte les erreurs que l'auteur a pu manquer
- Partage les connaissances dans l'équipe

**Configuration :**
```
☑️ Require approvals: 1
```

##### b) **Dismiss Stale Pull Request Approvals When New Commits are Pushed**
- **Valeur recommandée :** ✅ Activé

**Pourquoi ?**
- Si des changements sont ajoutés après approbation, la revue doit être refaite
- Évite qu'un développeur n'ajoute du code non revu après approbation
- Maintient l'intégrité du processus de revue

**Configuration :**
```
☑️ Dismiss stale pull request approvals when new commits are pushed
```

##### c) **Require Review from Code Owners** (Optionnel)
- **Valeur recommandée :** ⬜ Désactivé (pour les petites équipes)
- **Pour les projets critiques :** ✅ Activé

**Pourquoi l'activer ?**
- Les experts techniques doivent approuver les changements dans leurs domaines
- Garantit que les modifications critiques sont validées par les bonnes personnes

**Configuration :**
```
⬜ Require review from Code Owners (optionnel)
```

#### 2. **Require Status Checks to Pass Before Merging**
**Configuration :** ✅ Activé

Cette règle est **cruciale** pour l'intégration SonarCloud. Elle empêche la fusion si les vérifications automatiques échouent.

**Sous-options à configurer :**

##### a) **Require Branches to be Up to Date Before Merging**
- **Valeur recommandée :** ✅ Activé

**Pourquoi ?**
- Évite les conflits de fusion
- Garantit que les tests ont été exécutés sur la version la plus récente de `main`
- Prévient les régressions dues à des changements concurrents

**Configuration :**
```
☑️ Require branches to be up to date before merging
```

##### b) **Status Checks that are Required**

Vous devez sélectionner **tous les status checks SonarCloud** comme obligatoires. Voici la liste exacte basée sur vos workflows :

**Pour le workflow `sonarqube-full.yml` (analyse complète) :**
```
✅ Backend - user-service
✅ Backend - product-service
✅ Backend - media-service
✅ Frontend (Angular)
✅ Analysis Summary
```

**Pour le workflow `sonarqube-backend.yml` (backend seulement) :**
```
✅ Analyze User Service
✅ Analyze Product Service
✅ Analyze Media Service
```

**Pour le workflow `sonarqube-frontend.yml` (frontend seulement) :**
```
✅ Analyze Frontend (Angular)
```

**Important :** GitHub affichera ces checks après la première exécution des workflows. Vous devez créer une PR de test pour les voir apparaître dans la liste des status checks disponibles.

**Configuration visuelle :**
```
Search for status checks in the last week for this repository:

☑️ Analyze Frontend (Angular)
☑️ Analyze User Service
☑️ Analyze Product Service
☑️ Analyze Media Service
☑️ Backend - user-service
☑️ Backend - product-service
☑️ Backend - media-service
☑️ Frontend (Angular)
☑️ Analysis Summary
```

#### 3. **Require Conversation Resolution Before Merging**
**Configuration :** ✅ Activé

**Pourquoi ?**
- Force la résolution de tous les commentaires et suggestions
- Garantit que toutes les discussions sont closes
- Évite d'oublier des améliorations demandées

**Configuration :**
```
☑️ Require conversation resolution before merging
```

#### 4. **Require Signed Commits** (Optionnel)
**Configuration :** ⬜ Désactivé (pour la plupart des projets)

**Pourquoi le désactiver ?**
- Complexifie la configuration pour les débutants
- Nécessite une configuration GPG pour chaque développeur
- Recommandé uniquement pour les projets hautement sécurisés

**Configuration :**
```
⬜ Require signed commits (optionnel)
```

#### 5. **Require Linear History** (Optionnel)
**Configuration :** ⬜ Désactivé (recommandé)

**Pourquoi le désactiver ?**
- Permet les merge commits classiques
- Plus flexible pour les équipes débutantes
- Les squash merges sont généralement suffisants

**Configuration :**
```
⬜ Require linear history (optionnel)
```

#### 6. **Include Administrators**
**Configuration :** ⚠️ À décider selon le contexte

**Avantages si ACTIVÉ (✅) :**
- Même les administrateurs doivent suivre les règles
- Démontre l'engagement envers la qualité
- Évite les raccourcis en production
- Recommandé pour les environnements professionnels

**Inconvénients si ACTIVÉ (✅) :**
- Peut bloquer les corrections d'urgence
- Complique les hotfixes critiques
- Nécessite toujours une autre personne pour approuver

**Avantages si DÉSACTIVÉ (⬜) :**
- Permet les corrections d'urgence par les admins
- Flexibilité pour les situations critiques
- Utile pour les petites équipes

**Inconvénients si DÉSACTIVÉ (⬜) :**
- Les admins peuvent contourner les règles
- Risque d'incohérence dans l'application des standards

**Recommandation :**
- **Projets en production critiques :** ✅ Activé
- **Projets éducatifs/développement :** ⬜ Désactivé
- **Petites équipes (< 3 personnes) :** ⬜ Désactivé
- **Grandes équipes (> 5 personnes) :** ✅ Activé

**Configuration :**
```
☑️ Include administrators (recommandé pour production)
⬜ Include administrators (acceptable pour développement)
```

#### 7. **Do Not Allow Bypassing the Above Settings**
**Configuration :** ✅ Activé

**Pourquoi ?**
- Garantit que les règles ne peuvent pas être contournées temporairement
- Maintient la cohérence et l'intégrité du processus
- Évite les exceptions "juste cette fois"

**Configuration :**
```
☑️ Do not allow bypassing the above settings
```

#### 8. **Allow Force Pushes**
**Configuration :** ⬜ Désactivé (recommandé)

**Pourquoi désactiver ?**
- Les force pushes peuvent détruire l'historique
- Très dangereux sur la branche de production
- Peut causer des problèmes pour les autres développeurs

**Configuration :**
```
⬜ Allow force pushes (DANGER - ne pas activer)
```

#### 9. **Allow Deletions**
**Configuration :** ⬜ Désactivé (recommandé)

**Pourquoi désactiver ?**
- Empêche la suppression accidentelle de la branche `main`
- Protection critique pour les branches de production

**Configuration :**
```
⬜ Allow deletions (DANGER - ne pas activer)
```

### Résumé de la Configuration pour `main`

```
Branch Protection Rule: main

✅ Require a pull request before merging
   ✅ Require approvals: 1
   ✅ Dismiss stale pull request approvals when new commits are pushed
   ⬜ Require review from Code Owners (optionnel)
   ⬜ Require approval of the most recent reviewable push (optionnel)

✅ Require status checks to pass before merging
   ✅ Require branches to be up to date before merging
   Status checks that are required:
      ✅ Analyze Frontend (Angular)
      ✅ Analyze User Service
      ✅ Analyze Product Service
      ✅ Analyze Media Service
      ✅ Backend - user-service
      ✅ Backend - product-service
      ✅ Backend - media-service
      ✅ Frontend (Angular)
      ✅ Analysis Summary

✅ Require conversation resolution before merging
⬜ Require signed commits (optionnel)
⬜ Require linear history (optionnel)
✅ Include administrators (recommandé)
✅ Do not allow bypassing the above settings
⬜ Allow force pushes (DANGER)
⬜ Allow deletions (DANGER)
```

---

## Configuration pour la Branche Develop

Si vous utilisez une branche `develop` pour l'intégration continue, voici la configuration recommandée.

### Différences par rapport à `main`

La branche `develop` peut avoir des règles légèrement plus souples car elle sert à l'intégration et aux tests, pas à la production.

#### Configuration Recommandée pour `develop`

```
Branch Protection Rule: develop

✅ Require a pull request before merging
   ✅ Require approvals: 1
   ⬜ Dismiss stale pull request approvals when new commits are pushed (plus souple)
   ⬜ Require review from Code Owners

✅ Require status checks to pass before merging
   ✅ Require branches to be up to date before merging
   Status checks that are required:
      ✅ Backend - user-service
      ✅ Backend - product-service
      ✅ Backend - media-service
      ✅ Frontend (Angular)
      ⬜ Analysis Summary (optionnel pour develop)

⬜ Require conversation resolution before merging (plus flexible)
⬜ Require signed commits
⬜ Require linear history
⬜ Include administrators (permettre les corrections rapides)
✅ Do not allow bypassing the above settings
⬜ Allow force pushes (DANGER)
⬜ Allow deletions (DANGER)
```

### Pourquoi des Règles Plus Souples pour `develop` ?

1. **Itérations Rapides :** Les développeurs ont besoin de fusionner fréquemment
2. **Tests d'Intégration :** C'est l'endroit pour tester les intégrations avant `main`
3. **Corrections Rapides :** Les admins peuvent intervenir en cas de blocage
4. **Feedback Rapide :** Les conversations peuvent être résolues après fusion

**Important :** Même avec des règles plus souples, les quality gates SonarCloud restent obligatoires !

---

## Stratégie de Branches (Git Flow Adapté)

### Vue d'Ensemble de la Stratégie

Nous utilisons une version simplifiée de Git Flow adaptée aux projets modernes avec CI/CD.

```
┌─────────────────────────────────────────────────────────────┐
│                         BRANCHES                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  main (production)                                           │
│  ═══════════════════════════════════════════════════════    │
│       ↑                    ↑                    ↑            │
│       │ PR + Review        │ PR + Review        │            │
│       │ + Quality Gate     │ + Quality Gate     │            │
│       │                    │                    │            │
│  develop (intégration)                                       │
│  ───────────────────────────────────────────────────────    │
│       ↑           ↑           ↑           ↑                  │
│       │ PR        │ PR        │ PR        │ PR               │
│       │           │           │           │                  │
│  feature/login  feature/cart  bugfix/auth  hotfix/security  │
│  ──────────────  ────────────  ───────────  ──────────────  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Types de Branches

#### 1. **Branche `main`** (Production)
- **Protection :** Maximale
- **Objectif :** Code en production
- **Mise à jour :** Via PR depuis `develop` ou `hotfix/*`
- **Quality Gates :** Obligatoires (SonarCloud)
- **Approbations :** 1-2 reviewers minimum
- **Tests :** Tous les tests doivent passer
- **Déploiement :** Automatique vers production (optionnel)

**Commandes :**
```bash
# JAMAIS de commit direct sur main
git checkout main  # ❌ NE PAS FAIRE
git push origin main  # ❌ BLOQUÉ par protection

# Toujours via Pull Request
```

#### 2. **Branche `develop`** (Intégration)
- **Protection :** Modérée
- **Objectif :** Intégration des fonctionnalités
- **Mise à jour :** Via PR depuis `feature/*` ou `bugfix/*`
- **Quality Gates :** Obligatoires
- **Approbations :** 1 reviewer minimum
- **Tests :** Tous les tests doivent passer
- **Déploiement :** Automatique vers environnement de staging (optionnel)

**Commandes :**
```bash
# Créer une nouvelle branche depuis develop
git checkout develop
git pull origin develop
git checkout -b feature/user-authentication

# Développer, committer, puis créer une PR vers develop
```

#### 3. **Branches `feature/*`** (Nouvelles Fonctionnalités)
- **Protection :** Aucune
- **Objectif :** Développement de nouvelles fonctionnalités
- **Naming :** `feature/nom-descriptif`
- **Base :** Créée depuis `develop`
- **Fusion :** Vers `develop` via PR
- **Durée de vie :** Courte (quelques jours max)
- **Suppression :** Après fusion

**Exemples de nommage :**
```bash
feature/user-authentication
feature/shopping-cart
feature/payment-integration
feature/product-reviews
```

**Workflow complet :**
```bash
# 1. Créer la branche depuis develop
git checkout develop
git pull origin develop
git checkout -b feature/user-authentication

# 2. Développer et committer
git add .
git commit -m "feat: implement JWT authentication"

# 3. Pousser vers GitHub
git push origin feature/user-authentication

# 4. Créer une Pull Request sur GitHub
# develop ← feature/user-authentication

# 5. Attendre l'analyse SonarCloud et l'approbation

# 6. Fusionner via l'interface GitHub (Squash and merge recommandé)

# 7. Supprimer la branche après fusion
git checkout develop
git pull origin develop
git branch -d feature/user-authentication
git push origin --delete feature/user-authentication
```

#### 4. **Branches `bugfix/*`** (Corrections de Bugs)
- **Protection :** Aucune
- **Objectif :** Corriger des bugs dans `develop`
- **Naming :** `bugfix/nom-descriptif`
- **Base :** Créée depuis `develop`
- **Fusion :** Vers `develop` via PR

**Exemples de nommage :**
```bash
bugfix/login-redirect
bugfix/cart-total-calculation
bugfix/image-upload-size
```

**Workflow :**
```bash
git checkout develop
git pull origin develop
git checkout -b bugfix/login-redirect

# Corriger le bug
git add .
git commit -m "fix: correct login redirect after authentication"

git push origin bugfix/login-redirect
# Créer PR vers develop
```

#### 5. **Branches `hotfix/*`** (Corrections Urgentes en Production)
- **Protection :** Aucune
- **Objectif :** Corriger des bugs critiques en production
- **Naming :** `hotfix/nom-descriptif`
- **Base :** Créée depuis `main`
- **Fusion :** Vers `main` ET `develop` (double PR)
- **Priorité :** Maximale

**Exemples de nommage :**
```bash
hotfix/security-vulnerability
hotfix/critical-data-loss
hotfix/payment-gateway-error
```

**Workflow hotfix (important) :**
```bash
# 1. Créer depuis main (pas develop!)
git checkout main
git pull origin main
git checkout -b hotfix/security-vulnerability

# 2. Corriger le problème critique
git add .
git commit -m "fix: patch critical security vulnerability CVE-2024-1234"

# 3. Pousser
git push origin hotfix/security-vulnerability

# 4. Créer PR vers main (priorité haute)
# main ← hotfix/security-vulnerability

# 5. Après fusion dans main, créer une PR vers develop aussi
# develop ← hotfix/security-vulnerability

# 6. Ou merger main dans develop directement
git checkout develop
git pull origin develop
git merge main
git push origin develop
```

#### 6. **Branches `release/*`** (Préparation de Release) - Optionnel
- **Protection :** Modérée
- **Objectif :** Stabiliser une version avant production
- **Naming :** `release/v1.2.0`
- **Base :** Créée depuis `develop`
- **Fusion :** Vers `main` et `develop`
- **Usage :** Pour les projets avec cycles de release formels

**Workflow release (optionnel) :**
```bash
# 1. Créer depuis develop
git checkout develop
git pull origin develop
git checkout -b release/v1.2.0

# 2. Finaliser la version (bump version, changelog, etc.)
git add .
git commit -m "chore: prepare release v1.2.0"

# 3. Tester intensivement
# Corriger uniquement les bugs critiques

# 4. PR vers main
# main ← release/v1.2.0

# 5. PR vers develop pour récupérer les corrections
# develop ← release/v1.2.0

# 6. Tag après fusion dans main
git checkout main
git pull origin main
git tag -a v1.2.0 -m "Release version 1.2.0"
git push origin v1.2.0
```

### Workflow Développeur Complet

Voici le workflow quotidien d'un développeur :

```bash
# ============================================
# DÉBUT D'UNE NOUVELLE FONCTIONNALITÉ
# ============================================

# 1. Se positionner sur develop et mettre à jour
git checkout develop
git pull origin develop

# 2. Créer une branche feature
git checkout -b feature/shopping-cart

# ============================================
# DÉVELOPPEMENT
# ============================================

# 3. Développer en committant régulièrement
git add src/cart/
git commit -m "feat: add cart service with CRUD operations"

git add src/cart/
git commit -m "feat: implement cart UI components"

git add src/cart/
git commit -m "test: add unit tests for cart service"

# 4. Pousser régulièrement (backup)
git push origin feature/shopping-cart

# ============================================
# FINALISATION
# ============================================

# 5. Mettre à jour avec develop avant de créer la PR
git fetch origin
git rebase origin/develop
# Résoudre les conflits si nécessaire
git push origin feature/shopping-cart --force-with-lease

# 6. Créer la Pull Request sur GitHub
# Aller sur https://github.com/your-org/safe-zone/compare
# Base: develop ← Compare: feature/shopping-cart
# Titre: "feat: implement shopping cart functionality"
# Description détaillée

# ============================================
# REVUE ET ANALYSE
# ============================================

# 7. Attendre l'analyse automatique
# - GitHub Actions exécute les workflows SonarCloud
# - Les Quality Gates sont vérifiés
# - Un reviewer examine le code

# 8. Si des corrections sont demandées
git add .
git commit -m "fix: address review comments"
git push origin feature/shopping-cart
# Les vérifications se relancent automatiquement

# ============================================
# FUSION
# ============================================

# 9. Une fois approuvée et les Quality Gates passés
# Fusionner via l'interface GitHub (bouton "Merge")
# Option recommandée: "Squash and merge"

# ============================================
# NETTOYAGE
# ============================================

# 10. Après fusion, nettoyer
git checkout develop
git pull origin develop
git branch -d feature/shopping-cart
git push origin --delete feature/shopping-cart

# ============================================
# RÉPÉTER POUR LA PROCHAINE FONCTIONNALITÉ
# ============================================
```

### Conventions de Nommage des Commits

Utilisez les **Conventional Commits** pour un historique clair :

```
feat: ajouter une nouvelle fonctionnalité
fix: corriger un bug
docs: modifier la documentation
style: changements de formatage (whitespace, etc.)
refactor: refactorisation sans changement de fonctionnalité
test: ajouter ou modifier des tests
chore: tâches de maintenance (build, deps, etc.)
perf: améliorations de performance
ci: changements de CI/CD
```

**Exemples :**
```bash
git commit -m "feat: implement user authentication with JWT"
git commit -m "fix: correct cart total calculation for discounts"
git commit -m "docs: update API documentation for product endpoints"
git commit -m "test: add integration tests for payment service"
git commit -m "refactor: extract validation logic into separate service"
git commit -m "chore: upgrade Spring Boot to 3.2.1"
git commit -m "ci: add SonarCloud quality gates to workflow"
```

---

## Configuration via Interface GitHub

### Étapes Détaillées avec l'Interface Web

#### Étape 1 : Accéder aux Paramètres du Dépôt

1. Aller sur votre dépôt GitHub : `https://github.com/your-org/safe-zone`
2. Cliquer sur **Settings** (roue dentée en haut à droite)
3. Dans le menu latéral gauche, cliquer sur **Branches** (sous "Code and automation")

#### Étape 2 : Créer une Règle de Protection

1. Dans la section **"Branch protection rules"**, cliquer sur **"Add rule"**
2. Dans **"Branch name pattern"**, entrer : `main`

#### Étape 3 : Configurer "Require a Pull Request Before Merging"

1. ✅ Cocher **"Require a pull request before merging"**
2. Sous cette option, configurer :
   - ✅ **"Require approvals"** : Sélectionner `1` dans le dropdown
   - ✅ **"Dismiss stale pull request approvals when new commits are pushed"**
   - ⬜ **"Require review from Code Owners"** (laisser décoché pour débuter)

#### Étape 4 : Configurer "Require Status Checks to Pass"

1. ✅ Cocher **"Require status checks to pass before merging"**
2. ✅ Cocher **"Require branches to be up to date before merging"**
3. Dans **"Search for status checks in the last week for this repository"**, rechercher et sélectionner :

   **Note importante :** Ces checks n'apparaîtront qu'après la première exécution des workflows. Si vous ne les voyez pas :
   - Créez une branche de test : `git checkout -b test-branch`
   - Modifiez un fichier backend ou frontend
   - Poussez et créez une PR
   - Attendez que les workflows s'exécutent
   - Retournez dans Settings > Branches
   - Les checks apparaîtront maintenant dans la liste

   Sélectionnez tous ces checks :
   ```
   ✅ Analyze Frontend (Angular)
   ✅ Analyze User Service
   ✅ Analyze Product Service
   ✅ Analyze Media Service
   ✅ Backend - user-service
   ✅ Backend - product-service
   ✅ Backend - media-service
   ✅ Frontend (Angular)
   ✅ Analysis Summary
   ```

#### Étape 5 : Configurer les Options Additionnelles

1. ✅ Cocher **"Require conversation resolution before merging"**
2. ⬜ Laisser décoché **"Require signed commits"** (sauf besoin spécifique)
3. ⬜ Laisser décoché **"Require linear history"**
4. ✅ Cocher **"Include administrators"** (recommandé)
5. ✅ Cocher **"Do not allow bypassing the above settings"**
6. ⬜ Laisser décoché **"Allow force pushes"** (**IMPORTANT**)
7. ⬜ Laisser décoché **"Allow deletions"** (**IMPORTANT**)

#### Étape 6 : Sauvegarder

1. Descendre en bas de la page
2. Cliquer sur **"Create"** (ou **"Save changes"** si modification)
3. Confirmation : "Branch protection rule created"

### Vérification Visuelle

Après création, vous devriez voir dans la liste des rules :

```
┌────────────────────────────────────────────────────────┐
│ Branch protection rules                                │
├────────────────────────────────────────────────────────┤
│                                                         │
│ ◉ main                                                 │
│   │                                                     │
│   ├─ ✓ Require a pull request before merging          │
│   │  └─ Require 1 approving review                    │
│   │                                                     │
│   ├─ ✓ Require status checks to pass before merging   │
│   │  └─ 9 required status checks                      │
│   │                                                     │
│   ├─ ✓ Require conversation resolution                │
│   ├─ ✓ Include administrators                         │
│   └─ ✓ Do not allow bypassing settings               │
│                                                         │
│   [Edit] [Delete]                                      │
│                                                         │
└────────────────────────────────────────────────────────┘
```

---

## Automatisation avec GitHub CLI

### Installation de GitHub CLI

#### Linux (Debian/Ubuntu)
```bash
# Méthode 1 : Via apt
type -p curl >/dev/null || (sudo apt update && sudo apt install curl -y)
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
sudo chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update
sudo apt install gh -y

# Méthode 2 : Via snap
sudo snap install gh
```

#### Windows
```powershell
# Via Chocolatey
choco install gh

# Via Scoop
scoop install gh

# Via WinGet
winget install GitHub.cli
```

#### macOS
```bash
# Via Homebrew
brew install gh
```

### Authentification

```bash
# Lancer l'authentification interactive
gh auth login

# Sélectionner :
# ? What account do you want to log into? GitHub.com
# ? What is your preferred protocol for Git operations? HTTPS
# ? Authenticate Git with your GitHub credentials? Yes
# ? How would you like to authenticate GitHub CLI? Login with a web browser

# Suivre les instructions pour s'authentifier via le navigateur
```

### Script d'Automatisation pour Protection de `main`

Créez un fichier `setup-branch-protection.sh` :

```bash
#!/bin/bash

# ============================================================================
# Script d'Automatisation de la Protection de Branche GitHub
# ============================================================================
# Ce script configure automatiquement les règles de protection pour main
# Utilise GitHub CLI (gh) pour automatiser la configuration
# ============================================================================

set -e  # Arrêter en cas d'erreur

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction pour afficher les messages
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ============================================================================
# Vérifications préalables
# ============================================================================

log_info "Vérification de GitHub CLI..."
if ! command -v gh &> /dev/null; then
    log_error "GitHub CLI (gh) n'est pas installé"
    log_info "Installez-le depuis : https://cli.github.com/"
    exit 1
fi
log_success "GitHub CLI trouvé"

log_info "Vérification de l'authentification..."
if ! gh auth status &> /dev/null; then
    log_error "Vous n'êtes pas authentifié avec GitHub CLI"
    log_info "Lancez : gh auth login"
    exit 1
fi
log_success "Authentification OK"

# ============================================================================
# Configuration des variables
# ============================================================================

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
log_info "Dépôt : $REPO"

BRANCH="main"
log_info "Branche à protéger : $BRANCH"

# ============================================================================
# Liste des status checks requis (SonarCloud)
# ============================================================================

REQUIRED_CHECKS=(
    "Analyze Frontend (Angular)"
    "Analyze User Service"
    "Analyze Product Service"
    "Analyze Media Service"
    "Backend - user-service"
    "Backend - product-service"
    "Backend - media-service"
    "Frontend (Angular)"
    "Analysis Summary"
)

log_info "Status checks requis : ${#REQUIRED_CHECKS[@]}"

# ============================================================================
# Suppression de la règle existante si elle existe
# ============================================================================

log_info "Vérification d'une règle existante..."
if gh api "repos/$REPO/branches/$BRANCH/protection" &> /dev/null; then
    log_warning "Une règle de protection existe déjà pour $BRANCH"
    read -p "Voulez-vous la supprimer et la recréer ? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "Suppression de la règle existante..."
        gh api -X DELETE "repos/$REPO/branches/$BRANCH/protection" || true
        log_success "Règle supprimée"
    else
        log_error "Opération annulée"
        exit 1
    fi
fi

# ============================================================================
# Création de la règle de protection
# ============================================================================

log_info "Création de la règle de protection pour $BRANCH..."

# Construction du JSON pour les status checks
STATUS_CHECKS_JSON=$(printf '%s\n' "${REQUIRED_CHECKS[@]}" | jq -R . | jq -s '{contexts: .}')

# Configuration complète de la protection
gh api -X PUT "repos/$REPO/branches/$BRANCH/protection" \
  --input - <<EOF
{
  "required_status_checks": {
    "strict": true,
    "contexts": $(echo "$STATUS_CHECKS_JSON" | jq -c '.contexts')
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1,
    "require_last_push_approval": false
  },
  "required_linear_history": false,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "block_creations": false,
  "required_conversation_resolution": true,
  "lock_branch": false,
  "allow_fork_syncing": true
}
EOF

log_success "Règle de protection créée avec succès !"

# ============================================================================
# Vérification de la configuration
# ============================================================================

log_info "Vérification de la configuration..."

gh api "repos/$REPO/branches/$BRANCH/protection" | jq '{
  "Pull Request Reviews": .required_pull_request_reviews.required_approving_review_count,
  "Dismiss Stale Reviews": .required_pull_request_reviews.dismiss_stale_reviews,
  "Status Checks Required": .required_status_checks.contexts | length,
  "Status Checks Strict": .required_status_checks.strict,
  "Enforce Admins": .enforce_admins,
  "Conversation Resolution": .required_conversation_resolution,
  "Allow Force Pushes": .allow_force_pushes,
  "Allow Deletions": .allow_deletions
}'

log_success "Configuration terminée !"
log_info "Règle de protection active pour $REPO/$BRANCH"

# ============================================================================
# Instructions finales
# ============================================================================

echo ""
log_success "✅ La branche $BRANCH est maintenant protégée !"
echo ""
echo "Prochaines étapes :"
echo "  1. Créez une branche de test : git checkout -b test-protection"
echo "  2. Modifiez un fichier et committez"
echo "  3. Poussez : git push origin test-protection"
echo "  4. Créez une Pull Request pour tester la protection"
echo ""
echo "Documentation complète : docs/05-BRANCH-PROTECTION.md"
echo ""
```

### Utilisation du Script

```bash
# Rendre le script exécutable
chmod +x setup-branch-protection.sh

# Exécuter le script
./setup-branch-protection.sh
```

**Sortie attendue :**
```
[INFO] Vérification de GitHub CLI...
[SUCCESS] GitHub CLI trouvé
[INFO] Vérification de l'authentification...
[SUCCESS] Authentification OK
[INFO] Dépôt : your-org/safe-zone
[INFO] Branche à protéger : main
[INFO] Status checks requis : 9
[INFO] Vérification d'une règle existante...
[INFO] Création de la règle de protection pour main...
[SUCCESS] Règle de protection créée avec succès !
[INFO] Vérification de la configuration...
{
  "Pull Request Reviews": 1,
  "Dismiss Stale Reviews": true,
  "Status Checks Required": 9,
  "Status Checks Strict": true,
  "Enforce Admins": true,
  "Conversation Resolution": true,
  "Allow Force Pushes": false,
  "Allow Deletions": false
}
[SUCCESS] Configuration terminée !
[INFO] Règle de protection active pour your-org/safe-zone/main

✅ La branche main est maintenant protégée !

Prochaines étapes :
  1. Créez une branche de test : git checkout -b test-protection
  2. Modifiez un fichier et committez
  3. Poussez : git push origin test-protection
  4. Créez une Pull Request pour tester la protection

Documentation complète : docs/05-BRANCH-PROTECTION.md
```

### Commandes GitHub CLI Utiles

#### Voir la Configuration Actuelle
```bash
# Voir la protection de main
gh api repos/your-org/safe-zone/branches/main/protection | jq .

# Voir uniquement les status checks requis
gh api repos/your-org/safe-zone/branches/main/protection | jq '.required_status_checks.contexts'

# Voir si enforce_admins est activé
gh api repos/your-org/safe-zone/branches/main/protection | jq '.enforce_admins'
```

#### Modifier une Règle Existante
```bash
# Changer le nombre de reviews requis à 2
gh api -X PATCH "repos/your-org/safe-zone/branches/main/protection" \
  --field required_approving_review_count=2

# Activer enforce_admins
gh api -X PATCH "repos/your-org/safe-zone/branches/main/protection" \
  --field enforce_admins=true

# Désactiver force pushes (recommandé)
gh api -X PATCH "repos/your-org/safe-zone/branches/main/protection" \
  --field allow_force_pushes=false
```

#### Supprimer une Règle
```bash
# Supprimer la protection de main (DANGER !)
gh api -X DELETE "repos/your-org/safe-zone/branches/main/protection"
```

#### Lister Toutes les Branches Protégées
```bash
# Lister toutes les branches avec leur statut de protection
gh api "repos/your-org/safe-zone/branches" | jq '.[] | {name: .name, protected: .protected}'
```

---

## Tests de Validation

### Test 1 : Tentative de Push Direct sur `main` (Devrait Échouer)

**Objectif :** Vérifier que les commits directs sont bloqués.

```bash
# 1. Se positionner sur main
git checkout main

# 2. Créer un fichier de test
echo "Test direct push" > test-file.txt
git add test-file.txt
git commit -m "test: attempt direct push to main"

# 3. Tenter de pousser
git push origin main
```

**Résultat attendu (ÉCHEC) :**
```
remote: error: GH006: Protected branch update failed for refs/heads/main.
remote: error: Cannot push to protected branch 'main'
To https://github.com/your-org/safe-zone.git
 ! [remote rejected] main -> main (protected branch hook declined)
error: failed to push some refs to 'https://github.com/your-org/safe-zone.git'
```

**✅ Test réussi si le push est bloqué !**

**Nettoyage :**
```bash
# Annuler le commit local
git reset --soft HEAD~1
git checkout -- test-file.txt
```

### Test 2 : Créer une PR Sans Approbation (Devrait Bloquer le Merge)

**Objectif :** Vérifier que la fusion nécessite une approbation.

```bash
# 1. Créer une branche de test
git checkout -b test/no-approval
echo "Test PR without approval" > test-pr.txt
git add test-pr.txt
git commit -m "test: PR without approval"
git push origin test/no-approval

# 2. Créer une PR sur GitHub (via interface ou gh CLI)
gh pr create --title "Test: PR without approval" --body "Testing branch protection" --base main

# 3. Tenter de fusionner immédiatement
gh pr merge --auto
```

**Résultat attendu (ÉCHEC) :**
```
❌ Pull request cannot be merged
Required reviews: 0/1
Required status checks: 0/9 passed
```

**✅ Test réussi si le merge est bloqué avant approbation !**

### Test 3 : Créer une PR Sans Status Checks (Devrait Bloquer le Merge)

**Objectif :** Vérifier que les quality gates SonarCloud sont obligatoires.

```bash
# 1. Créer une branche avec des changements
git checkout -b test/no-status-checks
echo "Test PR without checks" > backend/user-service/src/test.txt
git add backend/user-service/src/test.txt
git commit -m "test: PR without passing status checks"
git push origin test/no-status-checks

# 2. Créer une PR
gh pr create --title "Test: PR without status checks" --body "Testing status checks requirement" --base main

# 3. Obtenir l'approbation d'un autre développeur

# 4. Tenter de fusionner avant que les checks ne passent
gh pr merge --auto
```

**Résultat attendu (ÉCHEC) :**
```
❌ Pull request cannot be merged
Required status checks: 3/9 passed
  ⏳ Backend - user-service (in progress)
  ⏳ Backend - product-service (pending)
  ⏳ Backend - media-service (pending)
  ...
```

**✅ Test réussi si le merge est bloqué avant la fin des checks !**

### Test 4 : Workflow Complet (Devrait Réussir)

**Objectif :** Tester un workflow complet avec succès.

```bash
# ============================================
# ÉTAPE 1 : Créer une branche feature
# ============================================
git checkout develop  # ou main si pas de develop
git pull origin develop
git checkout -b test/complete-workflow

# ============================================
# ÉTAPE 2 : Faire des changements valides
# ============================================
# Créer un fichier de documentation (pas de code pour éviter les issues)
cat > docs/TEST-WORKFLOW.md <<EOF
# Test Workflow Documentation

Ce fichier teste le workflow complet de protection de branche.

## Changements
- Ajout de documentation de test
- Vérification des quality gates
- Validation du processus de revue
EOF

git add docs/TEST-WORKFLOW.md
git commit -m "docs: add test workflow documentation"

# ============================================
# ÉTAPE 3 : Pousser et créer une PR
# ============================================
git push origin test/complete-workflow

gh pr create \
  --title "docs: test complete workflow" \
  --body "## Description

Test du workflow complet de protection de branche.

## Checklist
- [x] Documentation ajoutée
- [x] Aucun code smell introduit
- [x] Prêt pour review

## Type
- [ ] Feature
- [ ] Bugfix
- [x] Documentation
- [ ] Refactor" \
  --base main

# ============================================
# ÉTAPE 4 : Attendre les vérifications
# ============================================
echo "Attente des status checks SonarCloud..."
gh pr checks --watch

# ============================================
# ÉTAPE 5 : Demander une revue
# ============================================
# Demander à un collègue de reviewer
gh pr review --approve  # (effectué par un autre développeur)

# ============================================
# ÉTAPE 6 : Vérifier que tout est OK
# ============================================
gh pr view

# Devrait afficher :
# ✅ Review: 1 approval
# ✅ Status checks: 9/9 passed
# ✅ Conversations: resolved
# ✅ Ready to merge

# ============================================
# ÉTAPE 7 : Fusionner
# ============================================
gh pr merge --squash --delete-branch

# ============================================
# ÉTAPE 8 : Nettoyer
# ============================================
git checkout main
git pull origin main
```

**Résultat attendu (SUCCÈS) :**
```
✅ Pull request #123 merged successfully
✅ Deleted branch test/complete-workflow
```

**✅ Test réussi si toutes les étapes fonctionnent !**

### Test 5 : Tester "Dismiss Stale Reviews"

**Objectif :** Vérifier que les approbations sont annulées si de nouveaux commits sont ajoutés.

```bash
# 1. Créer une PR et obtenir une approbation
git checkout -b test/stale-review
echo "Initial change" > test-stale.txt
git add test-stale.txt
git commit -m "test: initial change for stale review test"
git push origin test/stale-review

gh pr create --title "Test: Stale Review" --body "Testing dismiss stale reviews" --base main

# Attendre qu'un reviewer approuve (via interface ou autre compte)
# gh pr review <PR_NUMBER> --approve

# 2. Ajouter un nouveau commit après approbation
echo "Additional change after approval" >> test-stale.txt
git add test-stale.txt
git commit -m "test: change after approval"
git push origin test/stale-review

# 3. Vérifier le statut de la PR
gh pr view
```

**Résultat attendu (SUCCÈS) :**
```
❌ Pull request cannot be merged
Required reviews: 0/1 (previous approval dismissed)
⚠️ New commits have been pushed since last review
```

**✅ Test réussi si l'approbation précédente est annulée !**

### Test 6 : Tester "Require Conversations Resolution"

**Objectif :** Vérifier que toutes les conversations doivent être résolues.

```bash
# 1. Créer une PR
git checkout -b test/conversation-resolution
echo "Test content" > test-conv.txt
git add test-conv.txt
git commit -m "test: conversation resolution"
git push origin test/conversation-resolution

gh pr create --title "Test: Conversation Resolution" --body "Testing conversation resolution requirement" --base main

# 2. Un reviewer ajoute un commentaire sans l'approuver
# Via interface GitHub ou :
gh pr comment <PR_NUMBER> --body "Please improve this documentation"

# 3. Tenter de fusionner même avec approbation
# (un autre reviewer approuve)
gh pr review <PR_NUMBER> --approve

# 4. Vérifier le statut
gh pr view
```

**Résultat attendu (ÉCHEC) :**
```
❌ Pull request cannot be merged
Required reviews: 1/1 ✅
Status checks: 9/9 ✅
⚠️ Unresolved conversations: 1
```

**✅ Test réussi si le merge est bloqué avec des conversations non résolues !**

**Résolution :**
```bash
# Résoudre la conversation via l'interface GitHub
# Puis :
gh pr merge --squash --delete-branch
```

### Checklist de Tests Complète

Utilisez cette checklist pour valider votre configuration :

```
┌─────────────────────────────────────────────────────────┐
│ CHECKLIST DE VALIDATION DE LA PROTECTION DES BRANCHES  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│ Protection Basique                                       │
│ ☐ Les commits directs sur main sont bloqués            │
│ ☐ Les PRs sont obligatoires pour fusionner              │
│                                                          │
│ Revues de Code                                           │
│ ☐ Au moins 1 approbation est requise                   │
│ ☐ Les approbations sont annulées après nouveaux commits │
│ ☐ Le merge est bloqué sans approbation                 │
│                                                          │
│ Status Checks SonarCloud                                 │
│ ☐ Les 3 services backend sont vérifiés                 │
│ ☐ Le frontend est vérifié                              │
│ ☐ Le job "Analysis Summary" est vérifié                │
│ ☐ Le merge est bloqué si un check échoue               │
│ ☐ Les branches doivent être à jour avant merge         │
│                                                          │
│ Conversations                                            │
│ ☐ Les conversations doivent être résolues              │
│ ☐ Le merge est bloqué avec conversations ouvertes      │
│                                                          │
│ Sécurité                                                 │
│ ☐ Les force pushes sont désactivés                     │
│ ☐ La suppression de branche est désactivée             │
│ ☐ Les administrateurs suivent les mêmes règles         │
│ ☐ Le contournement des règles est désactivé            │
│                                                          │
│ Workflow Complet                                         │
│ ☐ Une PR avec code propre peut être fusionnée          │
│ ☐ Une PR avec bugs est bloquée par SonarCloud          │
│ ☐ Les notifications de status sont visibles            │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Troubleshooting

### Problème 1 : Les Status Checks n'Apparaissent Pas

**Symptôme :**
Lors de la configuration, la recherche de status checks ne retourne aucun résultat.

**Cause :**
Les status checks n'apparaissent que lorsqu'ils ont été exécutés au moins une fois sur une PR.

**Solution :**
```bash
# 1. Créer une branche de test
git checkout -b initialize-status-checks

# 2. Faire un changement minimal
echo "# Initialize checks" >> README.md
git add README.md
git commit -m "chore: initialize status checks"

# 3. Pousser et créer une PR
git push origin initialize-status-checks
gh pr create --title "Initialize Status Checks" --body "First PR to trigger workflows" --base main

# 4. Attendre que les workflows s'exécutent
gh pr checks --watch

# 5. Retourner dans Settings > Branches
# Les checks apparaissent maintenant dans la liste

# 6. Fermer la PR de test (optionnel)
gh pr close
git checkout main
git branch -D initialize-status-checks
git push origin --delete initialize-status-checks
```

### Problème 2 : "Branch Protection Rule Already Exists"

**Symptôme :**
Lors de la création d'une règle : "A branch protection rule already exists for this branch pattern."

**Solution :**
```bash
# Supprimer la règle existante via GitHub CLI
gh api -X DELETE "repos/your-org/safe-zone/branches/main/protection"

# Ou via l'interface web :
# Settings > Branches > Cliquer sur [Delete] à côté de la règle main

# Puis recréer la règle
./setup-branch-protection.sh
```

### Problème 3 : "Required Status Checks Not Found"

**Symptôme :**
Les workflows s'exécutent mais le merge est bloqué avec "Required status check 'xxx' not found."

**Cause :**
Le nom du check dans la configuration ne correspond pas exactement au nom du job dans le workflow.

**Solution :**
```bash
# 1. Vérifier le nom exact des checks dans une PR récente
gh pr view <PR_NUMBER> --json statusCheckRollup | jq '.statusCheckRollup[] | .context'

# 2. Comparer avec votre configuration
gh api "repos/your-org/safe-zone/branches/main/protection" | jq '.required_status_checks.contexts'

# 3. Mettre à jour les noms si différents
# Via interface : Settings > Branches > Edit rule > Décocher/recocher les checks
# Via CLI : Modifier setup-branch-protection.sh avec les noms corrects
```

**Noms exacts des jobs dans vos workflows :**
```
sonarqube-backend.yml:
  - "Analyze User Service"
  - "Analyze Product Service"
  - "Analyze Media Service"

sonarqube-frontend.yml:
  - "Analyze Frontend (Angular)"

sonarqube-full.yml:
  - "Backend - user-service"
  - "Backend - product-service"
  - "Backend - media-service"
  - "Frontend (Angular)"
  - "Analysis Summary"
```

### Problème 4 : Impossible de Fusionner Malgré Tous les Checks Passés

**Symptôme :**
Tous les checks sont verts, la PR est approuvée, mais le bouton "Merge" reste grisé.

**Causes possibles :**

#### a) La Branche n'est Pas à Jour
**Vérification :**
```bash
gh pr view <PR_NUMBER> --json mergeable,mergeStateStatus

# Si mergeable: "BEHIND", la branche n'est pas à jour
```

**Solution :**
```bash
# Mettre à jour la branche
git checkout your-branch
git fetch origin
git rebase origin/main  # ou git merge origin/main
git push origin your-branch --force-with-lease

# Attendre que les checks se relancent
```

#### b) Des Conversations ne Sont Pas Résolues
**Vérification :**
```bash
# Via l'interface GitHub, vérifier la section "Conversations"
```

**Solution :**
```bash
# Résoudre toutes les conversations via l'interface
# Cliquer sur "Resolve conversation" pour chaque commentaire
```

#### c) Le Nombre d'Approbations est Insuffisant
**Vérification :**
```bash
gh pr view <PR_NUMBER> --json reviewDecision

# Si reviewDecision: "REVIEW_REQUIRED", il manque une approbation
```

**Solution :**
```bash
# Demander une revue à un collègue
gh pr review <PR_NUMBER> --approve  # (exécuté par un autre développeur)
```

### Problème 5 : "You Are Not Allowed to Merge This Pull Request"

**Symptôme :**
Erreur lors de la tentative de merge : "You are not allowed to merge this pull request."

**Cause :**
Vous avez activé "Include administrators" et vous essayez de merger votre propre PR sans approbation.

**Solution :**
```bash
# Option 1 : Demander l'approbation d'un autre développeur
gh pr review <PR_NUMBER> --approve  # (exécuté par un collègue)

# Option 2 : Désactiver temporairement "Include administrators" (non recommandé)
# Via Settings > Branches > Edit rule > Décocher "Include administrators"

# Option 3 : Ajouter un reviewer CODEOWNERS
# Créer .github/CODEOWNERS :
echo "* @other-developer" > .github/CODEOWNERS
git add .github/CODEOWNERS
git commit -m "chore: add CODEOWNERS"
git push
```

### Problème 6 : Les Workflows Ne Se Déclenchent Pas sur une PR

**Symptôme :**
Après création d'une PR, aucun workflow GitHub Actions ne démarre.

**Causes possibles :**

#### a) Les Chemins Filtrés Ne Correspondent Pas
**Vérification :**
```bash
# Vérifier quels fichiers ont été modifiés
gh pr diff <PR_NUMBER> --name-only

# Comparer avec les paths: dans les workflows
cat .github/workflows/sonarqube-backend.yml | grep -A 5 "paths:"
```

**Solution :**
Si vous avez modifié uniquement le README, les workflows backend/frontend ne se déclencheront pas.
```bash
# Modifier un fichier backend ou frontend
echo "// Trigger workflow" >> backend/user-service/README.md
git add backend/user-service/README.md
git commit -m "chore: trigger workflow"
git push origin your-branch
```

#### b) Le Dépôt n'a Pas Accès aux Workflows
**Vérification :**
```bash
# Vérifier les permissions des workflows
gh api "repos/your-org/safe-zone" | jq '.permissions'
```

**Solution :**
```bash
# Activer les workflows dans Settings > Actions > General
# Sélectionner "Allow all actions and reusable workflows"
```

### Problème 7 : Force Push Nécessaire Après Rebase

**Symptôme :**
Après un rebase, git refuse le push normal : "Updates were rejected because the tip of your current branch is behind."

**Solution :**
```bash
# Utiliser --force-with-lease pour un force push sécurisé
git push origin your-branch --force-with-lease

# NE JAMAIS utiliser --force sur main (bloqué de toute façon)
git push origin main --force  # ❌ ERREUR : blocked by protection
```

**Note :** `--force-with-lease` vérifie que personne d'autre n'a poussé entre-temps. C'est plus sûr que `--force`.

### Problème 8 : GitHub CLI Authentication Expired

**Symptôme :**
```
gh: error: authentication token expired
```

**Solution :**
```bash
# Se ré-authentifier
gh auth logout
gh auth login

# Vérifier le statut
gh auth status
```

### Problème 9 : "Quality Gate Status Unknown"

**Symptôme :**
Le job SonarCloud se termine mais le status est "UNKNOWN" au lieu de "PASSED" ou "FAILED".

**Cause :**
Le fichier `report-task.txt` n'a pas été généré correctement ou SonarCloud n'a pas fini l'analyse.

**Solution :**
```bash
# 1. Vérifier les logs du workflow
gh run view <RUN_ID> --log

# 2. Vérifier que le token SONAR_TOKEN est correct
gh secret list

# 3. Augmenter le timeout dans le workflow
# Dans sonarqube-backend.yml, ligne 149 :
# timeout-minutes: 5  # Passer à 10 si nécessaire

# 4. Vérifier sur SonarCloud directement
# https://sonarcloud.io/organizations/zone01-ecommerce/projects
```

### Problème 10 : Merge Accidentel de `main` dans une Feature Branch

**Symptôme :**
Vous avez accidentellement fait `git merge main` au lieu de `git rebase main` dans votre feature branch.

**Solution :**
```bash
# Si pas encore poussé, annuler le merge
git reset --hard HEAD~1

# Si déjà poussé, créer une nouvelle branche propre
git checkout main
git pull origin main
git checkout -b feature/same-feature-clean

# Cherry-pick vos commits (sans le merge commit)
git cherry-pick <commit1> <commit2> ...

# Supprimer l'ancienne branche
git branch -D feature/same-feature
git push origin --delete feature/same-feature

# Pousser la nouvelle branche
git push origin feature/same-feature-clean
```

---

## Bonnes Pratiques

### 1. Revue de Code Efficace

#### Checklist du Reviewer
```
☐ Le code est-il lisible et bien commenté ?
☐ Les noms de variables/fonctions sont-ils clairs ?
☐ Y a-t-il des duplications de code ?
☐ Les tests couvrent-ils les nouveaux changements ?
☐ La performance est-elle acceptable ?
☐ Y a-t-il des failles de sécurité potentielles ?
☐ La documentation est-elle mise à jour ?
☐ Les Quality Gates SonarCloud sont-ils passés ?
☐ Les conventions de nommage sont-elles respectées ?
☐ Y a-t-il des magic numbers ou hardcoded values ?
```

#### Commentaires Constructifs
```bash
# ✅ BON : Constructif et explicatif
"Considérer l'utilisation d'un Optional ici pour gérer le cas null de manière plus élégante. Exemple : return Optional.ofNullable(user);"

# ❌ MAUVAIS : Vague et non constructif
"Ce code est mauvais."
```

### 2. Taille des Pull Requests

#### Pull Request Idéale
- **Lignes de code :** 200-400 lignes modifiées max
- **Fichiers :** 5-15 fichiers max
- **Scope :** Une seule fonctionnalité ou correction
- **Temps de revue :** 15-30 minutes max pour un reviewer

#### Si Trop Grande
```bash
# Diviser en plusieurs PRs logiques
# PR 1 : Backend API
# PR 2 : Frontend UI
# PR 3 : Tests d'intégration
# PR 4 : Documentation
```

### 3. Description des Pull Requests

#### Template de PR Recommandé
Créez `.github/PULL_REQUEST_TEMPLATE.md` :

```markdown
## Description
<!-- Décrivez brièvement les changements apportés -->

## Type de changement
- [ ] 🚀 Nouvelle fonctionnalité (feature)
- [ ] 🐛 Correction de bug (bugfix)
- [ ] 📚 Documentation
- [ ] ♻️ Refactoring (pas de changement fonctionnel)
- [ ] ⚡ Amélioration de performance
- [ ] ✅ Tests
- [ ] 🔧 Configuration / Chores

## Checklist
- [ ] Mon code suit les conventions du projet
- [ ] J'ai effectué une auto-revue de mon code
- [ ] J'ai commenté les parties complexes
- [ ] J'ai mis à jour la documentation si nécessaire
- [ ] Mes changements ne génèrent pas de warnings
- [ ] J'ai ajouté des tests qui prouvent que ma correction fonctionne
- [ ] Les tests nouveaux et existants passent localement
- [ ] Les Quality Gates SonarCloud passent

## Liens
- Issue liée : #XXX
- Documentation : [lien]
- SonarCloud : [lien]

## Screenshots (si applicable)
<!-- Ajouter des captures d'écran pour les changements UI -->

## Tests effectués
<!-- Décrire les tests manuels effectués -->
1.
2.
3.

## Impact
<!-- Y a-t-il un impact sur d'autres parties du système ? -->
- [ ] Aucun impact
- [ ] Impact mineur (préciser)
- [ ] Impact majeur (préciser)

## Notes pour les reviewers
<!-- Indiquer les points qui nécessitent une attention particulière -->
```

### 4. Gestion des Conflits

#### Prévention
```bash
# Synchroniser régulièrement avec main
git fetch origin
git rebase origin/main

# Avant de finaliser une PR
git checkout your-branch
git fetch origin
git rebase origin/main
git push origin your-branch --force-with-lease
```

#### Résolution
```bash
# En cas de conflit pendant un rebase
git rebase origin/main

# Git marque les conflits
# Résoudre les conflits dans les fichiers marqués

# Après résolution
git add .
git rebase --continue

# Si trop complexe, abandonner et faire un merge
git rebase --abort
git merge origin/main
```

### 5. Nettoyage des Branches

#### Automatiser le Nettoyage
```bash
# Créer un alias git pour nettoyer les branches fusionnées
git config --global alias.cleanup "!git branch --merged | grep -v '\\*\\|main\\|develop' | xargs -n 1 git branch -d"

# Utiliser l'alias
git cleanup

# Supprimer les branches distantes fusionnées
git fetch origin --prune

# Script de nettoyage complet
cat > clean-branches.sh <<'EOF'
#!/bin/bash
echo "Nettoyage des branches locales fusionnées..."
git checkout main
git pull origin main
git branch --merged | grep -v "\*\|main\|develop" | xargs -n 1 git branch -d

echo "Nettoyage des branches distantes fusionnées..."
git fetch origin --prune

echo "Branches restantes :"
git branch -a
EOF

chmod +x clean-branches.sh
./clean-branches.sh
```

### 6. Communication dans les PRs

#### Bonnes Pratiques de Communication

**✅ Faire :**
- Répondre rapidement aux commentaires (< 24h)
- Expliquer les décisions techniques
- Remercier les reviewers
- Marquer les conversations comme résolues
- Utiliser des emojis pour clarifier le ton 😊

**❌ Éviter :**
- Être défensif face aux critiques
- Ignorer les commentaires
- Fusionner sans répondre aux questions
- Être passif-agressif

#### Exemples de Réponses
```markdown
# ✅ Bonne réponse
> Pourquoi utiliser un HashMap ici au lieu d'un TreeMap ?

Bonne question ! J'ai choisi HashMap car nous n'avons pas besoin d'ordre trié et HashMap offre O(1) pour get/put au lieu de O(log n) pour TreeMap. Vu que nous faisons des lookups très fréquents (voir ligne 245), c'est plus performant. Si l'ordre devient important plus tard, on pourra refactoriser.

# ❌ Mauvaise réponse
> Pourquoi utiliser un HashMap ici au lieu d'un TreeMap ?

Parce que ça marche.
```

### 7. Intégration Continue

#### Vérifications Locales Avant Push
```bash
# Créer un script pre-push
cat > .git/hooks/pre-push <<'EOF'
#!/bin/bash

echo "🔍 Running pre-push checks..."

# 1. Vérifier que les tests passent
echo "Running tests..."
if ! mvn test -f backend/user-service/pom.xml; then
    echo "❌ Backend tests failed"
    exit 1
fi

if ! cd frontend && npm test -- --watch=false; then
    echo "❌ Frontend tests failed"
    exit 1
fi

# 2. Vérifier le build
echo "Building application..."
if ! mvn clean package -DskipTests -f backend/user-service/pom.xml; then
    echo "❌ Backend build failed"
    exit 1
fi

if ! cd frontend && npm run build; then
    echo "❌ Frontend build failed"
    exit 1
fi

echo "✅ All pre-push checks passed!"
exit 0
EOF

chmod +x .git/hooks/pre-push
```

### 8. Documentation des Décisions

#### Architecture Decision Records (ADR)
```bash
# Créer un répertoire pour les ADRs
mkdir -p docs/adr

# Template ADR
cat > docs/adr/001-branch-protection-strategy.md <<'EOF'
# ADR 001: Stratégie de Protection des Branches

## Status
Accepté

## Context
Nous devons protéger la branche main pour éviter les commits directs et garantir la qualité du code via SonarCloud.

## Decision
- Utiliser les Branch Protection Rules de GitHub
- Exiger 1 approbation minimum
- Rendre obligatoires tous les status checks SonarCloud (9 checks)
- Activer "Include administrators" pour cohérence
- Désactiver force pushes et deletions

## Consequences
### Positives
- Qualité du code garantie
- Processus de revue systématique
- Traçabilité complète

### Négatives
- Ralentissement du workflow (compensé par automatisation)
- Nécessite au moins 2 développeurs pour merger
- Hotfixes plus complexes (nécessitent toujours une PR)

## Alternatives Considered
1. Protection manuelle (rejetée : erreur humaine possible)
2. Hooks Git locaux (rejetée : contournable)
3. CI/CD sans protection de branche (rejetée : pas de blocage)

## Date
2024-12-15
EOF
```

---

## Conclusion

La protection des branches est un élément essentiel d'un workflow de développement professionnel. En suivant ce guide, vous avez :

✅ **Configuré la protection de `main`** avec les règles les plus strictes
✅ **Intégré SonarCloud** comme quality gate obligatoire
✅ **Automatisé la configuration** avec GitHub CLI
✅ **Testé la configuration** avec des scénarios réels
✅ **Appris les bonnes pratiques** de revue de code et de collaboration

### Prochaines Étapes

1. **Appliquer la Configuration**
   ```bash
   ./setup-branch-protection.sh
   ```

2. **Tester avec une PR de Test**
   ```bash
   git checkout -b test/protection-validation
   # Faire des changements
   git push origin test/protection-validation
   gh pr create --base main
   ```

3. **Former l'Équipe**
   - Partager ce document avec tous les développeurs
   - Organiser une session de démonstration
   - Établir les conventions de l'équipe

4. **Monitorer et Ajuster**
   - Revoir la configuration après 2 semaines
   - Ajuster le nombre d'approbations si nécessaire
   - Optimiser les workflows en fonction du feedback

### Ressources Supplémentaires

- **Documentation GitHub :** [About Protected Branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- **GitHub CLI :** [gh branch protection](https://cli.github.com/manual/gh_api)
- **SonarCloud :** [Quality Gates Documentation](https://docs.sonarcloud.io/improving/quality-gates/)
- **Git Flow :** [A Successful Git Branching Model](https://nvie.com/posts/a-successful-git-branching-model/)

---

**Document rédigé le :** 2024-12-15
**Version :** 1.0
**Auteur :** Documentation Safe-Zone CI/CD
**Prochaine révision :** 2025-01-15
