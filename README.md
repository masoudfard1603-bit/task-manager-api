# Task Manager API

[![CI](https://github.com/YOUR_USERNAME/task-manager-api/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/task-manager-api/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A production-shaped **REST API** for managing tasks, built with **Spring Boot 3**, **Java 17**, and **JWT authentication**.
Designed as a portfolio project to demonstrate a clean, layered architecture, per-user data ownership, containerization, and a real CI pipeline — not just CRUD.

## Highlights

- 🔐 **JWT authentication** — register/login, stateless sessions, BCrypt password hashing
- 👤 **Per-user ownership** — each task belongs to a user; only the owner (or an admin) can view/edit/delete it
- 🏗️ **Clean layered architecture** — Controller → Service → Repository, DTOs decoupled from entities
- ✅ **Validation & centralized error handling** — consistent JSON error responses (400/401/403/404/409/500)
- 🐳 **Dockerized** — multi-stage Dockerfile + `docker-compose` with a real PostgreSQL database
- ⚙️ **CI pipeline** — GitHub Actions runs the full test suite and builds the Docker image on every push
- 🧪 **Layered test suite** — unit tests (Mockito), web-layer tests (MockMvc), repository tests (`@DataJpaTest`), and a full end-to-end test against a real Postgres container (Testcontainers)
- 📖 **Interactive API docs** — Swagger UI via springdoc-openapi
- ❤️ **Health checks** — Spring Boot Actuator, wired into the Docker healthcheck

## Tech Stack

Java 17 · Spring Boot 3.2 (Web, Data JPA, Security, Validation, Actuator) · PostgreSQL · H2 (local/dev) · JWT (jjwt) · Lombok · springdoc-openapi · JUnit 5 · Mockito · Testcontainers · Docker

## Architecture

```
Client
  │  JWT Bearer token
  ▼
TaskController / AuthController
  │
  ▼
TaskService / AuthService   ← business rules, ownership checks
  │
  ▼
TaskRepository / UserRepository  (Spring Data JPA)
  │
  ▼
PostgreSQL (Docker) / H2 (local dev)
```

Every request to `/api/v1/tasks/**` passes through a `JwtAuthenticationFilter` that validates the
`Authorization: Bearer <token>` header and populates the Spring Security context. The service layer
then checks that the authenticated user owns the task being accessed, unless they hold the `ADMIN` role.

## Project Structure

```
src/main/java/com/example/taskmanager/
├── controller/     REST controllers (Auth, Task)
├── service/        Business logic (interfaces + impl)
├── security/       JWT filter, JwtService, SecurityConfig, UserDetailsService
├── repository/     Spring Data JPA repositories
├── model/          JPA entities and enums (Task, User, TaskStatus, Role)
├── dto/            Request/response DTOs
├── mapper/         Entity <-> DTO mapping
└── exception/      Custom exceptions and global handler

src/test/java/com/example/taskmanager/
├── service/        Unit tests (Mockito)
├── controller/      Web-layer tests (MockMvc + @WithMockUser)
├── repository/      Repository tests (@DataJpaTest)
└── integration/      Full end-to-end test (Testcontainers + real Postgres)
```

## Getting Started

### Option A — Run with Docker (recommended, uses real PostgreSQL)

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`.

### Option B — Run locally with Maven (uses in-memory H2, no Docker needed)

```bash
mvn spring-boot:run
```

### Run the tests

```bash
mvn test
```

> The end-to-end test in `integration/` uses Testcontainers and needs a Docker daemon available
> (this is automatic in GitHub Actions CI). Unit, web-layer, and repository tests run without Docker.

### Explore the API

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`
- H2 console (local/dev only): `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:taskdb`, user: `sa`, empty password)

## API Endpoints

### Auth

| Method | Endpoint                  | Auth required | Description             |
|--------|-----------------------------|----------------|---------------------------|
| POST   | `/api/v1/auth/register`     | No             | Create an account, get a JWT |
| POST   | `/api/v1/auth/login`        | No             | Log in, get a JWT         |

### Tasks (all require `Authorization: Bearer <token>`)

| Method | Endpoint              | Description                                          |
|--------|------------------------|--------------------------------------------------------|
| POST   | `/api/v1/tasks`        | Create a task, owned by the caller                     |
| GET    | `/api/v1/tasks`        | List tasks — own tasks for users, all tasks for admins  |
| GET    | `/api/v1/tasks/{id}`   | Get a task (owner or admin only)                        |
| PUT    | `/api/v1/tasks/{id}`   | Update a task (owner or admin only)                      |
| DELETE | `/api/v1/tasks/{id}`   | Delete a task (owner or admin only)                      |

### Example flow

```bash
# 1. Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "email": "alice@example.com", "password": "password123"}'

# Response includes a JWT:
# { "token": "eyJhbGciOi...", "tokenType": "Bearer", "username": "alice", "role": "USER" }

# 2. Create a task using the token
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOi..." \
  -d '{"title": "Learn Spring Security", "description": "Add JWT auth", "status": "TODO"}'

# 3. List your tasks
curl http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer eyJhbGciOi..."
```

## Testing Strategy

| Layer                | Tool                          | What it covers                                    |
|------------------------|----------------------------------|------------------------------------------------------|
| Service                | JUnit 5 + Mockito                 | Business rules, ownership checks, edge cases          |
| Web (controller)        | MockMvc + `@WithMockUser`         | Request/response shape, validation, status codes       |
| Repository              | `@DataJpaTest`                    | Query correctness against an embedded database          |
| End-to-end              | Testcontainers + real PostgreSQL  | Full flow: register → login → create/access tasks with real ownership enforcement |

## CI/CD

Every push and pull request to `main` triggers a GitHub Actions workflow that:
1. Sets up JDK 17
2. Runs the full test suite (`mvn test`)
3. Builds the application jar
4. Builds the Docker image, to catch Dockerfile regressions early

See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## Configuration

| Env var                    | Used by         | Purpose                                  |
|-----------------------------|------------------|---------------------------------------------|
| `SPRING_DATASOURCE_URL`      | `docker` profile | PostgreSQL JDBC URL                          |
| `SPRING_DATASOURCE_USERNAME` | `docker` profile | PostgreSQL username                          |
| `SPRING_DATASOURCE_PASSWORD` | `docker` profile | PostgreSQL password                          |
| `APP_JWT_SECRET`             | all profiles     | HMAC signing key for JWTs (min. 32 bytes)     |

**Never commit a real `APP_JWT_SECRET` to source control.** The default in `application.yml` is for local development only.

## Possible Next Steps

- Add pagination and sorting to the list endpoint
- Add refresh tokens / token revocation
- Add role management endpoints for admins
- Deploy to a cloud provider (Render, Railway, Fly.io) with the existing Docker image

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
