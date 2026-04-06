# Multi-module build from repository root. Usage:
#   docker build -f docker/Dockerfile --build-arg MODULE=api-gateway -t api-gateway:local .
ARG MODULE=api-gateway

# ---------- Build ----------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
ARG MODULE
COPY . .
RUN mvn -B -ntp package -pl "${MODULE}" -am -DskipTests

# ---------- Runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG MODULE
COPY --from=builder /app/${MODULE}/target/*.jar app.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
