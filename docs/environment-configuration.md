# Kuter — Environment Configuration Specification

## 1. Overview & Profile Architecture

Kuter separates configuration across environments using Spring Boot's profile mechanism, inspired by the engineering conventions observed in the **Amoudi Financial System** reference project.

```text
backend/src/main/resources/
├── application.yml         # Shared baseline defaults, Actuator, JPA common settings, logging format
├── application-dev.yml     # Local development (PostgreSQL, developer logs, safe local defaults)
├── application-prod.yml    # Production deployment (strict external env vars, disabled debug, restricted actuator)
└── application-test.yml    # Automated test isolation (isolated test DB, schema validation, quiet logging)
```

---

## 2. Configuration Profiles Breakdown

### A. Shared Baseline (`application.yml`)

- **Purpose:** Environment-agnostic defaults common across all execution modes.
- **Key Settings:**
  - `server.port`: `${PORT:${SERVER_PORT:8080}}`
  - `spring.config.import`: Automatically imports `optional:file:.env[.properties]` and `optional:file:../.env[.properties]`.
  - `spring.jpa.open-in-view`: Explicitly set to `false` to avoid anti-pattern lazy loading in web threads.
  - `spring.jpa.hibernate.ddl-auto`: Strictly `validate`. Flyway owns all schema alterations.
  - `spring.flyway.baseline-on-migrate`: `true`
  - `management.endpoints.web.exposure.include`: `health,info`
  - `logging.pattern.console`: Structured log format containing timestamp, thread, level, correlation ID (`[CID:%X{correlationId:-none}]`), logger, and message.

### B. Local Development (`application-dev.yml`)

- **Purpose:** Fast, predictable developer experience with local PostgreSQL support.
- **Active Trigger:** Activated by default (`SPRING_PROFILES_ACTIVE=dev`).
- **Database Resolution:**
  - URL: `jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:kuter_dev}`
  - Username: `${DB_USERNAME:postgres}`
  - Password: `${DB_PASSWORD:1122}` (safe local default, overrideable via `.env`)
  - Pool: HikariCP with pool size of 10.
- **Logging:** `DEBUG` enabled for `com.kuter` and `org.flywaydb`.

### C. Automated Test Isolation (`application-test.yml`)

- **Purpose:** Fast, deterministic unit and integration testing without risking development or production database corruption.
- **Active Trigger:** Annotated with `@ActiveProfiles("test")`.
- **Database Resolution:**
  - Test datasource points to an isolated in-memory or dedicated test database (`kuter_test`).
  - H2 in PostgreSQL compatibility mode is configured as the default test driver:
    `jdbc:h2:mem:kuter_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1`
  - Schema clean is allowed (`clean-disabled: false`) exclusively within the test profile.
- **Safeguard:** Tests cannot connect to or modify `kuter_dev` or production databases.

### D. Production Deployment (`application-prod.yml`)

- **Purpose:** Hardened, secure production runtime.
- **Safeguards & Enforcement:**
  - **Zero Hardcoded Secrets:** Missing database connection settings fail application startup immediately (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`).
  - **No SQL Echoing:** `show-sql` is disabled; `format_sql` is disabled.
  - **Actuator Hardening:** Health endpoint details set to `show-details: never`. Sensitive operational metrics hidden from public access.
  - **Strict CORS:** `app.cors.allowed-origins` must be supplied via `${CORS_ALLOWED_ORIGINS}`. Wildcard (`*`) origins are prohibited.
  - **Flyway Baseline Disabled:** `baseline-on-migrate: false` ensures migrations run strictly in sequence without unverified baselines.

---

## 3. Environment Variables Reference

| Variable Name | Required In | Description | Local Dev Default | Production Example |
| :--- | :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | No | Active Spring profile | `dev` | `prod` |
| `SERVER_PORT` | No | HTTP listening port | `8080` | `8080` |
| `SERVER_ADDRESS` | No | Binding network address | `0.0.0.0` | `0.0.0.0` |
| `DB_HOST` | Dev | PostgreSQL hostname | `localhost` | N/A (`DB_URL` used in prod) |
| `DB_PORT` | Dev | PostgreSQL port | `5432` | N/A |
| `DB_NAME` | Dev | Database name | `kuter_dev` | N/A |
| `DB_USERNAME` | Yes | Database username | `postgres` | `kuter_prod_user` |
| `DB_PASSWORD` | Yes | Database user password | `1122` | `<strong-secret-password>` |
| `DB_URL` | Prod | Full JDBC connection URL | N/A | `jdbc:postgresql://db.prod:5432/kuter_prod` |
| `DB_POOL_MAX_SIZE` | No | HikariCP max pool size | `10` | `20` |
| `CORS_ALLOWED_ORIGINS` | Yes | Comma-separated client origins | `http://localhost:5173,http://localhost:5174` | `https://admin.kuter.com,https://app.kuter.com` |
| `JWT_SECRET` | Yes | 256-bit+ HMAC-SHA signing secret | Dev fallback key | `<openssl rand -hex 32>` |
| `JWT_EXPIRATION_MS` | No | Token expiration in ms | `86400000` (24h) | `86400000` |
| `KUTER_SUBSCRIPTIONS_EXPIRY_CRON` | No | Subscription expiry cron schedule | `0 0 * * * *` (hourly) | `0 0 * * * *` |
| `KUTER_SUBSCRIPTIONS_EXPIRY_SCHEDULER_ENABLED` | No | Enable background expiry job | `true` | `true` |
| `VITE_API_BASE_URL` | React | Base REST API URL | `http://localhost:8080/api/v1` | `/api/v1` |

---

## 4. Execution Commands by Profile

### Development Mode

```bash
# Windows PowerShell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Mode

```bash
# Using external environment variables
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://postgres.production:5432/kuter_prod
export DB_USERNAME=kuter_app
export DB_PASSWORD=SecretProductionPassword123!
export CORS_ALLOWED_ORIGINS=https://admin.kuter.com,https://app.kuter.com
export JWT_SECRET=c3a50e82c5f10b7a8d423985fa6b3e792e3a89045ef92a813876cd1209b5e341

./mvnw spring-boot:run
```

### Running Test Suite

```bash
# Windows PowerShell
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```
