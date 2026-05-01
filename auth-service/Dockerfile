# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# Using the official Maven image which already bundles JDK 17 — no need for
# `apk add maven` (which fails on Alpine because maven is not in the default repo).
# --platform linux/amd64 prevents "exec format error" on Windows Docker Desktop.
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy root parent POM + this service's POM (enables dependency caching layer)
COPY pom.xml ./pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml

# Download all dependencies first (cached unless pom.xml changes)
RUN mvn -f auth-service/pom.xml dependency:go-offline -B

# Copy source and build
COPY auth-service/src ./auth-service/src
RUN mvn -f auth-service/pom.xml clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/auth-service/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=dev
ENTRYPOINT ["java", "-jar", "app.jar"]

