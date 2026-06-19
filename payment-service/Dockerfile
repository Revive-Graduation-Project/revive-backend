# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml ./pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml

RUN mvn -f payment-service/pom.xml dependency:go-offline -B

COPY payment-service/src ./payment-service/src
RUN mvn -f payment-service/pom.xml clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/payment-service/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=dev
ENTRYPOINT ["java", "-jar", "app.jar"]
