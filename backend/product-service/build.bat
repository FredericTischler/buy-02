@echo off
echo ========================================
echo Building Product Service
echo ========================================

set JAVA_HOME=E:\Java
set MAVEN_HOME=E:\DevTools\maven-mvnd-1.0.3-windows-amd64\maven-mvnd-1.0.3-windows-amd64\mvn

echo JAVA_HOME: %JAVA_HOME%
echo MAVEN_HOME: %MAVEN_HOME%
echo.

echo Verifying Java installation...
java -version
echo.

echo Compiling project...
"%MAVEN_HOME%\bin\mvn.cmd" clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESS!
    echo JAR file: target\product-service-1.0.0.jar
    echo ========================================
) else (
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
)

pause
