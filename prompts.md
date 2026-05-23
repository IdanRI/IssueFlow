# IssueFlow - AI-Assisted Development Documentation

## AI Tooling

| Tool | Purpose |
|------|---------|
| **Claude Code CLI** (claude-opus-4-6) | Primary development agent - architecture, implementation, testing |
| **Claude Code Plan Mode** | Structured planning with approval gates before any code generation |
| **Swagger UI** (SpringDoc OpenAPI) | Interactive API documentation and manual verification |

## Development Methodology

I followed a **plan-first, implement-second** approach to maintain full control over architectural decisions while leveraging AI for efficient code generation. Every phase required my explicit approval before the agent proceeded, ensuring the codebase reflects deliberate engineering choices rather than unreviewed generated code.

### Workflow

```
Requirements Analysis → Architecture Plan → Review & Approve → Phased Implementation → Verification → Refinement
```

---

## Key Prompts & Interaction Flow

### 1. Initial Architecture & Planning

> Analyze the skeleton project structure, the requirements document (TDP_issueflow_requirements.pdf), and the README API contract. Design a comprehensive implementation plan following SOLID principles with a layered architecture. The code must be modular and reusable. Present the plan for my approval before writing any code.

**Why this prompt:** I deliberately requested a plan-first approach to ensure I could review architectural decisions (entity relationships, authentication strategy, error handling patterns) before any code was generated. This prevents rework and ensures the architecture is intentional.

**Result:** The agent produced a 12-phase implementation plan covering:
- Layered package structure (Controller → Service → Repository)
- 9 JPA entities with field-level constraints and relationship mappings
- Key trade-off decisions (e.g., explicit audit logging vs. AOP, DB-backed JWT blacklist vs. in-memory)
- Dependency-ordered implementation sequence
- Testing strategy (unit + integration with separate H2 test profile)

I reviewed and approved the plan before any implementation began.

### 2. Phased Implementation

After plan approval, I directed the agent through each phase sequentially, verifying test results at each stage:

- **Phase 1-2:** Foundation (enums, entities, repositories) + JWT Authentication
- **Phase 3-4:** Project/Ticket CRUD with audit logging + Comments with @mention parsing
- **Phase 5-6:** Ticket dependencies (blocker enforcement) + File attachments (type/size validation)
- **Phase 7-8:** CSV export/import + Soft delete with ADMIN-only restore
- **Phase 9-10:** Auto-assignment (least-loaded algorithm) + Auto-escalation scheduler
- **Phase 11-12:** Test suite (48 tests) + Documentation

### 3. Verification & Bug Fixing

> Run the full application against the PostgreSQL database and systematically verify every API endpoint matches the README specification. Test all business rules including edge cases.

**Why this prompt:** Automated tests validate logic in isolation, but I wanted end-to-end verification against the actual database to catch integration issues (serialization mismatches, SQL dialect differences, configuration problems).

**Issues found and fixed during verification:**
- `schema.sql` empty file causing EntityManagerFactory failure on startup — fixed by setting `sql.init.mode: never`
- JSON field serialized as `"overdue"` instead of `"isOverdue"` — fixed with `@JsonProperty` annotation
- Users could delete their own account, invalidating their JWT session — added self-deletion guard

### 4. Dockerization

> Containerize the application with a multi-stage Dockerfile for optimized image size. Configure Docker Compose with health checks so the app waits for PostgreSQL readiness. Add a Makefile for common operations.

**Why this prompt:** Production-ready deployment requires containerization. I specified multi-stage builds (JDK for build, JRE for runtime) to minimize the final image size, and health checks to prevent race conditions between the app and database containers.

---

## Architecture Decisions

Each decision was reviewed and approved during the planning phase:

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Audit logging | Explicit service calls (not AOP) | Cleaner code, supports SYSTEM vs USER actor distinction for auto-assignment/escalation |
| JWT invalidation | DB-backed token blacklist | Survives server restarts, testable with H2, no external cache dependency |
| Concurrency control | `@Version` optimistic locking | Prevents concurrent ticket/comment edits without pessimistic lock overhead |
| Attachment storage | Database BLOB (`@Lob byte[]`) | Simplifies deployment — no filesystem mounts or S3 configuration needed |
| Entity-DTO mapping | Static mapper classes | Zero dependencies (no MapStruct/ModelMapper), explicit and debuggable |
| @Mention parsing | Regex `@(\w+)` + case-insensitive DB lookup | Simple, predictable, full re-sync on comment update ensures consistency |
| Auto-assignment | Least-loaded DEVELOPER by open ticket count | Tie-break by `createdAt` for deterministic results |
| Scheduling | `@Scheduled(fixedRate)` | Built-in Spring mechanism, configurable via `application.yaml` |

## Project Statistics

| Metric | Count |
|--------|-------|
| Java source files | 88 |
| Test files | 10 |
| Test methods | 48 |
| JPA entities | 9 |
| REST endpoints | 30+ |
| Lines of code (src/main) | ~3,300 |

## Files Generated

All source files under `src/main/java/com/att/tdp/issueflow/` and test files under `src/test/java/com/att/tdp/issueflow/` were developed through AI-assisted interaction with my review and approval at each phase.
