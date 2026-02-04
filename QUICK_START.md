# Quick Reference - Running Your Application

## TL;DR - Start the App in 30 Seconds

### In IntelliJ IDEA:
1. Run → Edit Configurations
2. Create new Spring Boot configuration called "Marketplace Dev"
3. Environment tab → Load `.env.dev`
4. Active profiles: `dev`
5. Click Run ▶️

### From Command Line:
```powershell
cd "G:\Programming\Github\marketplace\marketplace"
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

## What's What

| Profile | Database | Use Case | Command |
|---------|----------|----------|---------|
| `dev` | PostgreSQL `marketplace` on localhost | Local development | `spring.profiles.active=dev` |
| `test` | H2 in-memory | Running tests | `mvn test` |
| `prod` | PostgreSQL (env vars) | Production | Docker image with env vars |

## Common Tasks

### Run the app locally
```powershell
# Load environment variables first
# Then run with:
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Run all tests (includes H2)
```powershell
mvn test
```

### Build production JAR
```powershell
mvn clean package
```

### Check PostgreSQL is running
```powershell
psql -U postgres -h 127.0.0.1 -c "SELECT 1;"
```

### Access GraphQL locally
- After starting: http://localhost:8080/graphiql

### Access application
- After starting: http://localhost:8080

## What Database Your App Uses

| Scenario | Database | Connection | Profile |
|----------|----------|-----------|---------|
| **Running locally in IDE** | PostgreSQL `marketplace` | localhost:5432 | `dev` |
| **Running tests** | H2 in-memory | N/A | `test` |
| **Running in production** | PostgreSQL (cloud) | Environment variables | `prod` |

## Environment Variables (`.env.dev`)

Key variables for local dev:
```
DB_URL=jdbc:postgresql://127.0.0.1:5432/marketplace
DB_USERNAME=postgres
DB_PASSWORD=0000
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=...
MAIL_HOST=smtp.hmailplus.com
... (see .env.dev for complete list)
```

## No More H2 Issues! ✅

Your setup now ensures:
- ✅ Dev uses PostgreSQL (not H2)
- ✅ H2 only loaded for tests (with `-Pwith-h2`)
- ✅ No "Cannot load driver class: org.h2.Driver" errors
- ✅ Access to your existing `marketplace` data
- ✅ Flyway migrations run automatically
- ✅ Database schema validated but not modified

## Files to Know

- **`application.yml`** - Base configuration (all profiles inherit)
- **`application-dev.yml`** - Dev profile overrides
- **`application-test.yml`** - Test profile overrides
- **`application-prod.yml`** - Prod profile overrides
- **`.env.dev`** - Environment variables for local development
- **`src/main/resources/db/migration/`** - Flyway migration scripts

## Troubleshooting

**"Cannot load driver class: org.postgresql.Driver"**
→ Verify PostgreSQL is running and reachable

**"database marketplace does not exist"**
→ Verify PostgreSQL has `marketplace` database

**"Table not found"**
→ Flyway migrations may not have run; check `flyway_schema_history` table

**"Connection refused"**
→ PostgreSQL is not running on port 5432

See `LOCAL_DEV_SETUP.md` for detailed troubleshooting.

