# Stage 1: Dependency caching & build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2: Minimal non-root JRE runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 1001 appgroup \
    && useradd --system --uid 1001 --gid appgroup --home-dir /app --shell /sbin/nologin appuser

COPY --from=build --chown=appuser:appgroup /app/target/social-shuffle-backend-1.0.jar /app/socialshuffle.jar

USER appuser:appgroup
ENV SERVER_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/api/system/health" || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:InitialRAMPercentage=40.0", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom", "-Xss512k", "-jar", "/app/socialshuffle.jar"]
