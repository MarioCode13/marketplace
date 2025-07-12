@echo off
echo Setting Neon environment variables...

set DB_URL=postgresql://neondb_owner:npg_CPcp9sN7xuMJ@ep-raspy-rice-aba2nr4u-pooler.eu-west-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require
set DB_USERNAME=neondb_owner
set DB_PASSWORD=npg_CPcp9sN7xuMJ
set JWT_SECRET=dev-jwt-secret-key-for-local-development-only

echo Environment variables set successfully!
echo.
echo Now you can run: mvn spring-boot:run -Dspring.profiles.active=dev
echo.
pause 