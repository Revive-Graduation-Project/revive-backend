# syntax=docker/dockerfile:1
# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml ./pom.xml
COPY client-service/pom.xml ./client-service/pom.xml

# Download all dependencies — ~/.m2 is persisted in a BuildKit cache volume
# so jars are NOT re-downloaded on every build (only when pom.xml changes).
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f client-service/pom.xml dependency:go-offline -B

COPY client-service/src ./client-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f client-service/pom.xml clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/client-service/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=dev
ENTRYPOINT ["java", "-jar", "app.jar"]
