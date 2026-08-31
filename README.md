# Rupee API — PaiseWise backend

Spring Boot **microservices** backend for the PaiseWise investing-education app
(the mobile client lives in `rupee-mobile`). Java 17, Maven multi-module,
Spring Cloud (Eureka + Gateway), JWT security, PostgreSQL (database-per-service).

## Architecture

```
                       ┌──────────────────┐
  mobile app  ───────► │   api-gateway     │  :8080   (single entry point)
                       └─────────┬─────────┘
                                 │ routes by path, load-balanced via Eureka
        ┌──────────┬─────────────┼──────────────┬──────────────┬─────────────┐
        ▼          ▼             ▼              ▼              ▼             ▼
   auth :8081  profile :8082  learn :8083  practice :8084  portfolio :8085  community :8086
        │          │             │              │              │             │
     auth-db    profile-db    learn-db     practice-db     portfolio-db   community-db
   (own Postgres schema per service — no shared database)

            discovery-server :8761   (Eureka registry — all services register here)
            common-security          (shared JWT issuing/validation + error handling)
```

| Module | Port | Responsibility |
|--------|------|----------------|
| `discovery-server` | 8761 | Eureka service registry |
| `api-gateway` | 8080 | Spring Cloud Gateway; routes `/auth`,`/profile`,`/learn`,`/practice`,`/portfolio`,`/community` |
| `auth-service` | 8081 | Register / login / JWT issue + refresh-token rotation (BCrypt) |
| `profile-service` | 8082 | Profile, badges, settings |
| `learn-service` | 8083 | Lessons, jargon dictionary, daily quiz |
| `practice-service` | 8084 | Stock universe + paper-trading orders |
| `portfolio-service` | 8085 | Holdings + plain-English P&L |
| `community-service` | 8086 | Beginner-safe Q&A posts + replies |
| `common-security` | — | Shared library: JWT, resource-server config, error shape |

## Security
- **JWT access + refresh.** `auth-service` issues; every service validates via the shared `common-security` filter (stateless, no sessions).
- **Refresh-token rotation** — refresh tokens are stored *hashed*; each use revokes the old token and mints a new pair.
- **BCrypt** password hashing; login returns a generic error to prevent user enumeration.
- **No secrets in source.** `JWT_SECRET` and DB credentials come from environment variables; the committed defaults are for local dev only and MUST be overridden in any real environment.
- **Database-per-service** — services never share a schema; they communicate only over HTTP through the gateway.

## Running locally

### Prerequisites
- JDK 17, Maven 3.9+
- Docker (for the Postgres databases) — OR use the `local` profile with in-memory H2.

### Option A — full stack with Postgres
```bash
docker compose up -d                 # starts 6 Postgres databases
mvn clean install -DskipTests        # build all modules

# In separate terminals (discovery first):
mvn -pl discovery-server spring-boot:run
mvn -pl api-gateway     spring-boot:run
mvn -pl auth-service    spring-boot:run
mvn -pl profile-service spring-boot:run
mvn -pl learn-service   spring-boot:run
mvn -pl practice-service spring-boot:run
mvn -pl portfolio-service spring-boot:run
mvn -pl community-service spring-boot:run
```

### Option B — single service, no Docker (in-memory H2)
```bash
mvn -pl auth-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Smoke test (via gateway)
```bash
# Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"9876543210","name":"Rahul","password":"secret1"}'

# Login → returns { user, tokens:{ accessToken, refreshToken } }
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"9876543210","password":"secret1"}'

# Call a protected endpoint
curl http://localhost:8080/practice/stocks \
  -H "Authorization: Bearer <accessToken>"
```

## Connecting the mobile app
In `rupee-mobile`, point the client at this gateway and disable mocks:
- `apiGatewayUrl = http://<host>:8080`
- `useMocks = false`

The per-service base paths (`/auth`, `/profile`, …) already match the gateway routes.

## Notes
- Content services (`learn`, `practice`, `community`) seed sample data on first
  startup, mirroring the mobile app's mock data so the two line up.
- Configuration is intentionally environment-driven; see each service's `application.yml`.
- This machine has no JDK/Maven installed, so the code was authored but not
  compiled here — build & test on a machine with JDK 17 + Maven.
