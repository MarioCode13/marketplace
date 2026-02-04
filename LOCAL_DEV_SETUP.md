# Local Development Setup Guide

## Prerequisites

You have an existing `marketplace` PostgreSQL database with data and a complete Flyway migration setup.

## Quick Start

### Step 1: Ensure PostgreSQL is Running
- PostgreSQL must be running on `localhost:5432`
- Database `marketplace` must exist with your existing data
- Default credentials: username `postgres`, password `0000`

### Step 2: Load Environment Variables
The `.env.dev` file contains all necessary configuration for local development:
```
DB_URL=jdbc:postgresql://127.0.0.1:5432/marketplace
DB_USERNAME=postgres
DB_PASSWORD=0000
```

#### Option A: In IntelliJ IDEA (Easiest)
1. Go to **Run → Edit Configurations**
2. Select your Spring Boot run configuration (or create a new one with name: `Marketplace Dev`)
3. Tab: **Environment**
4. Click the folder icon next to "Environment variables"
5. Select the `.env.dev` file
6. Click **OK**

#### Option B: From Command Line
```powershell
# Windows PowerShell - Load all variables from .env.dev
Get-Content .\.env.dev | ForEach-Object {
    if ($_ -match '^\s*([^=]+)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}

# Run the application
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Step 3: Run the Application
- **Active Profile**: `dev` (important!)
- The dev profile (`application-dev.yml`) is configured with:
  - PostgreSQL datasource pointing to existing `marketplace` database
  - Flyway enabled with migrations from `src/main/resources/db/migration`
  - Hibernate validation mode (`ddl-auto: validate`)

## Configuration Details

### application-dev.yml Setup
```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/marketplace  # ← Existing database
    username: postgres
    password: 0000
  
  jpa:
    hibernate:
      ddl-auto: validate  # ← Validates schema, doesn't modify
  
  flyway:
    enabled: true  # ← Runs migrations automatically on startup
    locations: classpath:db/migration
```

### How It Works
1. Application starts with `spring.profiles.active=dev`
2. Loads `application-dev.yml` configuration
3. Connects to existing `marketplace` database
4. Flyway runs any pending migrations from `src/main/resources/db/migration/V*.sql`
5. Hibernate validates the schema matches the JPA entities
6. Application is ready - all existing listings and users are accessible

## Test Profile (H2 Only)

Tests use a **separate** H2 in-memory database:
- Configured in `application-test.yml`
- Completely isolated from dev/prod databases
- Requires Maven profile: `-Pwith-h2`
- Run tests with: `mvn test`

## Troubleshooting

### "database marketplace does not exist"
**Solution**: 
- Verify PostgreSQL is running: `psql -U postgres -l`
- If database doesn't exist, it was already there before - check your PostgreSQL installation
- Check `DB_URL` in `.env.dev` - should be `jdbc:postgresql://127.0.0.1:5432/marketplace`

### "Cannot load driver class: org.postgresql.Driver"
**Solution**: 
- Verify PostgreSQL JDBC driver is in classpath (should be automatically with Maven)
- Run `mvn clean compile`

### "Table not found" errors
**Solution**: 
- Flyway migrations should have created the schema
- Check `src/main/resources/db/migration/V1__init.sql`
- Verify Flyway ran: look for `flyway_schema_history` table in the database

### "Pre-authenticated entry point called. Rejecting access"
**Solution**: 
- This is likely a CSRF or authentication issue unrelated to database
- Not a database connectivity problem

## Viewing Your Existing Data

Once the app is running with the dev profile:

1. **List existing users**: Connect to PostgreSQL and query the `users` table
2. **List existing listings**: Browse to `http://localhost:8080` or query `listings` table
3. **GraphQL Console**: Available at `http://localhost:8080/graphiql`

The application uses your existing data - no initialization needed!

