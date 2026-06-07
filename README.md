# SAPCyTI — Backend API

> Sistema de Administración de Posgrado del PCyTI — Universidad Autónoma Metropolitana, Unidad Iztapalapa

## Overview

Backend API for the SAPCyTI graduate program management portal. Built as a **modular monolith** with **Hexagonal Architecture (Ports & Adapters)** and **Domain-Driven Design (DDD)**.

## Architecture

- **Pattern:** Modular Monolith + Hexagonal Architecture
- **Design:** Domain-Driven Design (Bounded Contexts)
- **Full documentation:** If you have the monorepo layout, see [`../Docs/Design/Architecture.md`](../Docs/Design/Architecture.md). Otherwise use the architecture document from the **SAPCyTI documentation** repository your team links to this project.

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

## Run locally

You need **Docker Desktop** (or compatible engine), **Java 21**, and **Maven** (or use the included **`./mvnw`** / **`mvnw.cmd`** wrapper).

### 1. Start PostgreSQL (development)

The compose file maps the database to host port **5433** so it does not clash with another PostgreSQL often bound to **5432** on Windows.

```powershell
docker compose -f docker-compose.dev.yml up -d
```

Wait a few seconds for the container to become ready. Default credentials match [`docker-compose.dev.yml`](docker-compose.dev.yml): user `sapcyti`, password `sapcyti_dev_pass`, database `sapcyti_dev`.

### 2. Environment variables

[`application.yml`](src/main/resources/application.yml) supplies defaults aligned with the compose file. You only need to set the **Spring profile** for local development:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
```

Optional overrides (for example if you change the compose port or credentials):

| Variable | Example |
|----------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5433/sapcyti_dev` |
| `DB_USER` | `sapcyti` |
| `DB_PASS` | `sapcyti_dev_pass` |
| `SERVER_PORT` | `8080` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` (comma-separated for several origins) |

If you previously set `DB_*` globally to wrong values, clear them in the current shell: `Remove-Item Env:DB_URL, Env:DB_USER, Env:DB_PASS -ErrorAction SilentlyContinue`.

Reference template: [`.env.example`](.env.example) (Spring Boot does not load `.env` automatically; copy values into your shell or IDE run configuration).

### 3. Optional: commit hooks

```powershell
npm install
```

### 4. Build and run

```powershell
.\mvnw.cmd clean verify
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

On Linux or macOS use `./mvnw` instead of `.\mvnw.cmd`.

### 5. Verify

Open [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) or run:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/actuator/health -UseBasicParsing
```

Stop the API with `Ctrl+C`. Stop the database with `docker compose -f docker-compose.dev.yml down`.

## Authentication API (SPEC-012 — handoff for SPA)

JWT login is available for local development after PostgreSQL is running and Flyway has applied migrations (seed users — password **`password`** for all):

| Role | Email |
|------|-------|
| SYSTEM_ADMIN | `system_admin@uam.mx` |
| COORDINATOR | `coordinator@uam.mx` |
| ASSISTANT | `assistant@uam.mx` |
| PROFESSOR | `professor@uam.mx` |
| STUDENT | `student@uam.mx` |
| SPEAKER | `speaker@uam.mx` |

| Doc | Audience |
|-----|----------|
| [Frontend API contract](../Docs/implementation/guides/frontend-auth-api-contract.md) | SPA / SPEC-013 |
| [Login flow & tenant](../Docs/implementation/guides/authentication-login-flow.md) | Full stack |

**Base URLs:** host JVM `http://localhost:8080` (or `SERVER_PORT`); Docker stack maps API to **`http://localhost:8081`** (`sapcyti-infra/local-dev/docker-compose.stack.yml`).

| Endpoint | Method | Auth | Notes |
|----------|--------|------|-------|
| `/api/auth/login` | POST | None | Body: `{ "email", "password", "rememberMe", "deviceInfo?" }` |
| `/api/auth/refresh` | POST | Cookie `refreshToken` | Returns new `accessToken` |
| `/api/auth/logout` | POST | Cookie `refreshToken` | Revokes refresh session |

**Password recovery (SPEC-015 — HU-02):**

| Endpoint | Method | Auth | Notes |
|----------|--------|------|-------|
| `/api/auth/forgot-password` | POST | None | Body: `{ "email" }` — always **200** with generic message (no email enumeration) |
| `/api/auth/reset-password` | POST | None | Body: `{ "token", "newPassword" }` — **200** on success; **400** for invalid/expired/used token |

Forgot response (200): `{ "message": "If an account with that email exists, a recovery email has been sent" }` (English via `Accept-Language: en`; Spanish by default).

Reset errors (400): `{ "error": "INVALID_TOKEN" | "EXPIRED_TOKEN" | "TOKEN_USED", "message": "..." }`.

**Mail (dev):** defaults to `localhost:1025` (MailHog). Set `PASSWORD_RESET_BASE_URL` to the SPA origin (default `http://localhost:4200`) so reset links point to `/auth/reset-password?token=...`.

### MailHog (password recovery emails)

MailHog is **not** part of the Java API — it is a dev-only SMTP sink in [`sapcyti-infra/local-dev`](../sapcyti-infra/local-dev/).

| Mode | Start MailHog | API SMTP target | Inbox UI |
|------|---------------|-----------------|----------|
| JVM on host (`SPRING_PROFILES_ACTIVE=dev`) | `docker compose -f ../sapcyti-infra/local-dev/docker-compose.db.yml up -d` | `localhost:1025` (defaults) | [http://localhost:8025](http://localhost:8025) |
| Full Docker stack | `docker compose -f ../sapcyti-infra/local-dev/docker-compose.stack.yml up -d` | `mailhog:1025` (via `.env`) | [http://localhost:8025](http://localhost:8025) |

Example forgot-password + check inbox:

```powershell
$body = '{"email":"student@uam.mx"}'
Invoke-RestMethod -Uri http://localhost:8080/api/auth/forgot-password -Method POST -ContentType "application/json" -Body $body
# Open http://localhost:8025 — click the message and use the reset link
```

**Login response (200):** `{ "accessToken", "expiresIn": 900, "role" }` plus `Set-Cookie: refreshToken=...; HttpOnly; Path=/api/auth; SameSite=Strict`.

**Protected APIs:** send `Authorization: Bearer {accessToken}`.

**CORS (SPA on port 4200):** set `CORS_ALLOWED_ORIGINS=http://localhost:4200` (stack `.env` also includes `http://localhost:8888` for the Nginx edge).

**JWT keys (dev):** bundled under `src/main/resources/jwt/`. Regenerate with:

```powershell
.\scripts\generate-jwt-keys.ps1
```

**Example login:**

```powershell
$body = '{"email":"coordinator@uam.mx","password":"password","rememberMe":false}'
Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method POST -ContentType "application/json" -Body $body
```

## Quick Start (summary)

```powershell
docker compose -f docker-compose.dev.yml up -d
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

## Docker (API image)

Multi-stage image for Compose and CI (`SPEC-009`). The **`docker`** Spring profile connects to PostgreSQL hostname `db` on port **5432** inside the network (not `localhost`).

### Build image

From the repository root (`sapcyti-api/`):

```powershell
docker build -t sapcyti-api:local .
```

### Environment templates

| File | Purpose |
|------|---------|
| [`.env.docker.example`](.env.docker.example) | Local full stack (SPEC-010) — includes smoke-only `SMOKE_COORDINATOR_PASSWORD` |
| [`.env.preprod.example`](.env.preprod.example) | On-prem preprod placeholders — **no** smoke credentials |
| [`.env.example`](.env.example) | Host JVM + `docker-compose.dev.yml` (port **5433**) |

Copy the relevant example to `.env` for Compose; Spring Boot does not load `.env` automatically when you run the JAR outside Compose.

### Run API container with ephemeral Postgres

Useful to verify the image before the full stack ([`docker-compose.yml`](docker-compose.yml) is SPEC-010):

```powershell
docker network create sapcyti-smoke-net 2>$null
docker run -d --name sapcyti-smoke-db --network sapcyti-smoke-net `
  -e POSTGRES_DB=sapcyti_dev -e POSTGRES_USER=sapcyti -e POSTGRES_PASSWORD=sapcyti_dev_pass `
  postgres:16-alpine
docker run -d --name sapcyti-smoke-api --network sapcyti-smoke-net -p 8080:8080 `
  -e DB_URL=jdbc:postgresql://sapcyti-smoke-db:5432/sapcyti_dev `
  -e DB_USER=sapcyti -e DB_PASS=sapcyti_dev_pass `
  -e CORS_ALLOWED_ORIGINS=http://localhost `
  sapcyti-api:local
```

Wait for Flyway and health (start period up to ~60s), then:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/actuator/health -UseBasicParsing
```

Smoke CRUD against protected endpoints uses HTTP Basic (`coordinator` / password from `SMOKE_COORDINATOR_PASSWORD`, default `changeme` in `.env.docker.example` only). **Do not enable the `docker` profile in production** — see [TECH_DEBT.md](TECH_DEBT.md).

Cleanup:

```powershell
docker rm -f sapcyti-smoke-api sapcyti-smoke-db
docker network rm sapcyti-smoke-net
```

> **Note:** [`docker-compose.dev.yml`](docker-compose.dev.yml) remains the database-only workflow for `mvn spring-boot:run` on the host (port **5433**). It is unchanged by containerized API packaging.

## Full Docker Compose stack (SPEC-010)

Requires **`sapcyti-spa`** as a sibling directory (`../sapcyti-spa` relative to this repo).

### 1. Environment file

```powershell
Copy-Item .env.docker.example .env
```

`.env` is gitignored; values match [`.env.docker.example`](.env.docker.example).

### 2. Start stack

```powershell
docker compose -f docker-compose.yml up --build
```

| URL | Purpose |
|-----|---------|
| [http://localhost](http://localhost) | SPA (Nginx `edge`) — default `EDGE_HTTP_PORT=80` |
| [http://localhost:8888](http://localhost:8888) | Example if port 80 is blocked on Windows (`EDGE_HTTP_PORT=8888` in `.env`) |
| [http://localhost/api/](http://localhost/api/) | API via reverse proxy |
| [http://localhost/api/actuator/health](http://localhost/api/actuator/health) | Health via proxy (E5.3) |
| [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Health direct on API (debug) |

PostgreSQL is **not** published on the host (internal `db:5432` only). Host port **5433** remains for [`docker-compose.dev.yml`](docker-compose.dev.yml) only.

### 3. Smoke verification

After all services are healthy:

```powershell
.\scripts\smoke-stack.ps1
# If edge is not on port 80:
$env:SMOKE_BASE_URL = "http://localhost:8888"
.\scripts\smoke-stack.ps1
```

POSIX (Git Bash / WSL):

```bash
chmod +x scripts/smoke-stack.sh
./scripts/smoke-stack.sh
```

Uses HTTP Basic `coordinator` / `SMOKE_COORDINATOR_PASSWORD` from `.env` (SPEC-009 `docker` profile).

### 4. Clean restart

```powershell
docker compose -f docker-compose.yml down -v
docker compose -f docker-compose.yml up --build
```

Enable [Docker BuildKit](https://docs.docker.com/build/buildkit/) for faster rebuilds (`$env:DOCKER_BUILDKIT=1` on Windows).

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
├── enrollment/             # Enrollment bounded context (future)
├── audit/                  # Audit bounded context (future)
└── shared/                 # Tenant filter, CORS, cross-cutting config
```

See [`src/README.md`](src/README.md) for the full hexagonal layout and dependency rules.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on branching, commits, PRs, and code standards.

## License

MIT — See [LICENSE](LICENSE)
