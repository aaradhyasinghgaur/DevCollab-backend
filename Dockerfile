# Stage 1: Build the Spring Boot JAR
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

# Copy maven wrapper and pom.xml first for efficient layer caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Convert line endings of mvnw to unix if building on Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies (offline cache)
RUN ./mvnw dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Package application (skip tests for Docker build)
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime container with execution runtimes (Node.js, Python, Java, C/C++)
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Install Node.js, Python3, and compilers for code sandbox
RUN apt-get update && apt-get install -y --no-install-recommends \
    nodejs \
    npm \
    python3 \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root group and user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy built JAR from builder
COPY --from=builder /build/target/*.jar app.jar

# Expose default HTTP port
EXPOSE 8080

# Environment variables with sensible defaults
ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
