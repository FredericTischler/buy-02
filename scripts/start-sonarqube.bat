@echo off
REM ===================================
REM SCRIPT DE DÉMARRAGE SONARQUBE
REM E-Commerce Microservices Platform
REM Windows Version
REM ===================================

setlocal enabledelayedexpansion

REM Configuration
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "ENV_FILE=%PROJECT_ROOT%\.env"
set "ENV_EXAMPLE=%PROJECT_ROOT%\.env.example"

echo ================================================================================
echo   DEMARRAGE SONARQUBE - E-Commerce Platform
echo ================================================================================
echo.

REM ===================================
REM 1. VERIFICATION DES PREREQUIS
REM ===================================
echo [1/5] Verification des prerequis...

REM Verifier Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker n'est pas installe
    echo Installez Docker Desktop: https://docs.docker.com/desktop/install/windows-install/
    pause
    exit /b 1
)
echo [OK] Docker trouve

REM Verifier Docker Compose
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose n'est pas installe
    echo Docker Compose est normalement inclus avec Docker Desktop
    pause
    exit /b 1
)
echo [OK] Docker Compose trouve

REM Verifier que Docker est demarre
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker n'est pas demarre
    echo Demarrez Docker Desktop et reessayez
    pause
    exit /b 1
)
echo [OK] Docker est actif

REM ===================================
REM 2. CONFIGURATION WSL2 (Windows)
REM ===================================
echo.
echo [2/5] Configuration systeme...
echo.
echo IMPORTANT pour Windows/WSL2:
echo Si SonarQube ne demarre pas, executez dans PowerShell (Admin):
echo.
echo   wsl -d docker-desktop
echo   sysctl -w vm.max_map_count=262144
echo   exit
echo.
timeout /t 3 >nul

REM ===================================
REM 3. CONFIGURATION ENVIRONNEMENT
REM ===================================
echo [3/5] Configuration de l'environnement...

REM Creer le fichier .env s'il n'existe pas
if not exist "%ENV_FILE%" (
    echo [WARNING] Fichier .env non trouve
    if exist "%ENV_EXAMPLE%" (
        echo Creation du fichier .env depuis .env.example...
        copy "%ENV_EXAMPLE%" "%ENV_FILE%" >nul
        echo [OK] Fichier .env cree
    ) else (
        echo [ERROR] Fichier .env.example introuvable
        pause
        exit /b 1
    )
) else (
    echo [OK] Fichier .env trouve
)

REM ===================================
REM 4. DEMARRAGE SONARQUBE
REM ===================================
echo.
echo [4/5] Demarrage de SonarQube...

cd /d "%PROJECT_ROOT%"

REM Demarrer les conteneurs
echo Lancement des conteneurs Docker...
docker-compose -f docker-compose.sonarqube.yml up -d

if errorlevel 1 (
    echo [ERROR] Erreur lors du demarrage des conteneurs
    echo.
    echo Verifiez que:
    echo   1. Docker Desktop est bien demarre
    echo   2. Le reseau ecommerce-network existe (lancez: docker-compose up -d)
    echo   3. WSL2 est configure correctement
    pause
    exit /b 1
)
echo [OK] Conteneurs demarres avec succes

REM ===================================
REM 5. ATTENTE DU DEMARRAGE
REM ===================================
echo.
echo [5/5] Attente du demarrage de SonarQube...
echo Cela peut prendre 1-3 minutes...
echo.

REM Attendre 30 secondes pour laisser SonarQube demarrer
echo Patientez pendant le demarrage...
timeout /t 30 /nobreak >nul

REM Tenter de verifier si SonarQube repond
curl -s http://localhost:9000/api/system/status >nul 2>&1
if errorlevel 1 (
    echo [INFO] SonarQube est encore en cours de demarrage
    echo Verifiez les logs avec: docker logs -f ecommerce-sonarqube
) else (
    echo [OK] SonarQube est pret !
)

REM ===================================
REM RECAPITULATIF
REM ===================================
echo.
echo ================================================================================
echo   SonarQube demarre avec succes !
echo ================================================================================
echo.
echo Interface Web :      http://localhost:9000
echo Username :           admin
echo Password :           admin
echo.
echo IMPORTANT : Changez le mot de passe lors de la premiere connexion !
echo.
echo Documentation :      docs\sonarqube-setup.md
echo.
echo Conteneurs actifs :
docker ps --filter "name=ecommerce-sonarqube" --format "table {{.Names}}\t{{.Status}}"
echo.
echo Commandes utiles :
echo   - Voir les logs :        docker logs -f ecommerce-sonarqube
echo   - Arreter :              docker-compose -f docker-compose.sonarqube.yml down
echo   - Redemarrer :           docker-compose -f docker-compose.sonarqube.yml restart
echo   - Statut :               docker-compose -f docker-compose.sonarqube.yml ps
echo.
echo Bonne analyse de code !
echo.
pause