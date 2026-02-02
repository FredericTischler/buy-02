#!/bin/bash

# ===================================
# SCRIPT D'ARRÊT SONARQUBE
# E-Commerce Microservices Platform
# ===================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Détecter la commande Docker Compose
DOCKER_COMPOSE_CMD=""
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
else
    echo -e "${RED}✗ Docker Compose non trouvé${NC}"
    exit 1
fi

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  ARRÊT SONARQUBE - E-Commerce Platform${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

cd "$PROJECT_ROOT"

# Vérifier si les conteneurs existent
if docker ps -a | grep -q "ecommerce-sonarqube"; then
    echo "Arrêt des conteneurs SonarQube..."
    $DOCKER_COMPOSE_CMD -f docker-compose.sonarqube.yml down

    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✓ SonarQube arrêté avec succès${NC}"
    else
        echo -e "${RED}✗ Erreur lors de l'arrêt${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠ Aucun conteneur SonarQube en cours d'exécution${NC}"
fi

echo ""
echo -e "${GREEN}Terminé !${NC}"
echo ""
