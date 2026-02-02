# ========================================
# BUILD & RUN SCRIPT - User Service
# ========================================

Write-Host "🚀 Building User Service..." -ForegroundColor Cyan

# Configuration
$JAVA_HOME = "E:\Java"
$MAVEN_HOME = "E:\DevTools\maven-mvnd-1.0.3-windows-amd64\maven-mvnd-1.0.3-windows-amd64\mvn"

# Définir les variables d'environnement
$env:JAVA_HOME = $JAVA_HOME
$env:PATH = "$JAVA_HOME\bin;$MAVEN_HOME\bin;$env:PATH"

Write-Host "✅ JAVA_HOME: $JAVA_HOME" -ForegroundColor Green
Write-Host "✅ MAVEN_HOME: $MAVEN_HOME" -ForegroundColor Green

# Vérifier Java
Write-Host "`n📦 Vérification de Java..." -ForegroundColor Yellow
java -version

# Compiler le projet
Write-Host "`n🔨 Compilation du projet..." -ForegroundColor Yellow
& "$MAVEN_HOME\bin\mvn.cmd" clean package -DskipTests

# Vérifier si la compilation a réussi
if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Compilation réussie!" -ForegroundColor Green
    
    # Demander si on veut démarrer le service
    $response = Read-Host "`nVoulez-vous démarrer le User Service maintenant? (O/N)"
    if ($response -eq "O" -or $response -eq "o") {
        Write-Host "`n🚀 Démarrage du User Service sur http://localhost:8081..." -ForegroundColor Cyan
        java -jar target/user-service-1.0.0.jar
    }
} else {
    Write-Host "`n❌ Erreur lors de la compilation!" -ForegroundColor Red
    Write-Host "Vérifiez que Java et Maven sont bien installés." -ForegroundColor Yellow
}
