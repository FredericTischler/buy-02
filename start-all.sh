#!/bin/bash

# ===================================================================
# SCRIPT DE DEMARRAGE AUTOMATIQUE - E-COMMERCE MICROSERVICES
# ===================================================================
# Ce script démarre automatiquement tous les services de l'application
# ===================================================================

echo "========================================"
echo "  E-COMMERCE - DEMARRAGE AUTOMATIQUE  "
echo "========================================"
echo ""

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Vérifier Java
echo -e "${YELLOW}[1/6] Verification de Java...${NC}"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}  ✓ Java installe: $JAVA_VERSION${NC}"
else
    echo -e "${RED}  ✗ Java non trouve! Installez Java 17+${NC}"
    exit 1
fi

# Vérifier Node.js
echo -e "${YELLOW}[2/6] Verification de Node.js...${NC}"
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}  ✓ Node.js installe: $NODE_VERSION${NC}"
else
    echo -e "${RED}  ✗ Node.js non trouve! Installez Node.js${NC}"
    exit 1
fi

# Vérifier Docker
echo -e "${YELLOW}[3/6] Verification de Docker...${NC}"
if command -v docker &> /dev/null; then
    echo -e "${GREEN}  ✓ Docker installe${NC}"
else
    echo -e "${RED}  ✗ Docker non trouve! Installez Docker${NC}"
    exit 1
fi

# Démarrer Docker Compose (MongoDB, Kafka, Zookeeper)
echo -e "${YELLOW}[4/6] Demarrage de Docker Compose (MongoDB, Kafka, Zookeeper)...${NC}"
docker compose up -d
sleep 10
echo -e "${GREEN}  ✓ Docker Compose demarre${NC}"

# Démarrer les services backend
echo -e "${YELLOW}[5/6] Demarrage des services backend...${NC}"

echo -e "${CYAN}  - User Service (port 8081)...${NC}"
cd backend/user-service
java -jar target/user-service-1.0.0.jar > /dev/null 2>&1 &
cd ../..
sleep 5

echo -e "${CYAN}  - Product Service (port 8082)...${NC}"
cd backend/product-service
java -jar target/product-service-1.0.0.jar > /dev/null 2>&1 &
cd ../..
sleep 3

echo -e "${CYAN}  - Media Service (port 8083)...${NC}"
cd backend/media-service
java -jar target/media-service-1.0.0.jar > /dev/null 2>&1 &
cd ../..
sleep 3

echo -e "${CYAN}  - Order Service (port 8085)...${NC}"
cd backend/order-service
java -jar target/order-service-1.0.0.jar > /dev/null 2>&1 &
cd ../..
sleep 5

echo -e "${GREEN}  ✓ Services backend demarres${NC}"

# Démarrer le frontend
echo -e "${YELLOW}[6/6] Demarrage du frontend Angular...${NC}"
cd frontend
npm start > /dev/null 2>&1 &
cd ..

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}     TOUS LES SERVICES SONT DEMARRES    ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${CYAN}Services disponibles :${NC}"
echo -e "  - Frontend:        https://localhost:4200"
echo -e "  - User Service:    https://localhost:8081"
echo -e "  - Product Service: https://localhost:8082"
echo -e "  - Media Service:   https://localhost:8083"
echo -e "  - Order Service:   https://localhost:8085"
echo -e "  - MongoDB:         localhost:27017"
echo -e "  - Kafka:           localhost:9092"
echo ""
echo -e "${YELLOW}Note: Acceptez les certificats SSL dans votre navigateur${NC}"
echo ""
echo -e "${GREEN}Pour arreter tous les services, executez: ./stop-all.sh${NC}"
echo ""
