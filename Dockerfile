# Root Dockerfile: builds the Spring Boot backend from the repository root.
#
# This exists because some hosts only look for a Dockerfile at the root of the repository and
# will not follow one nested in a subdirectory. backend/Dockerfile is the same build with the
# context already inside backend/, and Render still uses that one via render.yaml. Keep the two
# in step: a change to the build belongs in both, or the two hosts drift apart silently.
#
# The only difference is the COPY paths, which are prefixed with backend/ here.

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first, so a source-only change does not re-download the world.
COPY backend/pom.xml .
RUN mvn -B -q dependency:go-offline

COPY backend/src ./src
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

# The app binds ${PORT:8080} (application.yml), which is what a serverless container host injects.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
