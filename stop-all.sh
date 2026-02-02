#!/bin/bash

# ===================================================================
# SCRIPT D'ARRET - E-COMMERCE MICROSERVICES
# ===================================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo "========================================"
echo "     ARRET DE TOUS LES SERVICES        "
echo "========================================"
echo ""

# Arrêter les processus Java
echo -e "${YELLOW}Arret des services backend...${NC}"
pkill -f "java -jar.*service.*\.jar" 2>/dev/null
echo -e "${GREEN}  ✓ Services backend arretes${NC}"

# Arrêter Node.js (Angular)
echo -e "${YELLOW}Arret du frontend...${NC}"
pkill -f "node.*ng serve" 2>/dev/null
pkill -f "npm start" 2>/dev/null
echo -e "${GREEN}  ✓ Frontend arrete${NC}"

# Arrêter Docker Compose
echo -e "${YELLOW}Arret de Docker Compose...${NC}"
docker-compose down
echo -e "${GREEN}  ✓ Docker Compose arrete${NC}"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   TOUS LES SERVICES SONT ARRETES      ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
