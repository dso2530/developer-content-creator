# syntax=docker/dockerfile:1

# --------------------------------------------------
# Build stage
# --------------------------------------------------
FROM docker.io/library/maven:3.9.12-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copier le monorepo pour conserver tous les modules Maven.
COPY . .

# Construire le service et ses dépendances Maven.
# Les credentials GitHub Packages sont montés temporairement
# et ne sont pas conservés dans les couches de l'image.
RUN --mount=type=secret,id=github_token \
    --mount=type=secret,id=github_actor \
    set -eu; \
    umask 077; \
    TOKEN="$(cat /run/secrets/github_token)"; \
    ACTOR="$(cat /run/secrets/github_actor)"; \
    SETTINGS_FILE="$(mktemp)"; \
    trap 'rm -f "$SETTINGS_FILE"' EXIT; \
    printf '%s\n' \
      '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">' \
      '  <servers>' \
      '    <server>' \
      '      <id>github</id>' \
      "      <username>${ACTOR}</username>" \
      "      <password>${TOKEN}</password>" \
      '    </server>' \
      '  </servers>' \
      '</settings>' \
      > "$SETTINGS_FILE"; \
    mvn -B -ntp \
      --settings "$SETTINGS_FILE" \
      -pl backend/repository-service \
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
    /workspace/backend/repository-service/target/repository-service-*.jar \
    /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
