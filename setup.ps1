# ===================================
# SAPCyTI — Local Environment Setup (Windows)
# ===================================
Write-Host "🔍 Checking prerequisites..." -ForegroundColor Cyan

# Check Java
try {
  $javaVersion = java -version 2>&1 | Select-Object -First 1
  Write-Host "✅ Java: $javaVersion" -ForegroundColor Green
} catch {
  Write-Host "❌ Java JDK 21 is not installed. See PREREQUISITES.md" -ForegroundColor Red
  exit 1
}

# Check Maven
try {
  $mvnVersion = mvn -version 2>&1 | Select-Object -First 1
  Write-Host "✅ Maven: $mvnVersion" -ForegroundColor Green
} catch {
  Write-Host "❌ Maven 3.9+ is not installed. See PREREQUISITES.md" -ForegroundColor Red
  exit 1
}

# Check Node
try {
  $nodeVersion = node -v
  Write-Host "✅ Node: $nodeVersion" -ForegroundColor Green
} catch {
  Write-Host "❌ Node.js 20 LTS is not installed. See PREREQUISITES.md" -ForegroundColor Red
  exit 1
}

# Check Docker
try {
  $dockerVersion = docker --version
  Write-Host "✅ Docker: $dockerVersion" -ForegroundColor Green
} catch {
  Write-Host "❌ Docker is not installed. See PREREQUISITES.md" -ForegroundColor Red
  exit 1
}

Write-Host ""
Write-Host "🐘 Starting PostgreSQL..." -ForegroundColor Cyan
docker compose -f docker-compose.dev.yml up -d

Write-Host ""
Write-Host "⏳ Waiting for PostgreSQL to be ready..." -ForegroundColor Cyan
do {
  Start-Sleep -Seconds 1
  $ready = docker compose -f docker-compose.dev.yml exec -T db pg_isready -U sapcyti -d sapcyti_dev 2>&1
} while ($LASTEXITCODE -ne 0)
Write-Host "✅ PostgreSQL is ready at localhost:5432" -ForegroundColor Green

Write-Host ""
Write-Host "📦 Installing dev tools (commitlint + husky)..." -ForegroundColor Cyan
npm install

Write-Host ""
Write-Host "🔨 Building backend..." -ForegroundColor Cyan
mvn clean compile -q

Write-Host ""
Write-Host "✅ Setup complete! You can now run:" -ForegroundColor Green
Write-Host "   mvn spring-boot:run     — Start the API server"
Write-Host "   mvn test                — Run tests"
