@echo off
REM ===================================
REM SCRIPT D'ARRET SONARQUBE
REM E-Commerce Microservices Platform
REM Windows Version
REM ===================================

setlocal

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."

echo ================================================================================
echo   ARRET SONARQUBE - E-Commerce Platform
echo ================================================================================
echo.

cd /d "%PROJECT_ROOT%"

REM Verifier si les conteneurs existent
docker ps -a | findstr "ecommerce-sonarqube" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Aucun conteneur SonarQube en cours d'execution
) else (
    echo Arret des conteneurs SonarQube...
    docker-compose -f docker-compose.sonarqube.yml down

    if errorlevel 1 (
        echo [ERROR] Erreur lors de l'arret
        pause
        exit /b 1
    )
    echo [OK] SonarQube arrete avec succes
)

echo.
echo Termine !
echo.
pause