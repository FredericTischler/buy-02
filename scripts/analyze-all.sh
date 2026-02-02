#!/bin/bash

# ===================================
# SCRIPT D'ANALYSE COMPLETE
# Analyse tous les microservices + frontend
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

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  ANALYSE COMPLETE - E-Commerce Platform${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Vérifier le token
if [[ -z "$SONAR_TOKEN" ]]; then
    echo -e "${YELLOW}⚠ Variable SONAR_TOKEN non définie${NC}"
    echo ""
    echo "Chargement depuis .env..."

    if [[ -f "$PROJECT_ROOT/.env" ]]; then
        export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)

        if [[ -z "$SONAR_TOKEN" ]]; then
            echo -e "${RED}✗ SONAR_TOKEN non trouvé dans .env${NC}"
            echo ""
            echo "Ajoutez votre token dans le fichier .env :"
            echo "  SONAR_TOKEN=votre_token_ici"
            echo ""
            echo "Pour générer un token :"
            echo "  1. Connectez-vous à http://localhost:9000"
            echo "  2. My Account > Security > Generate Token"
            exit 1
        fi
    else
        echo -e "${RED}✗ Fichier .env non trouvé${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✓ Token SonarQube trouvé${NC}"
echo ""

# Compteurs
TOTAL_SERVICES=0
SUCCESS_COUNT=0
FAILED_COUNT=0
FAILED_SERVICES=()

# ===================================
# ANALYSE BACKEND
# ===================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  BACKEND - Microservices Java${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

SERVICES=("user-service" "product-service" "media-service")

for service in "${SERVICES[@]}"; do
    TOTAL_SERVICES=$((TOTAL_SERVICES + 1))

    echo -e "${YELLOW}[${TOTAL_SERVICES}/4]${NC} Analyse de ${BLUE}${service}${NC}..."

    cd "$PROJECT_ROOT/backend/$service"

    if mvn clean verify sonar:sonar \
        -Dsonar.projectKey=ecommerce-$service \
        -Dsonar.host.url=http://localhost:9000 \
        -Dsonar.token=$SONAR_TOKEN \
        -q; then

        echo -e "${GREEN}✓ $service analysé avec succès${NC}"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo -e "${RED}✗ Échec de l'analyse de $service${NC}"
        FAILED_COUNT=$((FAILED_COUNT + 1))
        FAILED_SERVICES+=("$service")
    fi

    echo ""
    cd "$PROJECT_ROOT"
done

# ===================================
# ANALYSE FRONTEND
# ===================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  FRONTEND - Angular${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

TOTAL_SERVICES=$((TOTAL_SERVICES + 1))
echo -e "${YELLOW}[4/4]${NC} Analyse du ${BLUE}frontend${NC}..."

cd "$PROJECT_ROOT/frontend"

# Vérifier si le scanner est installé
if ! npm list sonarqube-scanner &> /dev/null; then
    echo -e "${YELLOW}⚠ Installation du scanner SonarQube...${NC}"
    npm install --save-dev sonarqube-scanner
fi

# Vérifier si sonar-project.properties existe
if [[ ! -f "sonar-project.properties" ]]; then
    echo -e "${YELLOW}⚠ Création de sonar-project.properties...${NC}"
    cat > sonar-project.properties << 'EOF'
sonar.projectKey=ecommerce-frontend
sonar.projectName=E-Commerce Frontend
sonar.projectVersion=1.0
sonar.sources=src
sonar.tests=src
sonar.test.inclusions=**/*.spec.ts
sonar.exclusions=**/node_modules/**,**/*.spec.ts,**/test/**,**/dist/**,**/*.config.ts,**/environments/**,**/.angular/**,**/coverage/**
sonar.coverage.exclusions=**/*.spec.ts,**/test/**,**/*.config.ts,**/environments/**,**/main.ts,**/*.module.ts
sonar.typescript.lcov.reportPaths=coverage/lcov.info
sonar.sourceEncoding=UTF-8
EOF
    echo -e "${GREEN}✓ Fichier créé${NC}"
fi

# Générer le coverage
echo "Exécution des tests avec coverage..."
if npm run test -- --code-coverage --watch=false --browsers=ChromeHeadless > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Tests exécutés${NC}"

    # Analyser avec SonarQube
    echo "Analyse SonarQube..."
    if npx sonar-scanner \
        -Dsonar.host.url=http://localhost:9000 \
        -Dsonar.token=$SONAR_TOKEN \
        > /dev/null 2>&1; then

        echo -e "${GREEN}✓ Frontend analysé avec succès${NC}"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo -e "${RED}✗ Échec de l'analyse du frontend${NC}"
        FAILED_COUNT=$((FAILED_COUNT + 1))
        FAILED_SERVICES+=("frontend")
    fi
else
    echo -e "${RED}✗ Échec des tests du frontend${NC}"
    FAILED_COUNT=$((FAILED_COUNT + 1))
    FAILED_SERVICES+=("frontend")
fi

echo ""
cd "$PROJECT_ROOT"

# ===================================
# RÉCAPITULATIF
# ===================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  RÉCAPITULATIF${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "Total de services analysés : ${TOTAL_SERVICES}"
echo -e "${GREEN}Succès : ${SUCCESS_COUNT}${NC}"

if [[ $FAILED_COUNT -gt 0 ]]; then
    echo -e "${RED}Échecs : ${FAILED_COUNT}${NC}"
    echo ""
    echo -e "${YELLOW}Services en échec :${NC}"
    for failed in "${FAILED_SERVICES[@]}"; do
        echo -e "  - $failed"
    done
fi

echo ""
echo -e "${BLUE}📊 Interface SonarQube :${NC} http://localhost:9000"
echo ""

if [[ $FAILED_COUNT -eq 0 ]]; then
    echo -e "${GREEN}✓ Toutes les analyses ont réussi !${NC}"
    echo ""
    exit 0
else
    echo -e "${YELLOW}⚠ Certaines analyses ont échoué${NC}"
    echo "Consultez les logs ci-dessus pour plus de détails"
    echo ""
    exit 1
fi
