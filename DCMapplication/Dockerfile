# Multi-stage build for Spring Boot application
# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set working directory for build stage
WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching
COPY pom.xml .

# Download dependencies offline for faster subsequent builds
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src

# Package the application, skip tests for faster builds
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory for runtime
WORKDIR /app

# Copy the built JAR from builder stage with proper ownership
COPY --chown=appuser:appgroup --from=builder /app/target/*.jar app.jar

# Switch to non-root user for security
USER appuser

# Expose the port Spring Boot runs on
EXPOSE 8080

# Simple entrypoint without extra JVM options
ENTRYPOINT ["java", "-jar", "app.jar"]