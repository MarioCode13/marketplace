# Stage 1: Build with Maven
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Copy everything
COPY . .

# Package the application
RUN mvn clean package -DskipTests

# Stage 2: Run with lightweight JDK
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy the built jar from previous stage
COPY --from=build /app/target/marketplace-0.0.1-SNAPSHOT.jar app.jar

# Expose the Spring Boot port
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
