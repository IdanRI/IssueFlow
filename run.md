# IssueFlow - Setup, Build & Run Guide

## Quick Start (Docker - Recommended)

The fastest way to run the entire application — no Java installation needed:

```bash
docker compose up --build -d
```

This builds the app image, starts PostgreSQL, waits for the database to be healthy, and launches the application. Once ready, open:

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **API Base URL:** http://localhost:8080

To stop:
```bash
docker compose down
```

To stop and remove all data:
```bash
docker compose down -v
```

On **Linux/Mac** with `make` installed, you can also use:
```bash
make up       # Build and start
make down     # Stop
make clean    # Stop and remove data
make logs     # Follow app logs
```

---

## Manual Setup (Local Development)

### Prerequisites

- **Java 21** (JDK) installed and on PATH
- **Docker** installed and running (for PostgreSQL)

### 1. Start the Database

```bash
docker compose up -d db
```

This starts a PostgreSQL instance on `localhost:5432` with database/username/password all set to `issueflow`.

### 2. Build the Project

```bash
./mvnw clean package
```

This compiles the code, runs all 48 tests (using H2 in-memory DB), and packages a runnable JAR.

To skip tests during build:
```bash
./mvnw clean package -DskipTests
```

### 3. Run the Application

```bash
java -jar target/issueflow-0.0.1-SNAPSHOT.jar
```

The application starts on **http://localhost:8080**.

### 4. Run Tests

```bash
./mvnw test
```

Tests use H2 in-memory database in PostgreSQL compatibility mode — no external database needed.

---

## Seed Data

On first startup, the application automatically creates 3 default users:

| Username | Password  | Role      |
|----------|-----------|-----------|
| admin    | admin123  | ADMIN     |
| dev1     | dev123    | DEVELOPER |
| dev2     | dev123    | DEVELOPER |

## Authentication

All API endpoints (except `POST /auth/login`) require a JWT token.

**Option 1 — Swagger UI (Recommended):**

1. Open http://localhost:8080/swagger-ui/index.html
2. Execute `POST /auth/login` with `{"username":"admin","password":"admin123"}`
3. Copy the `accessToken` from the response
4. Click the **Authorize** button, paste the token, and click **Authorize**
5. All endpoints are now authenticated — test them directly from the UI

**Option 2 — Command line:**

```bash
# Login to get a token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Use the token in subsequent requests
curl http://localhost:8080/users \
  -H "Authorization: Bearer <your-token>"
```

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.2 |
| Persistence | Spring Data JPA (Hibernate) |
| Database | PostgreSQL 16 (runtime), H2 (tests) |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| CSV | Apache Commons CSV |
| Build | Maven (wrapper included) |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito (48 tests) |
