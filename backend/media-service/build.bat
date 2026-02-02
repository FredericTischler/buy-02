@echo off
REM ===========================================
REM BUILD SCRIPT - MEDIA SERVICE
REM ===========================================

echo ============================================
echo COMPILATION DU MEDIA SERVICE
echo ============================================
echo.

REM Aller dans le répertoire du projet
cd /d E:\pZone01\Pjava\buy-01\backend\media-service

REM Définir JAVA_HOME
set JAVA_HOME=E:\Java
set PATH=%JAVA_HOME%\bin;%PATH%

REM Vérifier Java
echo [1/3] Vérification de Java...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Java n'est pas installé ou JAVA_HOME mal configuré
    pause
    exit /b 1
)
echo.

REM Nettoyer et compiler
echo [2/3] Compilation avec Maven...
E:\DevTools\maven-mvnd-1.0.3-windows-amd64\maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: La compilation a échoué
    pause
    exit /b 1
)
echo.

REM Vérifier le JAR
echo [3/3] Vérification du JAR...
if exist target\media-service-1.0.0.jar (
    echo ✅ JAR créé avec succès : target\media-service-1.0.0.jar
) else (
    echo ❌ ERREUR: Le JAR n'a pas été créé
    pause
    exit /b 1
)
echo.

echo ============================================
echo COMPILATION TERMINÉE AVEC SUCCÈS
echo ============================================
echo.
echo Pour démarrer le service :
echo   cd backend\media-service
echo   java -jar target\media-service-1.0.0.jar
echo.
pause
