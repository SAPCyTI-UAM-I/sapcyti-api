# SAPCyTI — Backend API

> Sistema de Administración de Posgrado del PCyTI — Universidad Autónoma Metropolitana, Unidad Iztapalapa

## Overview

Backend API for the SAPCyTI graduate program management portal. Built as a **modular monolith** with **Hexagonal Architecture (Ports & Adapters)** and **Domain-Driven Design (DDD)**.

## Architecture

- **Pattern:** Modular Monolith + Hexagonal Architecture
- **Design:** Domain-Driven Design (Bounded Contexts)
- **Full documentation:** [Architecture.md](../Docs/Design/Architecture.md)

## Tech Stack

| Component | Technology |
|-----------|------------|
| **Language** | Java 21 (Eclipse Temurin) |
| **Framework** | Spring Boot 3.x |
| **Persistence** | Spring Data JPA + Hibernate 6.x |
| **Database** | PostgreSQL 16 |
| **Migrations** | Flyway |
| **Build** | Maven |
| **Observability** | Spring Boot Actuator + Micrometer |
| **Logging** | SLF4J + Logback (JSON in prod) |

## Prerequisites

See [PREREQUISITES.md](PREREQUISITES.md) for required tools and versions.

## Quick Start

```bash
# 1. Start PostgreSQL
docker compose -f docker-compose.dev.yml up -d

# 2. Copy environment file
cp .env.example .env

# 3. Install dev tools (commitlint + husky)
npm install

# 4. Build
mvn clean compile

# 5. Run
mvn spring-boot:run

# 6. Verify
curl http://localhost:8080/actuator/health
```

## Development

```bash
# Run tests
mvn test

# Run tests with coverage report
mvn verify

# Check code style
mvn checkstyle:check

# Security scan
mvn dependency-check:check
```

## Project Structure

```
src/main/java/mx/uam/sapcyti/
├── configuration/          # Program Configuration bounded context
│   ├── domain/
│   │   ├── model/          # Aggregates, entities, value objects
│   │   ├── port/in/        # Input ports (use cases)
│   │   └── port/out/       # Output ports (repositories)
│   ├── application/
│   │   └── service/        # Use case implementations
│   └── infrastructure/
│       ├── adapter/in/     # REST controllers (driving adapters)
│       └── adapter/out/    # JPA implementations (driven adapters)
├── identity/               # Identity & Access bounded context (future)
├── academic/               # Academic Management bounded context (future)
├── offering/               # Academic Offering bounded context (future)
└── enrollment/             # Enrollment bounded context (future)
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on branching, commits, PRs, and code standards.

## License

MIT — See [LICENSE](LICENSE)
