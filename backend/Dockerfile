# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first, so a source-only change does not re-download the world.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- run ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run unprivileged. A container that does not need root should not have it.
RUN addgroup -S mathstrokes && adduser -S -G mathstrokes mathstrokes

COPY --from=build /build/target/*.jar app.jar
RUN chown mathstrokes:mathstrokes app.jar
USER mathstrokes

# Free tiers are memory-constrained; let the JVM size itself from the cgroup limit
# rather than assuming the host's total RAM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${PORT:-8080}/api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
