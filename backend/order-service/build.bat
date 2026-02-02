@echo off
echo Building Order Service...
mvn clean package -DskipTests
echo Build complete!
pause