# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml ./pom.xml
COPY menu-management-service/pom.xml ./menu-management-service/pom.xml

RUN mvn -f menu-management-service/pom.xml dependency:go-offline -B

COPY menu-management-service/src ./menu-management-service/src
RUN mvn -f menu-management-service/pom.xml clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/menu-management-service/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=dev
ENTRYPOINT ["java", "-jar", "app.jar"]

