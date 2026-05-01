# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# Standalone build — gateway-service is NOT a child of the parent POM,
# so we only need its own pom.xml (no root pom.xml required).
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Use a reliable mirror to avoid Maven Central connection resets
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>central-mirror</id>
      <mirrorOf>central</mirrorOf>
      <url>https://repo1.maven.org/maven2</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>retry</id>
      <properties>
        <maven.wagon.http.retryHandler.count>3</maven.wagon.http.retryHandler.count>
        <maven.wagon.httpconnectionManager.ttlSeconds>120</maven.wagon.httpconnectionManager.ttlSeconds>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>retry</activeProfile>
  </activeProfiles>
</settings>
EOF

# Copy POM first (dependency caching layer)
COPY pom.xml ./pom.xml

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xss4m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
