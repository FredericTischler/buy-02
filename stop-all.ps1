# Script d'arret - E-Commerce Microservices

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "     ARRET DE TOUS LES SERVICES        " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Arreter les processus Java
Write-Host "Arret des services backend..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "  OK Services backend arretes" -ForegroundColor Green

# Arreter Node.js (Angular)
Write-Host "Arret du frontend..." -ForegroundColor Yellow
Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "  OK Frontend arrete" -ForegroundColor Green

# Arreter Docker Compose
Write-Host "Arret de Docker Compose..." -ForegroundColor Yellow
docker-compose down
Write-Host "  OK Docker Compose arrete" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "   TOUS LES SERVICES SONT ARRETES      " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
