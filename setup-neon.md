# Neon PostgreSQL Setup Guide

## Step 1: Create Neon Account and Database

1. **Sign up at [neon.tech](https://neon.tech)**
2. **Create a new project:**
   - Click "Create Project"
   - Project name: `marketplace`
   - Region: Choose closest to your users
   - Plan: Free tier

3. **Get connection details:**
   - After creation, you'll see a connection string like:
   ```
   postgresql://[user]:[password]@[host]/[dbname]?sslmode=require
   ```

## Step 2: Set Environment Variables

Create a `.env` file in your project root with your Neon credentials:

```bash
# Copy from env-template.txt and replace with your actual values
DB_URL=postgresql://your-username:your-password@your-host/your-database?sslmode=require
DB_USERNAME=your-username
DB_PASSWORD=your-password
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
```

## Step 3: Test Local Connection

1. **Set environment variables:**
   ```bash
   # Windows PowerShell
   $env:DB_URL="postgresql://your-username:your-password@your-host/your-database?sslmode=require"
   $env:DB_USERNAME="your-username"
   $env:DB_PASSWORD="your-password"
   $env:JWT_SECRET="your-secret-key"
   
   # Or create a .env file and use spring-dotenv (already in your pom.xml)
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=dev
   ```

3. **Check database migration:**
   - The application should automatically run Flyway migrations
   - Check logs for successful migration messages

## Step 4: Update Render Environment Variables

1. **Go to your Render dashboard**
2. **Select your marketplace service**
3. **Go to Environment tab**
4. **Add/Update these variables:**
   ```
   DB_URL=postgresql://your-username:your-password@your-host/your-database?sslmode=require
   DB_USERNAME=your-username
   DB_PASSWORD=your-password
   JWT_SECRET=your-production-jwt-secret
   ```

## Step 5: Verify Setup

1. **Test GraphQL endpoint:**
   ```bash
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{"query": "{ getCategories { id name } }"}'
   ```

2. **Check health endpoint:**
   ```bash
   curl http://localhost:8080/health
   ```

## Troubleshooting

### Connection Issues
- **SSL Mode:** Neon requires SSL. The connection string includes `?sslmode=require`
- **Connection Pooling:** Updated HikariCP settings for Neon's serverless nature
- **Timeout Settings:** Optimized for Neon's connection limits

### Migration Issues
- **Flyway:** Configured to run automatically on startup
- **Baseline:** Set to `baseline-on-migrate: true` for existing databases
- **Logs:** Check Flyway logs for migration status

### Performance Tips
- **Connection Pool:** Reduced pool size for Neon's free tier limits
- **Idle Timeout:** Set to 5 minutes to avoid connection drops
- **Max Lifetime:** Set to 15 minutes for Neon's connection limits

## Neon Free Tier Limits

- **Storage:** 3GB
- **Compute:** 0.5 CPU, 1GB RAM
- **Connections:** 10 concurrent connections
- **Autoscaling:** Automatically scales to zero when not in use

## Security Notes

- **SSL:** Always use SSL with Neon (included in connection string)
- **JWT Secret:** Use a strong, unique secret in production
- **Environment Variables:** Never commit credentials to version control
- **Connection String:** Keep your connection string secure

## Next Steps

1. **Test locally** with Neon database
2. **Deploy to Render** with updated environment variables
3. **Monitor performance** using Neon's dashboard
4. **Set up backups** (Neon provides automatic backups)
5. **Consider upgrading** to paid plan for production use 