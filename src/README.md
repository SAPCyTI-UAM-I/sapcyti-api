# SAPCyTI API — source layout

Hexagonal (ports & adapters) modular monolith under `mx.uam.sapcyti`. Each bounded context owns its
`domain`, `application`, and `infrastructure` packages per [`Architecture.md`](../../Docs/Design/Architecture.md) §6.1.

## Modules (bounded contexts)

| Package | BC | Iteration scope |
|---------|----|-----------------|
| `configuration` | BC-04 Program Configuration | Phase 2+ — graduate program & parameters |
| `identity` | BC-06 Identity & Access | Iteration 3 — JWT, RBAC (`infrastructure/security`) |
| `academic` | BC-02 Academic Management | Phase 4 — students & professors |
| `offering` | BC-03 Academic Offering | Iteration 5 — terms, CSV offer |
| `enrollment` | BC-01 Enrollment | Iteration 5 — selection, approval, export |
| `audit` | BC-05 Audit | Later — audit adapter |
| `shared` | Cross-cutting | Phase 1 — tenant filter, CORS |

## Dependency rule

```
adapter/in → application → domain ← adapter/out
```

Domain must not depend on Spring infrastructure types beyond JPA annotations where agreed.

## Multi-tenant runtime

HTTP header `X-Graduate-Id` is read by `shared.tenant.TenantFilter`, stored in `TenantContext`, and mirrored
to MDC keys `graduate_program_id`, `request_id`, `user_id` (future) for structured logs.

See [`technologies/backend.md`](../../Docs/SDD/technologies/backend.md) and SPEC-001–003 under `Docs/SDD/specs/iteration-1/`.
