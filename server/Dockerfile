# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached between source changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/*.jar app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 1001 app \
    && mkdir -p /app/loggs && chown -R app:app /app
USER app

COPY --from=build --chown=app:app /workspace/app.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
