# =========================================================
# Stage 1: Build stage - compile and package the application
# =========================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Leverage Docker layer caching: copy pom.xml first and download dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source code and build the application
COPY src ./src
RUN mvn -B clean package -DskipTests

# =========================================================
# Stage 2: Runtime stage - minimal JRE image
# =========================================================
FROM eclipse-temurin:17-jre-jammy AS runtime

# Install wget for container health checks
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user/group to run the application
RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /build/target/app.jar app.jar

# Ensure the non-root user owns the application files
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
