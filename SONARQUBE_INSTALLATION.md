# Installation SonarQube - Récapitulatif

## 🎉 Installation terminée avec succès !

L'infrastructure SonarQube a été configurée pour votre projet e-commerce.

---

## 📁 Fichiers créés

### Configuration Docker
- `docker-compose.sonarqube.yml` - Configuration Docker pour SonarQube + PostgreSQL
- `.env.example` - Template des variables d'environnement
- `.gitignore` - Configuration Git mise à jour

### Scripts
- `scripts/start-sonarqube.sh` - Script de démarrage Linux/Mac
- `scripts/start-sonarqube.bat` - Script de démarrage Windows
- `scripts/stop-sonarqube.sh` - Script d'arrêt Linux/Mac
- `scripts/stop-sonarqube.bat` - Script d'arrêt Windows

### Documentation
- `docs/sonarqube-setup.md` - Documentation complète et détaillée
- `docs/sonarqube-quick-start.md` - Guide de démarrage rapide
- `docs/sonarqube-maven-config.md` - Configuration Maven pour les microservices
- `docs/sonarqube-angular-config.md` - Configuration Angular pour le frontend
- `SONARQUBE_INSTALLATION.md` - Ce fichier (récapitulatif)

---

## 🚀 Démarrage rapide

### 1. Créer le fichier .env (première fois)

```bash
cp .env.example .env
```

### 2. Démarrer SonarQube

**Linux/Mac :**
```bash
./scripts/start-sonarqube.sh
```

**Windows :**
```cmd
.\scripts\start-sonarqube.bat
```

### 3. Se connecter

- **URL** : http://localhost:9000
- **Username** : `admin`
- **Password** : `admin`

⚠️ **IMPORTANT** : Changez le mot de passe lors de la première connexion !

---

## 📊 Architecture

```
Infrastructure e-commerce (existante)
├── MongoDB (port 27017)          → Base de données du projet
├── Kafka + Zookeeper             → Messagerie
├── User Service (port 8081)
├── Product Service (port 8082)
├── Media Service (port 8083)
└── Frontend Angular (port 8084)

Infrastructure SonarQube (nouvelle)
├── PostgreSQL (interne)          → Base de données SonarQube uniquement
└── SonarQube (port 9000)         → Analyse de qualité du code
```

**Note importante** : PostgreSQL est utilisé **uniquement pour SonarQube** (contrainte technique). MongoDB reste la base de données de votre application e-commerce.

---

## ⚙️ Configuration système requise (Linux)

**Avant le premier démarrage**, configurez le système :

```bash
# Augmenter les limites (obligatoire pour Elasticsearch/SonarQube)
sudo sysctl -w vm.max_map_count=262144

# Pour rendre permanent
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

**Windows (WSL2)** : Voir la documentation complète.

---

## 📝 Prochaines étapes

### 1. Générer un token d'authentification

1. Connectez-vous à http://localhost:9000
2. **My Account** (avatar) > **Security**
3. **Generate Token**
   - Name : `ecommerce-analysis`
   - Type : `Global Analysis Token`
4. Copiez le token et ajoutez-le à `.env` :
   ```bash
   SONAR_TOKEN=votre_token_ici
   ```

### 2. Créer les projets dans SonarQube

Dans l'interface web, créez ces projets :

| Project Key | Display Name |
|------------|--------------|
| `ecommerce-user-service` | E-Commerce User Service |
| `ecommerce-product-service` | E-Commerce Product Service |
| `ecommerce-media-service` | E-Commerce Media Service |
| `ecommerce-frontend` | E-Commerce Frontend |

### 3. Configurer les projets

#### Backend (Maven)

Ajoutez la configuration SonarQube à chaque `pom.xml` des microservices.

**Voir le guide détaillé** : `docs/sonarqube-maven-config.md`

#### Frontend (Angular)

1. Créez le fichier `frontend/sonar-project.properties`
2. Installez le scanner : `npm install --save-dev sonarqube-scanner`

**Voir le guide détaillé** : `docs/sonarqube-angular-config.md`

### 4. Lancer votre première analyse

**Backend (exemple avec user-service) :**
```bash
cd backend/user-service
mvn clean verify sonar:sonar -Dsonar.token=VOTRE_TOKEN
```

**Frontend :**
```bash
cd frontend
npm run test -- --code-coverage --watch=false
npx sonar-scanner -Dsonar.token=VOTRE_TOKEN
```

---

## 🔧 Commandes utiles

```bash
# Démarrer
./scripts/start-sonarqube.sh

# Arrêter
./scripts/stop-sonarqube.sh

# Voir les logs
docker logs -f ecommerce-sonarqube

# Statut
docker-compose -f docker-compose.sonarqube.yml ps

# Redémarrer
docker-compose -f docker-compose.sonarqube.yml restart
```

---

## 🐛 Troubleshooting

### SonarQube ne démarre pas

**Linux :**
```bash
sudo sysctl -w vm.max_map_count=262144
```

**Windows (WSL2) - PowerShell Admin :**
```powershell
wsl -d docker-desktop
sysctl -w vm.max_map_count=262144
exit
```

### Port 9000 déjà utilisé

Modifiez `.env` :
```bash
SONAR_PORT=9001
```

### Le réseau n'existe pas

Démarrez d'abord l'application principale :
```bash
docker-compose up -d
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| `docs/sonarqube-setup.md` | **Documentation complète** avec troubleshooting détaillé |
| `docs/sonarqube-quick-start.md` | Guide de démarrage rapide (3 étapes) |
| `docs/sonarqube-maven-config.md` | Configuration Maven pour Java |
| `docs/sonarqube-angular-config.md` | Configuration Angular pour TypeScript |

---

## 🔗 Ressources

- **Interface web** : http://localhost:9000
- **Documentation officielle SonarQube** : https://docs.sonarqube.org/latest/
- **Community Forum** : https://community.sonarsource.com/

---

## ✅ Checklist de vérification

- [ ] SonarQube démarre sans erreur
- [ ] Accès à http://localhost:9000 fonctionne
- [ ] Mot de passe admin changé
- [ ] Token d'authentification généré et ajouté à `.env`
- [ ] Projets créés dans l'interface web
- [ ] Configuration Maven ajoutée aux `pom.xml`
- [ ] Configuration Angular créée (`sonar-project.properties`)
- [ ] Première analyse lancée avec succès

---

## 🎯 Objectifs de qualité recommandés

Pour votre projet e-commerce, visez :

- **Coverage** : ≥ 70%
- **Duplications** : ≤ 3%
- **Maintainability Rating** : A ou B
- **Reliability Rating** : A
- **Security Rating** : A
- **0 Vulnerabilities** critiques

---

## 🚀 Prêt pour la production

Une fois votre configuration validée en local, vous pourrez :

1. **Intégrer avec GitHub Actions** pour les analyses automatiques
2. **Configurer des Quality Gates** pour bloquer les merge de code de mauvaise qualité
3. **Ajouter des webhooks** pour notifier l'équipe
4. **Déployer SonarQube** sur un serveur dédié pour l'équipe

Consultez `docs/sonarqube-setup.md` section "Intégration CI/CD" pour plus de détails.

---

**Installation réalisée le** : 15 décembre 2024

**Bon développement ! 🎉**