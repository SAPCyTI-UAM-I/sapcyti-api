#!/bin/bash
# ===================================
# SAPCyTI — Local Environment Setup
# ===================================
set -e

echo "🔍 Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
  echo "❌ Java JDK 21 is not installed. See PREREQUISITES.md"
  exit 1
fi
echo "✅ Java: $(java -version 2>&1 | head -1)"

# Check Maven
if ! command -v mvn &> /dev/null; then
  echo "❌ Maven 3.9+ is not installed. See PREREQUISITES.md"
  exit 1
fi
echo "✅ Maven: $(mvn -version 2>&1 | head -1)"

# Check Node
if ! command -v node &> /dev/null; then
  echo "❌ Node.js 20 LTS is not installed. See PREREQUISITES.md"
  exit 1
fi
echo "✅ Node: $(node -v)"

# Check Docker
if ! command -v docker &> /dev/null; then
  echo "❌ Docker is not installed. See PREREQUISITES.md"
  exit 1
fi
echo "✅ Docker: $(docker --version)"

echo ""
echo "🐘 Starting PostgreSQL..."
docker compose -f docker-compose.dev.yml up -d

echo ""
echo "⏳ Waiting for PostgreSQL to be ready..."
until docker compose -f docker-compose.dev.yml exec -T db pg_isready -U sapcyti -d sapcyti_dev > /dev/null 2>&1; do
  sleep 1
done
echo "✅ PostgreSQL is ready at localhost:5432"

echo ""
echo "📦 Installing dev tools (commitlint + husky)..."
npm install

echo ""
echo "🔨 Building backend..."
mvn clean compile -q

echo ""
echo "✅ Setup complete! You can now run:"
echo "   mvn spring-boot:run     — Start the API server"
echo "   mvn test                — Run tests"
