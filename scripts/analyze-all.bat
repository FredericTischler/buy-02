@echo off
REM ===================================
REM SCRIPT D'ANALYSE COMPLETE
REM Analyse tous les microservices + frontend
REM Windows Version
REM ===================================

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "SUCCESS_COUNT=0"
set "FAILED_COUNT=0"
set "TOTAL_SERVICES=4"

echo ================================================================================
echo   ANALYSE COMPLETE - E-Commerce Platform
echo ================================================================================
echo.

REM Charger les variables d'environnement
if exist "%PROJECT_ROOT%\.env" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%PROJECT_ROOT%\.env") do (
        if not "%%a"=="" (
            set "%%a=%%b"
        )
    )
)

REM Verifier le token
if "%SONAR_TOKEN%"=="" (
    echo [ERROR] SONAR_TOKEN non defini dans .env
    echo.
    echo Ajoutez votre token dans le fichier .env :
    echo   SONAR_TOKEN=votre_token_ici
    echo.
    pause
    exit /b 1
)

echo [OK] Token SonarQube trouve
echo.

REM ===================================
REM ANALYSE BACKEND
REM ===================================
echo ================================================================================
echo   BACKEND - Microservices Java
echo ================================================================================
echo.

set "SERVICES=user-service product-service media-service"
set "SERVICE_NUM=0"

for %%s in (%SERVICES%) do (
    set /a SERVICE_NUM+=1
    echo [!SERVICE_NUM!/4] Analyse de %%s...

    cd /d "%PROJECT_ROOT%\backend\%%s"

    call mvn clean verify sonar:sonar ^
        -Dsonar.projectKey=ecommerce-%%s ^
        -Dsonar.host.url=http://localhost:9000 ^
        -Dsonar.token=%SONAR_TOKEN% ^
        -q

    if errorlevel 1 (
        echo [ERROR] Echec de l'analyse de %%s
        set /a FAILED_COUNT+=1
    ) else (
        echo [OK] %%s analyse avec succes
        set /a SUCCESS_COUNT+=1
    )

    echo.
    cd /d "%PROJECT_ROOT%"
)

REM ===================================
REM ANALYSE FRONTEND
REM ===================================
echo ================================================================================
echo   FRONTEND - Angular
echo ================================================================================
echo.

echo [4/4] Analyse du frontend...

cd /d "%PROJECT_ROOT%\frontend"

REM Verifier si sonar-project.properties existe
if not exist "sonar-project.properties" (
    echo [INFO] Creation de sonar-project.properties...
    (
        echo sonar.projectKey=ecommerce-frontend
        echo sonar.projectName=E-Commerce Frontend
        echo sonar.projectVersion=1.0
        echo sonar.sources=src
        echo sonar.tests=src
        echo sonar.test.inclusions=**/*.spec.ts
        echo sonar.exclusions=**/node_modules/**,**/*.spec.ts,**/test/**,**/dist/**
        echo sonar.coverage.exclusions=**/*.spec.ts,**/test/**
        echo sonar.typescript.lcov.reportPaths=coverage/lcov.info
        echo sonar.sourceEncoding=UTF-8
    ) > sonar-project.properties
    echo [OK] Fichier cree
)

REM Generer le coverage
echo Execution des tests avec coverage...
call npm run test -- --code-coverage --watch=false --browsers=ChromeHeadless >nul 2>&1

if errorlevel 1 (
    echo [ERROR] Echec des tests du frontend
    set /a FAILED_COUNT+=1
) else (
    echo [OK] Tests executes

    REM Analyser avec SonarQube
    echo Analyse SonarQube...
    call npx sonar-scanner ^
        -Dsonar.host.url=http://localhost:9000 ^
        -Dsonar.token=%SONAR_TOKEN% ^
        >nul 2>&1

    if errorlevel 1 (
        echo [ERROR] Echec de l'analyse du frontend
        set /a FAILED_COUNT+=1
    ) else (
        echo [OK] Frontend analyse avec succes
        set /a SUCCESS_COUNT+=1
    )
)

echo.
cd /d "%PROJECT_ROOT%"

REM ===================================
REM RECAPITULATIF
REM ===================================
echo ================================================================================
echo   RECAPITULATIF
echo ================================================================================
echo.
echo Total de services analyses : %TOTAL_SERVICES%
echo Succes : %SUCCESS_COUNT%
echo Echecs : %FAILED_COUNT%
echo.
echo Interface SonarQube : http://localhost:9000
echo.

if %FAILED_COUNT%==0 (
    echo [OK] Toutes les analyses ont reussi !
) else (
    echo [WARNING] Certaines analyses ont echoue
)

echo.
pause