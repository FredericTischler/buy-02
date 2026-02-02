#!/bin/bash

#################################################
# Script de Configuration SonarCloud Quality Gates
#
# Ce script crée un Quality Gate personnalisé
# et l'applique à tous les projets
#################################################

set -e  # Exit on error

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  SonarCloud Quality Gates Setup${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Vérifier les prérequis
command -v curl >/dev/null 2>&1 || { echo -e "${RED}❌ curl n'est pas installé${NC}" >&2; exit 1; }
command -v jq >/dev/null 2>&1 || { echo -e "${YELLOW}⚠️  jq n'est pas installé (optionnel mais recommandé)${NC}"; }

# Variables
SONAR_HOST="https://sonarcloud.io"
SONAR_TOKEN="${SONAR_TOKEN:-}"
ORGANIZATION="${SONAR_ORGANIZATION:-zone01-ecommerce}"
QUALITY_GATE_NAME="Zone01 School Project"

# Liste des projets
PROJECTS=(
    "ecommerce-user-service"
    "ecommerce-product-service"
    "ecommerce-media-service"
    "ecommerce-frontend"
)

# Demander le token si non défini
if [[ -z "$SONAR_TOKEN" ]]; then
    echo -e "${YELLOW}⚠️  Variable SONAR_TOKEN non définie${NC}"
    echo -e "${BLUE}Entrez votre token SonarCloud (sqp_xxx):${NC}"
    read -r SONAR_TOKEN
    echo ""
fi

# Vérifier que le token fonctionne
echo -e "${BLUE}🔐 Vérification de l'authentification...${NC}"
AUTH_CHECK=$(curl -s -u "${SONAR_TOKEN}:" "${SONAR_HOST}/api/authentication/validate")
if echo "$AUTH_CHECK" | grep -q "true"; then
    echo -e "${GREEN}✅ Authentification réussie${NC}"
else
    echo -e "${RED}❌ Authentification échouée. Vérifiez votre token.${NC}"
    exit 1
fi
echo ""

# Fonction pour créer le Quality Gate
create_quality_gate() {
    echo -e "${BLUE}📊 Création du Quality Gate '${QUALITY_GATE_NAME}'...${NC}"

    # Vérifier si le Quality Gate existe déjà
    EXISTING_QG=$(curl -s -u "${SONAR_TOKEN}:" \
        "${SONAR_HOST}/api/qualitygates/list" | \
        grep -o "\"name\":\"${QUALITY_GATE_NAME}\"" || true)

    if [[ -n "$EXISTING_QG" ]]; then
        echo -e "${YELLOW}⚠️  Quality Gate '${QUALITY_GATE_NAME}' existe déjà${NC}"
        echo -e "${BLUE}   Récupération de l'ID...${NC}"

        QG_ID=$(curl -s -u "${SONAR_TOKEN}:" \
            "${SONAR_HOST}/api/qualitygates/list" | \
            grep -A 5 "\"name\":\"${QUALITY_GATE_NAME}\"" | \
            grep -o '"id":"[0-9]*"' | \
            head -1 | \
            grep -o '[0-9]*')
    else
        # Créer le Quality Gate
        RESPONSE=$(curl -s -u "${SONAR_TOKEN}:" -X POST \
            "${SONAR_HOST}/api/qualitygates/create" \
            -d "name=${QUALITY_GATE_NAME// /%20}")

        QG_ID=$(echo "$RESPONSE" | grep -o '"id":"[0-9]*"' | grep -o '[0-9]*')
        echo -e "${GREEN}✅ Quality Gate créé avec ID: ${QG_ID}${NC}"
    fi

    echo ""
    return 0
}

# Fonction pour ajouter une condition
add_condition() {
    local metric=$1
    local operator=$2
    local threshold=$3
    local gate_id=$4

    echo -e "${BLUE}  Ajout condition: ${metric} ${operator} ${threshold}${NC}"

    curl -s -u "${SONAR_TOKEN}:" -X POST \
        "${SONAR_HOST}/api/qualitygates/create_condition" \
        -d "gateId=${gate_id}" \
        -d "metric=${metric}" \
        -d "op=${operator}" \
        -d "error=${threshold}" > /dev/null

    echo -e "${GREEN}  ✅ Condition ajoutée${NC}"
    return 0
}

# Fonction pour configurer les conditions
configure_conditions() {
    local gate_id=$1

    echo -e "${BLUE}🎯 Configuration des conditions du Quality Gate...${NC}"
    echo ""

    # Coverage on New Code >= 80%
    add_condition "new_coverage" "LT" "80" "$gate_id"

    # Duplicated Lines on New Code <= 3%
    add_condition "new_duplicated_lines_density" "GT" "3" "$gate_id"

    # Maintainability Rating on New Code = A
    add_condition "new_maintainability_rating" "GT" "1" "$gate_id"

    # Reliability Rating on New Code = A
    add_condition "new_reliability_rating" "GT" "1" "$gate_id"

    # Security Rating on New Code = A
    add_condition "new_security_rating" "GT" "1" "$gate_id"

    # Security Hotspots Reviewed >= 100%
    add_condition "new_security_hotspots_reviewed" "LT" "100" "$gate_id"

    echo ""
    echo -e "${GREEN}✅ Toutes les conditions configurées${NC}"
    echo ""
    return 0
}

# Fonction pour appliquer le Quality Gate à un projet
apply_quality_gate_to_project() {
    local project_key=$1
    local gate_id=$2

    echo -e "${BLUE}  Projet: ${project_key}${NC}"

    # Vérifier si le projet existe
    PROJECT_CHECK=$(curl -s -u "${SONAR_TOKEN}:" \
        "${SONAR_HOST}/api/projects/search?projects=${project_key}" | \
        grep -o "\"key\":\"${project_key}\"" || true)

    if [[ -z "$PROJECT_CHECK" ]]; then
        echo -e "${YELLOW}  ⚠️  Projet non trouvé, ignoré${NC}"
        return 0
    fi

    # Appliquer le Quality Gate
    curl -s -u "${SONAR_TOKEN}:" -X POST \
        "${SONAR_HOST}/api/qualitygates/select" \
        -d "projectKey=${project_key}" \
        -d "gateId=${gate_id}" > /dev/null

    echo -e "${GREEN}  ✅ Quality Gate appliqué${NC}"
    return 0
}

# Fonction pour appliquer aux projets
apply_to_projects() {
    local gate_id=$1

    echo -e "${BLUE}🎯 Application du Quality Gate aux projets...${NC}"
    echo ""

    for project in "${PROJECTS[@]}"; do
        apply_quality_gate_to_project "$project" "$gate_id"
    done

    echo ""
    echo -e "${GREEN}✅ Quality Gate appliqué à tous les projets${NC}"
    echo ""
    return 0
}

# Fonction pour définir comme Quality Gate par défaut
set_as_default() {
    local gate_id=$1

    echo -e "${BLUE}🌟 Définition comme Quality Gate par défaut...${NC}"

    curl -s -u "${SONAR_TOKEN}:" -X POST \
        "${SONAR_HOST}/api/qualitygates/set_as_default" \
        -d "id=${gate_id}" > /dev/null

    echo -e "${GREEN}✅ Défini comme Quality Gate par défaut${NC}"
    echo ""
    return 0
}

# Fonction pour afficher le résumé
show_summary() {
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}  Configuration Terminée !${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo ""
    echo -e "${GREEN}✅ Quality Gate '${QUALITY_GATE_NAME}' configuré${NC}"
    echo ""
    echo -e "${BLUE}Conditions configurées :${NC}"
    echo -e "  • Coverage on New Code >= 80%"
    echo -e "  • Duplicated Lines <= 3%"
    echo -e "  • Maintainability Rating = A"
    echo -e "  • Reliability Rating = A"
    echo -e "  • Security Rating = A"
    echo -e "  • Security Hotspots Reviewed >= 100%"
    echo ""
    echo -e "${BLUE}Projets configurés :${NC}"
    for project in "${PROJECTS[@]}"; do
        echo -e "  • ${project}"
    done
    echo ""
    echo -e "${BLUE}🌐 Accéder à SonarCloud :${NC}"
    echo -e "  ${SONAR_HOST}/organizations/${ORGANIZATION}/quality_gates"
    echo ""
    return 0
}

# Exécution principale
main() {
    create_quality_gate
    configure_conditions "$QG_ID"
    apply_to_projects "$QG_ID"
    set_as_default "$QG_ID"
    show_summary
}

# Confirmation avant exécution
echo -e "${YELLOW}Ce script va :${NC}"
echo -e "  1. Créer un Quality Gate '${QUALITY_GATE_NAME}'"
echo -e "  2. Configurer 6 conditions"
echo -e "  3. L'appliquer aux 4 projets"
echo -e "  4. Le définir comme Quality Gate par défaut"
echo ""
echo -e "${YELLOW}Organisation : ${ORGANIZATION}${NC}"
echo -e "${YELLOW}SonarCloud : ${SONAR_HOST}${NC}"
echo ""
read -p "Continuer ? (y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    main
else
    echo -e "${RED}❌ Annulé${NC}"
    exit 0
fi
