# syntax=docker/dockerfile:1

# --------------------------------------------------
# Build stage
# --------------------------------------------------
FROM docker.io/library/maven:3.9.12-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copier le monorepo pour conserver tous les modules Maven
# déclarés dans le POM racine.
COPY . .

# Construire le service et ses dépendances Maven.
RUN mvn -B -ntp \
    -pl backend/ai-orchestrator \
    -am \
    -DskipTests \
    clean package

# --------------------------------------------------
# Runtime stage
# --------------------------------------------------
FROM gcr.io/distroless/java21-debian13:nonroot

WORKDIR /app

COPY --from=builder \
    --chown=65532:65532 \
    /workspace/backend/ai-orchestrator/target/ai-orchestrator-*.jar \
    /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
