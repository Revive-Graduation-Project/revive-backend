# syntax=docker/dockerfile:1
# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# --platform linux/amd64 prevents "exec format error" on Windows Docker Desktop.
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy root parent POM + this service's POM (enables dependency caching layer)
COPY pom.xml ./pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml

# Download all dependencies — ~/.m2 is persisted in a BuildKit cache volume
# so jars are NOT re-downloaded on every build (only when pom.xml changes).
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f auth-service/pom.xml dependency:go-offline -B

# Copy source and build
COPY auth-service/src ./auth-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f auth-service/pom.xml clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/auth-service/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=dev
ENTRYPOINT ["java", "-jar", "app.jar"]
