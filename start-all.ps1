# Script de demarrage automatique - E-Commerce Microservices
# Ce script demarre automatiquement tous les services de l'application

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  E-COMMERCE - DEMARRAGE AUTOMATIQUE  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verification de Java
Write-Host "[1/6] Verification de Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "  OK Java installe: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "  ERREUR Java non trouve! Installez Java 17+" -ForegroundColor Red
    exit 1
}

# Verification de Node.js
Write-Host "[2/6] Verification de Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version
    Write-Host "  OK Node.js installe: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "  ERREUR Node.js non trouve! Installez Node.js" -ForegroundColor Red
    exit 1
}

# Verification de Docker
Write-Host "[3/6] Verification de Docker..." -ForegroundColor Yellow
try {
    docker --version | Out-Null
    Write-Host "  OK Docker installe" -ForegroundColor Green
} catch {
    Write-Host "  ERREUR Docker non trouve! Installez Docker Desktop" -ForegroundColor Red
    exit 1
}

# Demarrer Docker Compose
Write-Host "[4/6] Demarrage de Docker Compose..." -ForegroundColor Yellow
docker compose up -d
Start-Sleep -Seconds 10
Write-Host "  OK Docker Compose demarre" -ForegroundColor Green

# Demarrer les services backend
Write-Host "[5/6] Demarrage des services backend..." -ForegroundColor Yellow

Write-Host "  - User Service (port 8081)..." -ForegroundColor Cyan
cd backend\user-service
Start-Process -NoNewWindow -FilePath "java" -ArgumentList "-jar","target\user-service-1.0.0.jar"
cd ..\..
Start-Sleep -Seconds 5

Write-Host "  - Product Service (port 8082)..." -ForegroundColor Cyan
cd backend\product-service
Start-Process -NoNewWindow -FilePath "java" -ArgumentList "-jar","target\product-service-1.0.0.jar"
cd ..\..
Start-Sleep -Seconds 3

Write-Host "  - Media Service (port 8083)..." -ForegroundColor Cyan
cd backend\media-service
Start-Process -NoNewWindow -FilePath "java" -ArgumentList "-jar","target\media-service-1.0.0.jar"
cd ..\..
Start-Sleep -Seconds 5

Write-Host "  OK Services backend demarres" -ForegroundColor Green

# Demarrer le frontend
Write-Host "[6/6] Demarrage du frontend Angular..." -ForegroundColor Yellow
cd frontend
Start-Process -NoNewWindow -FilePath "npm" -ArgumentList "start"
cd ..

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "     TOUS LES SERVICES SONT DEMARRES    " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Services disponibles :" -ForegroundColor Cyan
Write-Host "  - Frontend:        https://localhost:4200" -ForegroundColor White
Write-Host "  - User Service:    https://localhost:8081" -ForegroundColor White
Write-Host "  - Product Service: https://localhost:8082" -ForegroundColor White
Write-Host "  - Media Service:   https://localhost:8083" -ForegroundColor White
Write-Host "  - MongoDB:         localhost:27017" -ForegroundColor White
Write-Host "  - Kafka:           localhost:9092" -ForegroundColor White
Write-Host ""
Write-Host "Note: Acceptez les certificats SSL dans votre navigateur" -ForegroundColor Yellow
Write-Host ""
Write-Host "Appuyez sur une touche pour quitter..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
