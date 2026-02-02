#!/bin/bash

# ===================================
# SCRIPT DE DÉMARRAGE SONARQUBE
# E-Commerce Microservices Platform
# ===================================

set -e

# Couleurs pour l'output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$PROJECT_ROOT/.env"
ENV_EXAMPLE="$PROJECT_ROOT/.env.example"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  DÉMARRAGE SONARQUBE - E-Commerce Platform${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# ===================================
# 1. VÉRIFICATION DES PRÉREQUIS
# ===================================
echo -e "${YELLOW}[1/6]${NC} Vérification des prérequis..."

# Vérifier Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker n'est pas installé${NC}"
    echo "Installez Docker: https://docs.docker.com/get-docker/"
    exit 1
fi
echo -e "${GREEN}✓ Docker trouvé : $(docker --version)${NC}"

# Vérifier Docker Compose (v2 intégré ou v1 standalone)
DOCKER_COMPOSE_CMD=""
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
    echo -e "${GREEN}✓ Docker Compose trouvé : $(docker compose version)${NC}"
elif command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
    echo -e "${GREEN}✓ Docker Compose trouvé : $(docker-compose --version)${NC}"
else
    echo -e "${RED}✗ Docker Compose n'est pas installé${NC}"
    echo "Installez Docker Compose: https://docs.docker.com/compose/install/"
    exit 1
fi

# Vérifier que Docker est démarré
if ! docker info &> /dev/null; then
    echo -e "${RED}✗ Docker n'est pas démarré${NC}"
    echo "Démarrez Docker et réessayez"
    exit 1
fi
echo -e "${GREEN}✓ Docker est actif${NC}"

# ===================================
# 2. CONFIGURATION SYSTÈME
# ===================================
echo ""
echo -e "${YELLOW}[2/6]${NC} Vérification de la configuration système..."

# Vérifier vm.max_map_count (requis pour Elasticsearch/SonarQube)
current_max_map_count=$(sysctl -n vm.max_map_count 2>/dev/null || echo "0")
required_max_map_count=262144

if [[ "$current_max_map_count" -lt "$required_max_map_count" ]]; then
    echo -e "${YELLOW}⚠ vm.max_map_count trop bas ($current_max_map_count < $required_max_map_count)${NC}"
    echo "Tentative d'augmentation (peut nécessiter sudo)..."

    if sudo sysctl -w vm.max_map_count=$required_max_map_count &> /dev/null; then
        echo -e "${GREEN}✓ vm.max_map_count mis à jour${NC}"
    else
        echo -e "${RED}✗ Impossible de mettre à jour vm.max_map_count${NC}"
        echo "Exécutez manuellement: sudo sysctl -w vm.max_map_count=262144"
        exit 1
    fi
else
    echo -e "${GREEN}✓ vm.max_map_count OK ($current_max_map_count)${NC}"
fi

# ===================================
# 3. CONFIGURATION ENVIRONNEMENT
# ===================================
echo ""
echo -e "${YELLOW}[3/6]${NC} Configuration de l'environnement..."

# Créer le fichier .env s'il n'existe pas
if [[ ! -f "$ENV_FILE" ]]; then
    echo -e "${YELLOW}⚠ Fichier .env non trouvé${NC}"
    if [[ -f "$ENV_EXAMPLE" ]]; then
        echo "Création du fichier .env depuis .env.example..."
        cp "$ENV_EXAMPLE" "$ENV_FILE"
        echo -e "${GREEN}✓ Fichier .env créé${NC}"
    else
        echo -e "${RED}✗ Fichier .env.example introuvable${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Fichier .env trouvé${NC}"
fi

# ===================================
# 4. RÉSEAU DOCKER
# ===================================
echo ""
echo -e "${YELLOW}[4/6]${NC} Vérification du réseau Docker..."

# Vérifier si le réseau ecommerce-network existe
if docker network inspect safe-zone_ecommerce-network &> /dev/null; then
    echo -e "${GREEN}✓ Réseau ecommerce-network existe${NC}"
else
    echo -e "${YELLOW}⚠ Réseau ecommerce-network non trouvé${NC}"
    echo "Le réseau sera créé automatiquement au premier lancement de l'application principale"
    echo "Si vous voulez démarrer SonarQube maintenant, lancez d'abord l'application principale:"
    echo "  $DOCKER_COMPOSE_CMD up -d"
    echo ""
    read -p "Voulez-vous démarrer l'application principale maintenant? (y/N) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Démarrage de l'application principale..."
        cd "$PROJECT_ROOT"
        $DOCKER_COMPOSE_CMD up -d
        echo -e "${GREEN}✓ Application principale démarrée${NC}"
    else
        echo -e "${YELLOW}Le réseau sera créé au premier lancement${NC}"
    fi
fi

# ===================================
# 5. DÉMARRAGE SONARQUBE
# ===================================
echo ""
echo -e "${YELLOW}[5/6]${NC} Démarrage de SonarQube..."

cd "$PROJECT_ROOT"

# Démarrer les conteneurs
echo "Lancement des conteneurs Docker..."
$DOCKER_COMPOSE_CMD -f docker-compose.sonarqube.yml up -d

if [[ $? -eq 0 ]]; then
    echo -e "${GREEN}✓ Conteneurs démarrés avec succès${NC}"
else
    echo -e "${RED}✗ Erreur lors du démarrage des conteneurs${NC}"
    exit 1
fi

# ===================================
# 6. ATTENTE DU DÉMARRAGE
# ===================================
echo ""
echo -e "${YELLOW}[6/6]${NC} Attente du démarrage de SonarQube..."
echo "Cela peut prendre 1-3 minutes..."

# Fonction pour vérifier si SonarQube est prêt
wait_for_sonarqube() {
    local max_attempts=60
    local attempt=1

    while [[ $attempt -le $max_attempts ]]; do
        if curl -s http://localhost:9000/api/system/status | grep -q "UP"; then
            return 0
        fi

        echo -ne "\rTentative $attempt/$max_attempts..."
        sleep 5
        attempt=$((attempt + 1))
    done

    return 1
}

if wait_for_sonarqube; then
    echo -e "\n${GREEN}✓ SonarQube est prêt !${NC}"
else
    echo -e "\n${YELLOW}⚠ SonarQube prend plus de temps que prévu${NC}"
    echo "Vérifiez les logs avec: docker logs -f ecommerce-sonarqube"
fi

# ===================================
# RÉCAPITULATIF
# ===================================
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  SonarQube démarré avec succès !${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}📊 Interface Web :${NC}      http://localhost:9000"
echo -e "${BLUE}👤 Username :${NC}          admin"
echo -e "${BLUE}🔐 Password :${NC}          admin"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANT :${NC} Changez le mot de passe lors de la première connexion !"
echo ""
echo -e "${BLUE}📖 Documentation :${NC}     docs/sonarqube-setup.md"
echo ""
echo -e "${BLUE}Conteneurs actifs :${NC}"
docker ps --filter "name=ecommerce-sonarqube" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo -e "${BLUE}Commandes utiles :${NC}"
echo "  • Voir les logs :        docker logs -f ecommerce-sonarqube"
echo "  • Arrêter :              $DOCKER_COMPOSE_CMD -f docker-compose.sonarqube.yml down"
echo "  • Redémarrer :           $DOCKER_COMPOSE_CMD -f docker-compose.sonarqube.yml restart"
echo "  • Statut :               $DOCKER_COMPOSE_CMD -f docker-compose.sonarqube.yml ps"
echo ""
echo -e "${GREEN}Bonne analyse de code ! 🚀${NC}"
echo ""
