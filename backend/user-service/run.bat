@echo off
echo ========================================
echo Starting User Service on port 8081
echo ========================================

REM Définir JAVA_HOME
set JAVA_HOME=E:\Java
set PATH=%JAVA_HOME%\bin;%PATH%

echo JAVA_HOME: %JAVA_HOME%
echo.

REM Démarrer le service
echo Starting User Service...
java -jar target/user-service-1.0.0.jar

pause
