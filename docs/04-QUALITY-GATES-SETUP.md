# Guide Complet : Configuration des Quality Gates SonarQube/SonarCloud

## Table des Matières
1. [Qu'est-ce qu'un Quality Gate ?](#quest-ce-quun-quality-gate)
2. [Conditions Recommandées avec Justifications](#conditions-recommandées-avec-justifications)
3. [Configuration via Interface Web](#configuration-via-interface-web)
4. [Configuration via API](#configuration-via-api)
5. [Définition du "New Code"](#définition-du-new-code)
6. [Comment Tester les Quality Gates](#comment-tester-les-quality-gates)
7. [Quality Gates Avancés](#quality-gates-avancés)
8. [Dépannage](#dépannage)

---

## 1. Qu'est-ce qu'un Quality Gate ?

### Définition

Un **Quality Gate** (porte de qualité) est un ensemble de **conditions** que votre code doit respecter pour être considéré comme "acceptable" pour la production. C'est un mécanisme de contrôle qualité automatique qui évalue votre code à chaque analyse SonarQube/SonarCloud.

### Fonctionnement

```
Code committé et pushé
       ↓
GitHub Actions déclenche le build
       ↓
Tests unitaires exécutés
       ↓
JaCoCo génère le rapport de couverture
       ↓
Maven envoie le code + rapport à SonarCloud
       ↓
SonarCloud analyse le code (bugs, smells, vulnérabilités)
       ↓
SonarCloud évalue les métriques contre les conditions du Quality Gate
       ↓
    ┌──────────────┐
    │ Quality Gate │
    │   PASSED ?   │
    └──────┬───────┘
           │
     ┌─────┴─────┐
     │           │
    PASSED      FAILED
     │           │
     ↓           ↓
Merge autorisé  Merge bloqué
```

### Statuts Possibles

#### 1. PASSED (Vert)
```
✅ All conditions met
Quality Gate status: PASSED
```
- Toutes les conditions sont respectées
- Le code est de bonne qualité
- Le merge peut être effectué (si GitHub Checks configuré)

#### 2. FAILED (Rouge)
```
❌ Quality Gate failed
1 condition(s) failed:
  - Coverage on New Code is 45.2% (required ≥ 70%)
```
- Au moins une condition n'est pas respectée
- Le code nécessite des améliorations
- Le merge est bloqué (si GitHub Branch Protection activée)

#### 3. WARNING (Orange - SonarQube seulement)
```
⚠️ Quality Gate passed with warnings
1 condition(s) on warning:
  - Code Smells on New Code: 15 (threshold: 10)
```
- Conditions en warning, mais pas bloquantes
- SonarCloud n'a pas de status WARNING (seulement PASSED/FAILED)

### Pourquoi c'est Important ?

#### 1. Prévention de la Dette Technique
```
Sans Quality Gate :
  Développeur A commit du code avec 30% de couverture
  → Merge direct
  → Dette technique s'accumule
  → 6 mois plus tard : code impossible à maintenir

Avec Quality Gate :
  Développeur A commit du code avec 30% de couverture
  → Quality Gate FAILED (< 70%)
  → Merge bloqué
  → Développeur ajoute des tests
  → 75% de couverture → Merge OK
  → Code maintenable long terme
```

#### 2. Standards Uniformes
- Toute l'équipe respecte les mêmes standards
- Pas de "passes-droit" pour certains développeurs
- Qualité cohérente sur tout le projet

#### 3. Feedback Immédiat
- Développeur sait immédiatement si son code est acceptable
- Pas besoin d'attendre une revue manuelle
- Corrections faites avant le merge (moins coûteux)

#### 4. Documentation Visuelle
```markdown
# README.md

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=key&metric=alert_status)](https://sonarcloud.io/dashboard?id=key)

Badge affiché : [Quality Gate PASSED] (vert)
→ Signal de confiance pour les utilisateurs/recruteurs
```

---

## 2. Conditions Recommandées avec Justifications

### Vue d'Ensemble

Un Quality Gate contient plusieurs **conditions** (critères) évaluant différents aspects du code :

| Catégorie | Exemple Metric | Seuil Typique |
|-----------|----------------|---------------|
| **Fiabilité** | Bugs sur nouveau code | 0 |
| **Sécurité** | Vulnérabilités | 0 |
| **Maintenabilité** | Code Smells | < 5 |
| **Couverture** | Coverage sur nouveau code | ≥ 70% |
| **Duplication** | Duplicated Lines (%) | < 3% |
| **Complexité** | Cognitive Complexity | < 15 |

### Quality Gate Recommandé pour Projet École

#### Configuration Niveau "Stricte mais Réaliste"

### Condition 1 : Coverage sur Nouveau Code ≥ 70%

**Métrique :** `Coverage on New Code`
**Opérateur :** `is less than`
**Valeur seuil :** `70.0%`

**Justification :**
- **70%** est un bon équilibre entre qualité et pragmatisme
- **80-90%** serait idéal mais difficile à atteindre systématiquement
- **60%** est trop laxiste (laisse trop de code non testé)

**Pourquoi "Nouveau Code" et pas "Overall Code" ?**
```
Overall Code : Évalue tout le code (ancien + nouveau)
→ Problème : Si votre code existant a 40% de couverture, vous ne pourrez
  jamais passer le Quality Gate même si votre nouveau code a 90%

New Code : Évalue seulement le code ajouté/modifié dans cette PR
→ Solution : Vous pouvez améliorer progressivement sans être bloqué par l'ancien code
```

**Impact sur le Projet :**
```java
// Exemple : Vous ajoutez cette nouvelle classe
public class OrderService {
    public Order createOrder(User user, List<Product> products) {
        // 10 lignes de code
    }

    public void cancelOrder(String orderId) {
        // 5 lignes de code
    }
}

// Tests requis pour passer le Quality Gate :
// 70% de 15 lignes = 10.5 lignes doivent être couvertes
// → Vous devez tester au moins createOrder() complètement
```

**Exceptions :**
- DTOs (Data Transfer Objects) : Exclus via `sonar.exclusions`
- Configuration classes : Exclus
- Application.java (main) : Exclu

### Condition 2 : Bugs sur Nouveau Code = 0

**Métrique :** `Bugs on New Code`
**Opérateur :** `is greater than`
**Valeur seuil :** `0`

**Justification :**
- **Tolérance zéro** pour les bugs
- Un bug trouvé par SonarCloud = probable bug en production
- Correction immédiate moins coûteuse que debug en production

**Types de Bugs Détectés :**

```java
// Bug 1 : NullPointerException potentiel
public String getUserEmail(User user) {
    return user.getEmail().toLowerCase();  // ❌ user peut être null
}
// Fix :
public String getUserEmail(User user) {
    return user != null ? user.getEmail().toLowerCase() : null;  // ✅
}

// Bug 2 : Resource leak
public void readFile(String path) {
    FileInputStream fis = new FileInputStream(path);
    // ... lecture
    // ❌ Pas de fis.close()
}
// Fix :
public void readFile(String path) {
    try (FileInputStream fis = new FileInputStream(path)) {  // ✅ try-with-resources
        // ... lecture
    }
}

// Bug 3 : Condition toujours vraie
public boolean isValid(int age) {
    if (age > 0 || age < 0) {  // ❌ Toujours vrai (sauf age == 0)
        return true;
    }
    return false;
}
```

**Impact :**
```
Développeur commit du code avec un NullPointerException potentiel
→ SonarCloud détecte le bug
→ Quality Gate FAILED
→ GitHub Checks bloque le merge
→ Développeur corrige le bug
→ Nouveau commit
→ Quality Gate PASSED
→ Merge autorisé
```

### Condition 3 : Vulnérabilités sur Nouveau Code = 0

**Métrique :** `Vulnerabilities on New Code`
**Opérateur :** `is greater than`
**Valeur seuil :** `0`

**Justification :**
- **Tolérance zéro** pour les failles de sécurité
- Vulnérabilité = porte d'entrée pour attaquants
- Projet école = apprentissage des bonnes pratiques de sécurité

**Types de Vulnérabilités Détectées :**

```java
// Vulnérabilité 1 : SQL Injection
public User getUser(String userId) {
    String query = "SELECT * FROM users WHERE id = '" + userId + "'";  // ❌
    return jdbcTemplate.queryForObject(query, User.class);
}
// Fix :
public User getUser(String userId) {
    String query = "SELECT * FROM users WHERE id = ?";  // ✅ Prepared statement
    return jdbcTemplate.queryForObject(query, User.class, userId);
}

// Vulnérabilité 2 : Hardcoded password
public void connect() {
    String password = "hardcodedPassword";  // ❌ Mot de passe en clair
    database.connect("admin", password);
}
// Fix :
public void connect() {
    String password = System.getenv("DB_PASSWORD");  // ✅ Variable d'environnement
    database.connect("admin", password);
}

// Vulnérabilité 3 : Weak cryptography
public String hash(String password) {
    return md5(password);  // ❌ MD5 est cassé
}
// Fix :
public String hash(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());  // ✅ BCrypt
}
```

**Impact :**
```
CVSS Score (Common Vulnerability Scoring System) :
- 9.0-10.0 : Critical
- 7.0-8.9  : High
- 4.0-6.9  : Medium
- 0.1-3.9  : Low

Quality Gate bloque si CVSS ≥ 1.0 (toute vulnérabilité)
```

### Condition 4 : Security Hotspots Reviewed = 100%

**Métrique :** `Security Hotspots Reviewed`
**Opérateur :** `is less than`
**Valeur seuil :** `100%`

**Justification :**
- Les Security Hotspots sont des zones de code "suspectes" mais pas forcément vulnérables
- Nécessitent une revue manuelle du développeur
- 100% = Tous les hotspots ont été examinés (et marqués comme safe ou fixed)

**Qu'est-ce qu'un Security Hotspot ?**

```java
// Hotspot 1 : Utilisation de Random (pas cryptographiquement sécurisé)
public String generateToken() {
    Random random = new Random();  // ⚠️ Hotspot
    return String.valueOf(random.nextInt());
}
// Revue : Est-ce un token de sécurité ou juste un ID ?
// → Si token de sécurité : Utiliser SecureRandom
// → Si juste un ID : Marquer comme "Safe"

// Hotspot 2 : Cookie sans flag HttpOnly
response.addCookie(new Cookie("sessionId", token));  // ⚠️ Hotspot
// Revue : Le cookie contient-il des données sensibles ?
// → Si oui : Ajouter httpOnly = true
// → Si non : Marquer comme "Safe"
```

**Processus de Review :**
```
1. Developer commit du code
2. SonarCloud détecte un hotspot
3. Developer va sur SonarCloud dashboard
4. Clique sur le hotspot
5. Lit la recommandation
6. Options :
   - "Fixed" : J'ai corrigé le problème
   - "Safe" : Ce n'est pas un problème dans ce contexte
   - "Acknowledge" : Je suis au courant, je fixerai plus tard
7. Quality Gate vérifie que 100% des hotspots sont reviewed
```

### Condition 5 : Code Smells sur Nouveau Code ≤ 5

**Métrique :** `Code Smells on New Code`
**Opérateur :** `is greater than`
**Valeur seuil :** `5`

**Justification :**
- **Code Smell** = Code fonctionnel mais difficile à maintenir
- 5 smells max = Accepter quelques petits problèmes sans bloquer systématiquement
- 0 smell serait trop strict (bloque pour des détails mineurs)

**Types de Code Smells :**

```java
// Smell 1 : Méthode trop longue (> 50 lignes)
public void processOrder(Order order) {
    // ... 80 lignes de code
}
// Fix : Découper en plusieurs méthodes

// Smell 2 : Trop de paramètres (> 7)
public void createUser(String name, String email, String password,
                       String address, String city, String zipCode,
                       String country, String phone) {
    // ...
}
// Fix : Utiliser un objet UserDTO

// Smell 3 : Complexité cognitive élevée
public boolean isEligible(User user) {
    if (user.getAge() > 18) {
        if (user.hasLicense()) {
            if (user.getScore() > 500) {
                if (user.isVerified()) {
                    return true;
                }
            }
        }
    }
    return false;
}
// Fix : Utiliser des early returns ou extraire la logique

// Smell 4 : Variable non utilisée
public void calculate() {
    int result = 10 + 5;  // ❌ result jamais utilisé
    System.out.println("Done");
}
// Fix : Supprimer la variable

// Smell 5 : Code dupliqué
public void sendEmailToAdmin() {
    Email email = new Email();
    email.setTo("admin@example.com");
    email.setSubject("Alert");
    email.send();
}
public void sendEmailToUser(String userEmail) {
    Email email = new Email();
    email.setTo(userEmail);
    email.setSubject("Alert");
    email.send();
}
// Fix : Créer une méthode sendEmail(String to)
```

**Sévérité des Code Smells :**
```
Blocker    : Doit être fixé immédiatement (rare)
Critical   : Sérieux problème de maintenabilité
Major      : Problème significatif (majorité des smells)
Minor      : Petit problème, amélioration recommandée
Info       : Suggestion, pas vraiment un problème
```

### Condition 6 : Duplicated Lines < 3%

**Métrique :** `Duplicated Lines (%)`
**Opérateur :** `is greater than`
**Valeur seuil :** `3.0%`

**Justification :**
- **Duplication = Dette technique**
- Si un bug est trouvé dans le code dupliqué, il faut le fixer à plusieurs endroits
- 3% = Tolérance pour quelques duplications légitimes (DTOs, configs)

**Détection de Duplication :**

```java
// Exemple : Code dupliqué

// UserController.java
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody UserDTO dto) {
    if (dto.getName() == null || dto.getName().isEmpty()) {
        throw new BadRequestException("Name is required");
    }
    if (dto.getEmail() == null || !dto.getEmail().contains("@")) {
        throw new BadRequestException("Invalid email");
    }
    User user = userService.create(dto);
    return ResponseEntity.ok(user);
}

// ProductController.java
@PostMapping("/products")
public ResponseEntity<Product> createProduct(@RequestBody ProductDTO dto) {
    if (dto.getName() == null || dto.getName().isEmpty()) {  // ← Duplication
        throw new BadRequestException("Name is required");
    }
    if (dto.getPrice() == null || dto.getPrice() <= 0) {
        throw new BadRequestException("Invalid price");
    }
    Product product = productService.create(dto);
    return ResponseEntity.ok(product);
}

// Fix : Extraire une méthode de validation réutilisable
public class ValidationUtils {
    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new BadRequestException(fieldName + " is required");
        }
    }
}
```

**Calcul du Pourcentage :**
```
Total lines of code : 1000 lignes
Duplicated lines    : 25 lignes
Duplication %       : (25 / 1000) × 100 = 2.5%

2.5% < 3% → Quality Gate PASSED ✅
```

**Exceptions :**
- DTOs avec getters/setters identiques : Acceptable
- Configurations similaires : Acceptable
- Tests avec setup similaire : Acceptable

### Condition 7 : Maintainability Rating ≥ A

**Métrique :** `Maintainability Rating on New Code`
**Opérateur :** `is worse than`
**Valeur seuil :** `A`

**Justification :**
- **Rating A** = Ratio de dette technique < 5%
- Mesure globale de la maintenabilité
- Résumé des code smells et leur impact

**Échelle de Rating :**
```
A : Technical Debt Ratio ≤ 5%     ✅ Excellent
B : Technical Debt Ratio 6-10%    ⚠️ Bon
C : Technical Debt Ratio 11-20%   ⚠️ Moyen
D : Technical Debt Ratio 21-50%   ❌ Mauvais
E : Technical Debt Ratio > 50%    ❌ Très mauvais
```

**Technical Debt Ratio (TDR) :**
```
TDR = (Effort to fix code smells / Development time) × 100

Exemple :
- Effort to fix all code smells : 2 heures
- Time to develop this code     : 40 heures
- TDR = (2 / 40) × 100 = 5%
- Rating = A ✅
```

**Comment Améliorer le Rating :**
```
1. Réduire le nombre de code smells
2. Fixer les code smells "Major" et "Critical" en priorité
3. Éviter les méthodes trop longues
4. Réduire la complexité cyclomatique
5. Éliminer le code dupliqué
```

### Condition 8 : Reliability Rating ≥ A

**Métrique :** `Reliability Rating on New Code`
**Opérateur :** `is worse than`
**Valeur seuil :** `A`

**Justification :**
- **Rating A** = 0 bugs
- Garantit la fiabilité du code
- Bugs = comportements incorrects potentiels

**Échelle :**
```
A : 0 bugs            ✅
B : ≥ 1 minor bug     ⚠️
C : ≥ 1 major bug     ❌
D : ≥ 1 critical bug  ❌
E : ≥ 1 blocker bug   ❌
```

### Condition 9 : Security Rating ≥ A

**Métrique :** `Security Rating on New Code`
**Opérateur :** `is worse than`
**Valeur seuil :** `A`

**Justification :**
- **Rating A** = 0 vulnérabilités
- Sécurité critique pour toute application
- Apprendre les bonnes pratiques de sécurité

**Échelle :**
```
A : 0 vulnerabilities                        ✅
B : ≥ 1 minor vulnerability (CVSS < 4.0)    ⚠️
C : ≥ 1 major vulnerability (CVSS 4.0-6.9)  ❌
D : ≥ 1 critical vulnerability (CVSS 7.0-8.9) ❌
E : ≥ 1 blocker vulnerability (CVSS ≥ 9.0)  ❌
```

---

## 3. Configuration via Interface Web

### Sur SonarCloud (Recommandé)

#### Étape 1 : Accéder aux Quality Gates

```
1. Aller sur https://sonarcloud.io
2. Se connecter avec GitHub
3. Sélectionner votre organisation (ex: zone01-normandie)
```

**Navigation :**
```
Dashboard → Cliquer sur "Quality Gates" dans la barre du haut

Barre de navigation :
[Projects] [Issues] [Rules] [Quality Profiles] [Quality Gates] [Administration]
                                                      ↑
                                                 Cliquer ici
```

#### Étape 2 : Choisir entre Utiliser/Créer un Quality Gate

**Option A : Utiliser le Quality Gate par Défaut (Recommandé pour Débuter)**

```
SonarCloud fournit un Quality Gate par défaut appelé "Sonar way"

Caractéristiques du "Sonar way" :
✅ Coverage on New Code ≥ 80%
✅ Duplicated Lines on New Code < 3%
✅ Maintainability Rating on New Code ≥ A
✅ Reliability Rating on New Code ≥ A
✅ Security Rating on New Code ≥ A
✅ Security Hotspots Reviewed = 100%

Actions :
1. Aller sur votre projet (ex: safe-zone_user-service)
2. Onglet "Administration" → "Quality Gate"
3. Vérifier que "Sonar way" est sélectionné
4. C'est tout ! Rien à configurer.
```

**Option B : Créer un Quality Gate Personnalisé (Recommandé pour Projet École)**

```
Raison : Le "Sonar way" exige 80% de couverture, ce qui peut être trop strict
         pour un projet école en phase de développement.

On va créer un Quality Gate plus permissif (70% de couverture).
```

#### Étape 3 : Créer un Nouveau Quality Gate

```
Page "Quality Gates"
→ Cliquer sur le bouton [Create] en haut à droite

Formulaire :
┌─────────────────────────────────────────────────┐
│ Create Quality Gate                             │
├─────────────────────────────────────────────────┤
│ Name *                                          │
│ ┌─────────────────────────────────────────────┐ │
│ │ SafeZone Quality Gate                        │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ [Create]                                        │
└─────────────────────────────────────────────────┘

Cliquer [Create]
```

**Résultat :**
```
Vous êtes redirigé vers la page du nouveau Quality Gate (vide pour l'instant)
```

#### Étape 4 : Ajouter les Conditions

**Interface :**
```
Page "SafeZone Quality Gate"
┌─────────────────────────────────────────────────┐
│ SafeZone Quality Gate                           │
│                                                  │
│ Conditions on New Code                          │
│ [Add Condition]                                 │
│                                                  │
│ (Aucune condition pour l'instant)               │
└─────────────────────────────────────────────────┘
```

**Ajouter Condition 1 : Coverage ≥ 70%**

```
1. Cliquer sur [Add Condition]

2. Formulaire s'affiche :
   Metric : [Dropdown]
   → Taper "coverage" dans la recherche
   → Sélectionner "Coverage on New Code"

   Operator : [Dropdown]
   → Sélectionner "is less than"

   Value : [Input]
   → Taper "70"

3. Cliquer [Add]
```

**Représentation visuelle du résultat :**
```
┌─────────────────────────────────────────────────┐
│ Conditions on New Code                          │
│ [Add Condition]                                 │
├─────────────────────────────────────────────────┤
│ Coverage on New Code     is less than    70.0%  │
│                                        [Delete] │
└─────────────────────────────────────────────────┘
```

**Ajouter Condition 2 : Bugs = 0**

```
[Add Condition]
Metric   : Bugs on New Code
Operator : is greater than
Value    : 0
[Add]
```

**Ajouter Condition 3 : Vulnerabilities = 0**

```
[Add Condition]
Metric   : Vulnerabilities on New Code
Operator : is greater than
Value    : 0
[Add]
```

**Ajouter Condition 4 : Security Hotspots = 100%**

```
[Add Condition]
Metric   : Security Hotspots Reviewed
Operator : is less than
Value    : 100
[Add]
```

**Ajouter Condition 5 : Code Smells ≤ 5**

```
[Add Condition]
Metric   : Code Smells on New Code
Operator : is greater than
Value    : 5
[Add]
```

**Ajouter Condition 6 : Duplication < 3%**

```
[Add Condition]
Metric   : Duplicated Lines (%) on New Code
Operator : is greater than
Value    : 3.0
[Add]
```

**Ajouter Condition 7 : Maintainability Rating ≥ A**

```
[Add Condition]
Metric   : Maintainability Rating on New Code
Operator : is worse than
Value    : A
[Add]
```

**Ajouter Condition 8 : Reliability Rating ≥ A**

```
[Add Condition]
Metric   : Reliability Rating on New Code
Operator : is worse than
Value    : A
[Add]
```

**Ajouter Condition 9 : Security Rating ≥ A**

```
[Add Condition]
Metric   : Security Rating on New Code
Operator : is worse than
Value    : A
[Add]
```

**Résultat Final :**
```
┌─────────────────────────────────────────────────────────────┐
│ SafeZone Quality Gate                                       │
│                                                              │
│ Conditions on New Code                        [Add Condition]│
├─────────────────────────────────────────────────────────────┤
│ Coverage on New Code            is less than         70.0%  │
│ Bugs on New Code                is greater than      0      │
│ Vulnerabilities on New Code     is greater than      0      │
│ Security Hotspots Reviewed      is less than         100.0  │
│ Code Smells on New Code         is greater than      5      │
│ Duplicated Lines (%)            is greater than      3.0    │
│ Maintainability Rating          is worse than        A      │
│ Reliability Rating              is worse than        A      │
│ Security Rating                 is worse than        A      │
└─────────────────────────────────────────────────────────────┘
```

#### Étape 5 : Assigner le Quality Gate aux Projets

**Pour un Projet Spécifique :**

```
1. Aller sur votre projet (ex: safe-zone_user-service)
   SonarCloud Dashboard → Projects → safe-zone_user-service

2. Cliquer sur l'onglet "Administration"
   [Project Information] [Quality Gate] [Analysis Method] [...]
                              ↑
                         Cliquer ici

3. Section "Select a quality gate"
   ○ Use the default quality gate (Sonar way)
   ● Use a specific quality gate : [Dropdown]

4. Dropdown : Sélectionner "SafeZone Quality Gate"

5. Cliquer [Save]
```

**Répéter pour les 3 autres projets :**
- safe-zone_product-service
- safe-zone_media-service
- safe-zone_frontend

**Pour Tous les Projets de l'Organisation (Alternative) :**

```
1. Retourner sur Quality Gates → SafeZone Quality Gate

2. Cliquer sur "Set as Default"
   [Set as Default]

3. Confirmation :
   "Are you sure you want to set 'SafeZone Quality Gate' as default?"
   [Confirm]

Résultat : Tous les nouveaux projets utiliseront automatiquement ce Quality Gate
```

#### Étape 6 : Vérification

```
1. Aller sur un de vos projets
2. Onglet "Overview"
3. Section "Quality Gate"

Vous devriez voir :
┌─────────────────────────────────────────────────┐
│ Quality Gate: SafeZone Quality Gate             │
│ Status: Not computed yet (no analysis)          │
└─────────────────────────────────────────────────┘

Ou si déjà une analyse :
┌─────────────────────────────────────────────────┐
│ Quality Gate: SafeZone Quality Gate             │
│ Status: PASSED ✅                                │
│ 9 conditions met                                │
└─────────────────────────────────────────────────┘
```

---

## 4. Configuration via API

### Pourquoi Utiliser l'API ?

**Cas d'usage :**
- Automatiser la création de Quality Gates pour plusieurs projets
- Reproduire la configuration sur un autre environnement
- Infrastructure as Code (IaC) : Version control de la config Quality Gate
- Scripts d'initialisation de projet

### Pré-requis

**Outils nécessaires :**
```bash
# curl (généralement pré-installé sur Linux/Mac)
curl --version

# jq (pour parser JSON) - optionnel mais utile
sudo apt install jq  # Linux
brew install jq      # macOS
```

**Token d'API :**
```
Utiliser le même token que pour les analyses (SONAR_TOKEN)
Ou créer un token dédié à l'administration :
SonarCloud → My Account → Security → Generate Token
Type : User Token
Permissions : Administer Quality Gates
```

### Script Complet : Création d'un Quality Gate

**Fichier : `scripts/setup-quality-gate.sh`**

```bash
#!/bin/bash

# Script pour créer et configurer un Quality Gate SonarCloud
# Usage: ./setup-quality-gate.sh

set -e  # Exit on error

# ============================================
# CONFIGURATION
# ============================================
SONAR_TOKEN="${SONAR_TOKEN:-your-token-here}"
SONAR_HOST_URL="https://sonarcloud.io"
QUALITY_GATE_NAME="SafeZone Quality Gate"
ORGANIZATION="zone01-normandie"

# Projets à configurer
PROJECTS=(
  "zone01-normandie_safe-zone_user-service"
  "zone01-normandie_safe-zone_product-service"
  "zone01-normandie_safe-zone_media-service"
  "zone01-normandie_safe-zone_frontend"
)

# ============================================
# FONCTIONS
# ============================================

# Fonction pour faire des requêtes à l'API SonarCloud
sonar_api() {
  local endpoint=$1
  shift
  curl -s -u "${SONAR_TOKEN}:" "${SONAR_HOST_URL}/api/${endpoint}" "$@"
}

# ============================================
# ÉTAPE 1 : Créer le Quality Gate
# ============================================
echo "Creating Quality Gate: ${QUALITY_GATE_NAME}..."

response=$(sonar_api "qualitygates/create" \
  -X POST \
  -d "name=${QUALITY_GATE_NAME}" \
  -d "organization=${ORGANIZATION}")

# Extraire l'ID du Quality Gate créé
GATE_ID=$(echo "$response" | grep -oP '"id":\s*\K[0-9]+')

if [ -z "$GATE_ID" ]; then
  echo "Error: Failed to create Quality Gate"
  echo "Response: $response"
  exit 1
fi

echo "✅ Quality Gate created with ID: ${GATE_ID}"

# ============================================
# ÉTAPE 2 : Ajouter les Conditions
# ============================================
echo "Adding conditions to Quality Gate..."

# Condition 1: Coverage on New Code >= 70%
sonar_api "qualitygates/create_condition" \
  -X POST \
  -d "gateId=${GATE_ID}" \
  -d "metric=new_coverage" \
  -d "op=LT" \
  -d "error=70" \
  > /dev/null
echo "  ✅ Coverage on New Code >= 70%"

# Condition 2: Bugs on New Code = 0
sonar_api "qualitygates/create_condition" \
  -X POST \
  -d "gateId=${GATE_ID}" \
  -d "metric=new_bugs" \
  -d "op=GT" \
  -d "error=0" \
  > /dev/null
echo "  ✅ Bugs on New Code = 0"

# Condition 3: Vulnerabilities on New Code = 0
sonar_api "qualitygates/create_condition" \
  -X POST \
  -d "gateId=${GATE_ID}" \
  -d "metric=new_vulnerabilities" \
  -d "op=GT" \
  -d "error=0" \
  > /dev/null
echo "  ✅ Vulnerabilities on New Code = 0"

# Condition 4: Security Hotspots Reviewed = 100%
sonar_api "qualitygates/create_condition" \
  -X POST \
  -d "gateId=${GATE_ID}" \
  -d "metric=new_security_hotspots_reviewed" \
  -d "op=LT" \
  -d "error=100" \
  > /dev/null
echo "  ✅ Security Hotspots Reviewed = 100%"

# Condition 5: Code Smells on New Code <= 5
sonar_api "qualitygates/create_condition" \
  -X POST \
  -d "gateId=${GATE_ID}" \
  -d "metric=new_code_smells" \
  -d "op=GT" \
  -d "error=5" \
  > /dev/null
echo "  ✅ Code Smells on New Code <= 5"

# Condition 6: Duplicated Lines < 3%
sonar_api "qualitygates/create_condition" \
  -X POST \
  -d "gateId=${GATE_ID}" \
  -d "metric=new_duplicated_lines_density" \
  -d "op=GT" \
  -d "error=3" \
  > /dev/null
echo "  ✅ Duplicated Lines < 3%"

# Condition 7: Maintainability Rating >= A
sonar_api "qualitygates/create_condition" \
  -X POST \
  -d "gateId=${GATE_ID}" \
  -d "metric=new_maintainability_rating" \
  -d "op=GT" \
  -d "error=1" \
  > /dev/null
echo "  ✅ Maintainability Rating >= A"

# Condition 8: Reliability Rating >= A
sonar_api "qualitygates/create_condition" \
  -X POST \
  -d "gateId=${GATE_ID}" \
  -d "metric=new_reliability_rating" \
  -d "op=GT" \
  -d "error=1" \
  > /dev/null
echo "  ✅ Reliability Rating >= A"

# Condition 9: Security Rating >= A
sonar_api "qualitygates/create_condition" \
  -X POST \
  -d "gateId=${GATE_ID}" \
  -d "metric=new_security_rating" \
  -d "op=GT" \
  -d "error=1" \
  > /dev/null
echo "  ✅ Security Rating >= A"

# ============================================
# ÉTAPE 3 : Assigner aux Projets
# ============================================
echo "Assigning Quality Gate to projects..."

for project in "${PROJECTS[@]}"; do
  sonar_api "qualitygates/select" \
    -X POST \
    -d "gateId=${GATE_ID}" \
    -d "projectKey=${project}" \
    -d "organization=${ORGANIZATION}" \
    > /dev/null
  echo "  ✅ ${project}"
done

# ============================================
# SUCCÈS
# ============================================
echo ""
echo "========================================="
echo "✅ Quality Gate setup complete!"
echo "========================================="
echo "Quality Gate Name: ${QUALITY_GATE_NAME}"
echo "Quality Gate ID: ${GATE_ID}"
echo "Projects configured: ${#PROJECTS[@]}"
echo ""
echo "View on SonarCloud:"
echo "${SONAR_HOST_URL}/organizations/${ORGANIZATION}/quality_gates/show/${GATE_ID}"
```

### Utilisation du Script

**Étape 1 : Rendre le script exécutable**
```bash
chmod +x scripts/setup-quality-gate.sh
```

**Étape 2 : Exporter le token**
```bash
export SONAR_TOKEN="squ_abc123xyz789..."
```

**Étape 3 : Exécuter le script**
```bash
./scripts/setup-quality-gate.sh
```

**Résultat attendu :**
```
Creating Quality Gate: SafeZone Quality Gate...
✅ Quality Gate created with ID: 12345

Adding conditions to Quality Gate...
  ✅ Coverage on New Code >= 70%
  ✅ Bugs on New Code = 0
  ✅ Vulnerabilities on New Code = 0
  ✅ Security Hotspots Reviewed = 100%
  ✅ Code Smells on New Code <= 5
  ✅ Duplicated Lines < 3%
  ✅ Maintainability Rating >= A
  ✅ Reliability Rating >= A
  ✅ Security Rating >= A

Assigning Quality Gate to projects...
  ✅ zone01-normandie_safe-zone_user-service
  ✅ zone01-normandie_safe-zone_product-service
  ✅ zone01-normandie_safe-zone_media-service
  ✅ zone01-normandie_safe-zone_frontend

=========================================
✅ Quality Gate setup complete!
=========================================
Quality Gate Name: SafeZone Quality Gate
Quality Gate ID: 12345
Projects configured: 4

View on SonarCloud:
https://sonarcloud.io/organizations/zone01-normandie/quality_gates/show/12345
```

### Référence API

**Endpoints Utilisés :**

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/qualitygates/create` | POST | Créer un Quality Gate |
| `/api/qualitygates/create_condition` | POST | Ajouter une condition |
| `/api/qualitygates/select` | POST | Assigner QG à un projet |
| `/api/qualitygates/list` | GET | Lister tous les QG |
| `/api/qualitygates/show` | GET | Détails d'un QG |
| `/api/qualitygates/destroy` | POST | Supprimer un QG |

**Métriques Disponibles :**

| Metric Key | Description |
|------------|-------------|
| `new_coverage` | Coverage on New Code |
| `new_bugs` | Bugs on New Code |
| `new_vulnerabilities` | Vulnerabilities on New Code |
| `new_security_hotspots_reviewed` | Security Hotspots Reviewed |
| `new_code_smells` | Code Smells on New Code |
| `new_duplicated_lines_density` | Duplicated Lines (%) on New Code |
| `new_maintainability_rating` | Maintainability Rating (1=A, 2=B, ...) |
| `new_reliability_rating` | Reliability Rating |
| `new_security_rating` | Security Rating |

**Opérateurs :**

| Operator | Description |
|----------|-------------|
| `LT` | is less than (< ) |
| `GT` | is greater than (>) |
| `EQ` | equals (=) |
| `NE` | not equals (≠) |

---

## 5. Définition du "New Code"

### Qu'est-ce que le "New Code" ?

Le **New Code** (nouveau code) est le code qui a été **ajouté ou modifié** depuis une référence de départ définie. C'est la portion de code sur laquelle les conditions du Quality Gate sont évaluées.

### Pourquoi Différencier New Code et Overall Code ?

**Problème avec Overall Code :**
```
Scénario :
- Vous héritez d'un projet existant avec 10,000 lignes de code
- Couverture actuelle : 30% (ancien code non testé)
- Vous ajoutez 100 lignes de nouveau code avec 90% de couverture

Calcul Overall Code Coverage :
(10000 × 0.30 + 100 × 0.90) / 10100 = 30.6%

Si Quality Gate exige 70% sur Overall Code :
❌ Quality Gate FAILED malgré votre excellent nouveau code

Résultat : Impossible de merger à cause de l'ancien code
```

**Solution avec New Code :**
```
Même scénario :
- Quality Gate évalue seulement les 100 nouvelles lignes
- Coverage on New Code : 90%

Si Quality Gate exige 70% sur New Code :
✅ Quality Gate PASSED

Résultat : Vous pouvez merger, et progressivement le Overall Code s'améliore
```

### Stratégies de Définition du New Code

SonarCloud offre plusieurs stratégies pour définir ce qui est considéré comme "nouveau" :

#### Stratégie 1 : Previous Version (Recommandée)

**Description :**
Tout code modifié depuis la dernière analyse est considéré comme nouveau.

**Fonctionnement :**
```
Analyse N-1 (hier) : Version A du code
Analyse N (aujourd'hui) : Version B du code

New Code = Différence entre Version B et Version A
```

**Avantages :**
- Simple et automatique
- Adapté à un workflow avec CI/CD continu
- Chaque commit améliore progressivement le code

**Inconvénients :**
- Si vous faites peu de commits, beaucoup de code est "nouveau" à chaque analyse
- Historique perdu si vous supprimez des branches

**Configuration :**
```
SonarCloud → Projet → Administration → New Code
→ Previous version
[Save]
```

#### Stratégie 2 : Number of Days

**Description :**
Tout code modifié dans les X derniers jours est considéré comme nouveau.

**Fonctionnement :**
```
Aujourd'hui : 2024-12-15
New Code = Lignes modifiées entre 2024-12-08 et 2024-12-15 (si 7 jours)
```

**Avantages :**
- Flexibilité (ajuster selon vos sprints)
- Idéal pour suivi par période (sprint de 2 semaines = 14 jours)

**Inconvénients :**
- Arbitraire (pourquoi 7 jours plutôt que 10 ?)
- Peut inclure du code ancien si peu d'activité

**Configuration :**
```
SonarCloud → Projet → Administration → New Code
→ Number of days : [Input: 30]
[Save]
```

**Recommandation :** 30 jours pour un projet école (entre 2 soutenances typiques)

#### Stratégie 3 : Specific Analysis

**Description :**
Tout code modifié depuis une analyse spécifique (baseline) est considéré comme nouveau.

**Fonctionnement :**
```
Baseline : Analyse du 2024-11-01 (début du projet)
New Code = Tout code écrit après le 2024-11-01
```

**Avantages :**
- Contrôle total sur le point de départ
- Idéal pour mesurer l'amélioration depuis le début du projet
- Utile pour "reset" après un gros refactoring

**Inconvénients :**
- Nécessite de choisir et noter l'analyse baseline
- Moins adapté au développement continu

**Configuration :**
```
SonarCloud → Projet → Administration → New Code
→ Specific analysis

Liste des analyses passées s'affiche :
2024-12-15 14:30 - Analysis #45
2024-12-14 10:15 - Analysis #44
2024-12-13 16:45 - Analysis #43
2024-11-01 09:00 - Analysis #1  ← Sélectionner celui-ci

[Save]
```

#### Stratégie 4 : Reference Branch

**Description :**
Tout code différent de la branche de référence (généralement `main`) est considéré comme nouveau.

**Fonctionnement :**
```
Branch main : État stable du code
Feature branch (feature/add-payment) : Votre travail en cours

New Code dans feature/add-payment = Différence avec main
```

**Avantages :**
- Adapté au workflow Git Flow / GitHub Flow
- Évalue seulement le code de votre feature
- Quality Gate vérifié avant le merge dans main

**Inconvénients :**
- Nécessite une branche de référence stable
- Conflits si main évolue rapidement

**Configuration :**
```
SonarCloud → Projet → Administration → New Code
→ Reference branch : [Dropdown]
→ Sélectionner "main"
[Save]
```

### Recommandation pour Projet École

**Configuration Recommandée :**

```
Stratégie : Previous Version

Raison :
- Simplicité (automatique)
- Adapté au workflow main-only
- Chaque push améliore le code progressivement
```

**Configuration :**
```
Pour les 4 projets (user-service, product-service, media-service, frontend) :

1. SonarCloud → Projet → Administration → New Code
2. Sélectionner "Previous version"
3. [Save]
4. Répéter pour les 3 autres projets
```

### Visualisation du New Code dans SonarCloud

**Onglet "Overall Code" vs "New Code" :**

```
SonarCloud Dashboard → Projet → Overview

Vous voyez 2 sections :

┌─────────────────────────────────────────────────┐
│ Overall Code                                    │
├─────────────────────────────────────────────────┤
│ Bugs               : 2                          │
│ Vulnerabilities    : 0                          │
│ Code Smells        : 45                         │
│ Coverage           : 42.3%                      │
│ Duplications       : 5.2%                       │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ New Code (since previous analysis)              │
├─────────────────────────────────────────────────┤
│ Bugs               : 0                          │
│ Vulnerabilities    : 0                          │
│ Code Smells        : 3                          │
│ Coverage           : 85.7%                      │
│ Duplications       : 1.2%                       │
│                                                  │
│ Quality Gate       : PASSED ✅                   │
└─────────────────────────────────────────────────┘

Quality Gate évalue seulement la section "New Code"
```

---

## 6. Comment Tester les Quality Gates

### Test 1 : Scénario de Succès (Quality Gate PASSED)

**Objectif :** Vérifier que le Quality Gate passe avec du bon code

**Étape 1 : Créer une Feature Branch**
```bash
git checkout -b test/quality-gate-pass
```

**Étape 2 : Ajouter du Code Bien Testé**

**Fichier : `backend/user-service/src/main/java/com/ecommerce/user/util/EmailValidator.java`**
```java
package com.ecommerce.user.util;

public class EmailValidator {

    public boolean isValid(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
```

**Fichier : `backend/user-service/src/test/java/com/ecommerce/user/util/EmailValidatorTest.java`**
```java
package com.ecommerce.user.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {

    private final EmailValidator validator = new EmailValidator();

    @Test
    void testValidEmail() {
        assertTrue(validator.isValid("test@example.com"));
        assertTrue(validator.isValid("user+tag@domain.co.uk"));
    }

    @Test
    void testInvalidEmail() {
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid("invalid"));
        assertFalse(validator.isValid("@example.com"));
        assertFalse(validator.isValid("test@"));
    }

    @Test
    void testNormalize() {
        assertEquals("test@example.com", validator.normalize("  Test@Example.COM  "));
        assertNull(validator.normalize(null));
    }
}
```

**Coverage attendue : 100% (toutes les lignes testées)**

**Étape 3 : Commit et Push**
```bash
git add .
git commit -m "test: add EmailValidator with full coverage"
git push origin test/quality-gate-pass
```

**Étape 4 : Créer une Pull Request**
```
GitHub → Pull requests → New pull request
base: main ← compare: test/quality-gate-pass
→ Create pull request
```

**Étape 5 : Attendre les Checks**
```
GitHub Actions démarre automatiquement
→ Build et tests (2-3 minutes)
→ SonarCloud analyse (1 minute)
→ Quality Gate évaluation
```

**Résultat Attendu :**
```
Pull Request Checks:

✅ build-user-service (3m 25s)
✅ sonarcloud-quality-gate (45s)

Détail SonarCloud :
┌─────────────────────────────────────────────────┐
│ Quality Gate: PASSED ✅                          │
│                                                  │
│ New Code Analysis:                              │
│ • Coverage: 100.0% (≥ 70%) ✅                    │
│ • Bugs: 0 ✅                                     │
│ • Vulnerabilities: 0 ✅                          │
│ • Security Hotspots: N/A ✅                      │
│ • Code Smells: 0 ✅                              │
│ • Duplications: 0.0% ✅                          │
│ • Maintainability Rating: A ✅                   │
│ • Reliability Rating: A ✅                       │
│ • Security Rating: A ✅                          │
└─────────────────────────────────────────────────┘

[Merge pull request] ← Bouton activé
```

### Test 2 : Scénario d'Échec (Quality Gate FAILED) - Coverage Insuffisante

**Objectif :** Vérifier que le Quality Gate bloque du code mal testé

**Étape 1 : Créer une Feature Branch**
```bash
git checkout -b test/quality-gate-fail-coverage
```

**Étape 2 : Ajouter du Code Sans Tests**

**Fichier : `backend/user-service/src/main/java/com/ecommerce/user/util/PhoneValidator.java`**
```java
package com.ecommerce.user.util;

public class PhoneValidator {

    public boolean isValid(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        // Valide format international : +33612345678
        return phone.matches("^\\+[1-9]\\d{1,14}$");
    }

    public String format(String phone) {
        // Formate en groupes de 2 : +33 6 12 34 56 78
        if (phone == null || phone.length() < 3) {
            return phone;
        }
        StringBuilder formatted = new StringBuilder();
        formatted.append(phone.substring(0, 3)).append(" ");
        for (int i = 3; i < phone.length(); i += 2) {
            formatted.append(phone.substring(i, Math.min(i + 2, phone.length())));
            if (i + 2 < phone.length()) {
                formatted.append(" ");
            }
        }
        return formatted.toString();
    }
}
```

**Fichier de test VIDE : `backend/user-service/src/test/java/com/ecommerce/user/util/PhoneValidatorTest.java`**
```java
package com.ecommerce.user.util;

import org.junit.jupiter.api.Test;

class PhoneValidatorTest {
    // Aucun test !
}
```

**Coverage attendue : 0%**

**Étape 3 : Commit et Push**
```bash
git add .
git commit -m "test: add PhoneValidator without tests (should fail QG)"
git push origin test/quality-gate-fail-coverage
```

**Étape 4 : Créer une Pull Request et Observer**

**Résultat Attendu :**
```
Pull Request Checks:

✅ build-user-service (3m 25s)  # Build OK
❌ sonarcloud-quality-gate (45s)  # Quality Gate FAILED

Détail SonarCloud :
┌─────────────────────────────────────────────────┐
│ Quality Gate: FAILED ❌                          │
│                                                  │
│ 1 condition failed:                             │
│ ❌ Coverage on New Code: 0.0% (required ≥ 70%)  │
│                                                  │
│ Other conditions:                               │
│ ✅ Bugs: 0                                       │
│ ✅ Vulnerabilities: 0                            │
│ ✅ Code Smells: 2 (≤ 5)                          │
│ ... (autres conditions OK)                      │
└─────────────────────────────────────────────────┘

[Merge pull request] ← Bouton DÉSACTIVÉ (si branch protection)

Commentaire automatique de SonarCloud dans la PR :
"Quality Gate failed for this pull request.
- Coverage on New Code dropped to 0.0% (required ≥ 70%)
View details on SonarCloud: [lien]"
```

**Étape 5 : Corriger le Code**

**Ajouter les tests manquants :**
```java
// PhoneValidatorTest.java
package com.ecommerce.user.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhoneValidatorTest {

    private final PhoneValidator validator = new PhoneValidator();

    @Test
    void testValidPhone() {
        assertTrue(validator.isValid("+33612345678"));
        assertTrue(validator.isValid("+1234567890"));
    }

    @Test
    void testInvalidPhone() {
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid("0612345678"));  // Pas de +
        assertFalse(validator.isValid("+33abc"));      // Lettres
    }

    @Test
    void testFormat() {
        assertEquals("+33 6 12 34 56 78", validator.format("+33612345678"));
        assertNull(validator.format(null));
    }
}
```

**Commit et Push :**
```bash
git add .
git commit -m "test: add tests for PhoneValidator (coverage now 100%)"
git push
```

**Résultat :**
```
GitHub Actions re-déclenche automatiquement

Nouveau résultat :
✅ sonarcloud-quality-gate

Commentaire SonarCloud mis à jour :
"Quality Gate now passes! ✅
- Coverage on New Code: 100.0%"

[Merge pull request] ← Bouton ACTIVÉ
```

### Test 3 : Scénario d'Échec - Bugs Détectés

**Code avec Bug :**
```java
public class OrderCalculator {
    public double calculateTotal(List<OrderItem> items) {
        double total = 0;
        for (OrderItem item : items) {  // ❌ items peut être null → NullPointerException
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }
}
```

**SonarCloud détecte :**
```
❌ Bug: NullPointerException possible si items est null
Severity: Major
Type: Reliability

Quality Gate FAILED:
❌ Bugs on New Code: 1 (required = 0)
```

**Fix :**
```java
public double calculateTotal(List<OrderItem> items) {
    if (items == null || items.isEmpty()) {  // ✅ Vérification null
        return 0.0;
    }
    double total = 0;
    for (OrderItem item : items) {
        total += item.getPrice() * item.getQuantity();
    }
    return total;
}
```

### Test 4 : Scénario d'Échec - Duplication

**Code dupliqué :**
```java
// UserController.java
public void validateUser(UserDTO dto) {
    if (dto.getName() == null || dto.getName().length() < 3) {
        throw new ValidationException("Name too short");
    }
    if (dto.getEmail() == null || !dto.getEmail().contains("@")) {
        throw new ValidationException("Invalid email");
    }
}

// ProductController.java
public void validateProduct(ProductDTO dto) {
    if (dto.getName() == null || dto.getName().length() < 3) {  // ← DUPLICATION
        throw new ValidationException("Name too short");
    }
    if (dto.getPrice() == null || dto.getPrice() <= 0) {
        throw new ValidationException("Invalid price");
    }
}
```

**SonarCloud détecte :**
```
⚠️ Duplication: 4 lignes dupliquées dans UserController et ProductController
Duplication rate: 3.5%

Quality Gate FAILED:
❌ Duplicated Lines: 3.5% (required < 3.0%)
```

**Fix : Extraire la logique commune**
```java
// ValidationUtils.java
public class ValidationUtils {
    public static void validateName(String name) {
        if (name == null || name.length() < 3) {
            throw new ValidationException("Name too short");
        }
    }
}

// UserController.java
public void validateUser(UserDTO dto) {
    ValidationUtils.validateName(dto.getName());  // ✅ Réutilisation
    if (dto.getEmail() == null || !dto.getEmail().contains("@")) {
        throw new ValidationException("Invalid email");
    }
}

// ProductController.java
public void validateProduct(ProductDTO dto) {
    ValidationUtils.validateName(dto.getName());  // ✅ Réutilisation
    if (dto.getPrice() == null || dto.getPrice() <= 0) {
        throw new ValidationException("Invalid price");
    }
}
```

---

## 7. Quality Gates Avancés

### Conditions Additionnelles (Optionnelles)

#### Condition : Lines of Code sur Nouveau Code

**Métrique :** `new_lines`
**Utilité :** Limiter la taille des Pull Requests

**Configuration :**
```
Metric   : Lines of Code on New Code
Operator : is greater than
Value    : 500

Raison : PRs > 500 lignes sont difficiles à review
→ Encourager des PRs plus petites et focalisées
```

#### Condition : Cognitive Complexity

**Métrique :** `new_cognitive_complexity`
**Utilité :** Limiter la complexité du code

**Configuration :**
```
Metric   : Cognitive Complexity on New Code
Operator : is greater than
Value    : 15

Raison : Code trop complexe = difficile à comprendre et tester
```

**Exemple de complexité élevée :**
```java
// Cognitive Complexity: 12
public boolean checkEligibility(User user, Product product) {
    if (user != null) {                          // +1
        if (user.getAge() >= 18) {               // +2 (nested)
            if (user.isVerified()) {             // +3 (nested)
                if (product.isAvailable()) {     // +4 (nested)
                    if (product.getStock() > 0) {  // +5 (nested)
                        return true;
                    }
                }
            }
        }
    }
    return false;
}

// Fix avec early returns (Cognitive Complexity: 5)
public boolean checkEligibility(User user, Product product) {
    if (user == null) return false;              // +1
    if (user.getAge() < 18) return false;        // +1
    if (!user.isVerified()) return false;        // +1
    if (!product.isAvailable()) return false;    // +1
    return product.getStock() > 0;               // +1
}
```

### Quality Gates par Environnement

**Scénario :** Conditions plus strictes pour production, plus permissives pour développement

**Quality Gate "Dev" :**
```
Coverage on New Code >= 60%
Bugs = 0
Vulnerabilities = 0
Code Smells <= 10
```

**Quality Gate "Production" :**
```
Coverage on New Code >= 80%
Bugs = 0
Vulnerabilities = 0
Code Smells <= 3
Duplication < 2%
```

**Configuration :**
```
Branche develop → Quality Gate "Dev"
Branche main → Quality Gate "Production"

SonarCloud → Projet → Administration → New Code
→ Configure different gates per branch
```

### Quality Gate avec Warnings (SonarQube Self-Hosted Only)

**Note :** SonarCloud ne supporte pas le status WARNING, seulement PASSED/FAILED.

**SonarQube Self-Hosted :**
```
Condition : Code Smells on New Code
Error threshold   : 10  (> 10 → FAILED)
Warning threshold : 5   (5-10 → WARNING)
```

**Résultat :**
```
Code Smells = 3  → PASSED ✅
Code Smells = 7  → WARNING ⚠️ (merge autorisé mais attention requise)
Code Smells = 12 → FAILED ❌ (merge bloqué)
```

---

## 8. Dépannage

### Problème 1 : Quality Gate Toujours "Not Computed"

**Symptôme :**
```
SonarCloud Dashboard → Projet → Overview
Quality Gate: Not computed
```

**Causes possibles :**

#### Cause A : Aucune Analyse Effectuée

**Vérification :**
```
SonarCloud → Projet → Activity
→ Liste des analyses

Si liste vide : Aucune analyse n'a été faite
```

**Solution :**
```
1. Vérifier que GitHub Actions a bien exécuté le workflow
2. Vérifier les logs du step "SonarCloud Scan"
3. Chercher des erreurs dans les logs
4. Re-déclencher le workflow manuellement
```

#### Cause B : Analyse Échouée

**Vérification :**
```
GitHub Actions → Workflow run → Step "SonarCloud Scan"
Chercher des erreurs comme :
[ERROR] Failed to execute goal sonar:sonar
```

**Solutions communes :**
- Token invalide : Vérifier SONAR_TOKEN
- Projet non trouvé : Vérifier sonar.projectKey dans pom.xml
- Organization incorrecte : Vérifier sonar.organization

### Problème 2 : Quality Gate Évalue l'Overall Code au lieu du New Code

**Symptôme :**
```
Quality Gate FAILED
❌ Coverage: 45% (required ≥ 70%)

Mais mon nouveau code a 90% de couverture !
```

**Cause :**
Les conditions sont configurées sur "Overall Code" au lieu de "New Code"

**Vérification :**
```
SonarCloud → Quality Gates → Votre Quality Gate
→ Conditions

Vérifier que chaque condition dit "on New Code" :
✅ Coverage on New Code
❌ Coverage (Overall)  ← Mauvais
```

**Solution :**
```
1. Supprimer les conditions "Overall"
2. Re-créer les conditions avec "on New Code"
3. Exemple :
   Metric : Coverage on New Code  ← Important !
   Operator : is less than
   Value : 70
```

### Problème 3 : Quality Gate Pass alors qu'il devrait Fail

**Symptôme :**
```
Vous ajoutez du code avec 0% de couverture
Mais Quality Gate PASSED ✅

Pourquoi ?
```

**Causes possibles :**

#### Cause A : Code Exclu de l'Analyse

**Vérification :**
```
Fichier pom.xml :
<sonar.exclusions>**/dto/**,**/entity/**,**/config/**</sonar.exclusions>

Si votre nouveau code est dans un package "dto/" :
→ Il est exclu de l'analyse
→ Pas compté dans la couverture
→ Quality Gate n'évalue pas ce code
```

**Solution :**
```
Soit :
1. Déplacer le code hors des packages exclus
2. Ou enlever l'exclusion si c'est du vrai code métier
```

#### Cause B : New Code Period Mal Configuré

**Vérification :**
```
SonarCloud → Projet → Administration → New Code
→ Vérifier la stratégie

Si "Number of days: 365" :
→ Seulement le code modifié dans les 365 derniers jours est "nouveau"
→ Si votre projet a < 365 jours, TOUT est "nouveau" (dilution)
```

**Solution :**
```
Changer pour "Previous version"
```

### Problème 4 : Merge Autorisé Malgré Quality Gate FAILED

**Symptôme :**
```
Quality Gate FAILED ❌
Mais le bouton "Merge pull request" est actif
```

**Cause :**
Branch Protection Rules pas configurées

**Solution :**
```
GitHub → safe-zone → Settings → Branches
→ Branch protection rules → main → Edit

Ajouter :
☑ Require status checks to pass before merging
  Status checks required :
    ☑ sonarcloud
    ☑ sonarcloud-quality-gate (si check séparé)

☑ Require branches to be up to date

[Save changes]
```

**Test :**
```
Créer une nouvelle PR avec Quality Gate FAILED
→ Bouton "Merge" doit être grisé avec message :
   "Merging is blocked: Required status check has failed"
```

### Problème 5 : Security Hotspots Non Reviewed Bloque le QG

**Symptôme :**
```
Quality Gate FAILED
❌ Security Hotspots Reviewed: 0% (required 100%)

Vous voyez 3 Security Hotspots sur SonarCloud
```

**Solution :**
```
1. Aller sur SonarCloud → Projet → Security Hotspots

2. Pour chaque hotspot :
   - Cliquer dessus pour voir les détails
   - Lire la recommandation SonarCloud
   - Décider de l'action :

   Option A : Le code est sûr dans ce contexte
   → Cliquer [Change status] → "Safe"
   → Ajouter un commentaire expliquant pourquoi

   Option B : C'est un vrai problème de sécurité
   → Fixer le code
   → Commit et push
   → SonarCloud ré-analyse
   → Hotspot disparaît automatiquement
   → Cliquer [Change status] → "Fixed"

3. Quand tous les hotspots sont reviewés :
   Security Hotspots Reviewed: 100% ✅
```

---

## Résumé : Checklist Quality Gates

### Configuration Initiale

- [ ] Créer un Quality Gate personnalisé "SafeZone Quality Gate"
- [ ] Ajouter les 9 conditions recommandées
- [ ] Assigner le Quality Gate aux 4 projets
- [ ] Configurer "New Code" sur "Previous version"
- [ ] Activer Branch Protection sur main avec status check "sonarcloud"

### Tests de Validation

- [ ] Test 1 : Créer du code avec 100% couverture → QG doit PASSER
- [ ] Test 2 : Créer du code sans tests → QG doit ÉCHOUER
- [ ] Test 3 : Introduire un bug → QG doit ÉCHOUER
- [ ] Test 4 : Dupliquer du code (> 3%) → QG doit ÉCHOUER
- [ ] Test 5 : Vérifier que le merge est bloqué si QG FAILED

### Maintenance

- [ ] Documenter les conditions dans README.md ou CONTRIBUTING.md
- [ ] Former l'équipe sur comment lire les résultats SonarCloud
- [ ] Réviser les seuils après 1 mois (trop strict ? trop laxiste ?)
- [ ] Monitorer le Technical Debt Ratio mensuel

---

**Document créé le** : 2025-12-15
**Auteur** : Documentation CI/CD Zone01
**Version** : 1.0
**Statut** : Complet
