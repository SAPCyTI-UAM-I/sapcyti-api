# Technical Debt — SAPCyTI Backend

> Track known technical debt to manage it intentionally. Update this file when introducing or resolving tech debt.

| ID | Description | Priority | Rationale | Impact | Target Iteration |
|----|-------------|----------|-----------|--------|-----------------|
| TD-009-1 | `DockerSecurityConfig` exposes HTTP Basic with in-memory `coordinator` user (`@Profile("docker")`) for stack smoke scripts | Medium | `@PreAuthorize` blocks CRUD until Phase 6 JWT; smoke needs a principal | Must not ship with `SPRING_PROFILES_ACTIVE=docker` in preprod/prod | Phase 6 |

## Priority Levels

- **Critical** — Must resolve before next release
- **High** — Should resolve within current iteration
- **Medium** — Plan for next iteration
- **Low** — Resolve when convenient
