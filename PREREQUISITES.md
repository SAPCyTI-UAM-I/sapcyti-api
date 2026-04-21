# Prerequisites — SAPCyTI Development Environment

## Required Tools

| Tool | Version | Download | Verify |
|------|---------|----------|--------|
| **Java JDK** | 21 (LTS) | [Eclipse Temurin](https://adoptium.net/) | `java -version` |
| **Apache Maven** | 3.9+ | [Maven Downloads](https://maven.apache.org/download.cgi) | `mvn -version` |
| **Node.js** | 20 LTS | [Node.js Downloads](https://nodejs.org/) | `node -v` |
| **npm** | 10+ | Included with Node.js | `npm -v` |
| **Angular CLI** | 17+ | `npm install -g @angular/cli` | `ng version` |
| **Docker Desktop** | Latest | [Docker Desktop](https://www.docker.com/products/docker-desktop/) | `docker --version` |
| **Docker Compose** | v2+ | Included with Docker Desktop | `docker compose version` |
| **Git** | 2.40+ | [Git Downloads](https://git-scm.com/downloads) | `git --version` |

## Recommended IDE

- **IntelliJ IDEA** (Community or Ultimate) for backend
- **VS Code** with Angular Language Service extension for frontend

## Environment Variables

Copy `.env.example` to `.env` and adjust values as needed. See each repository's `.env.example` for required variables.

## Quick Start

```bash
# 1. Clone repositories
git clone https://github.com/SAPCyTI-UAM-I/sapcyti-api.git
git clone https://github.com/SAPCyTI-UAM-I/sapcyti-spa.git

# 2. Start PostgreSQL
docker compose -f docker-compose.dev.yml up -d

# 3. Backend
cd sapcyti-api
npm install          # Install commitlint + husky
mvn clean compile    # Verify build

# 4. Frontend
cd sapcyti-spa
npm install          # Install dependencies + commitlint + husky
ng serve             # Start dev server at http://localhost:4200
```
