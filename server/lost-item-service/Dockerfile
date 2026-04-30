# Multi-stage build for lost-item-service
# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy parent pom
COPY ../pom.xml ./

# Copy lost-item-service
COPY . ./lost-item-service/

# Build the application
RUN mvn clean package -DskipTests -pl lost-item-service -am

# Stage 2: Runtime stage
FROM eclipse-temurin:17.0.11_9-jre-alpine

LABEL maintainer="MindCare Team"
LABEL description="Lost Item Microservice for MindCare - Alzheimer patient supervision"

# Create app user for security
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /build/lost-item-service/target/lost-item-service-0.0.1-SNAPSHOT.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher \
    -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 \
    -Dcom.sun.management.jmxremote.rmi.port=9010 || exit 1

# Expose port
EXPOSE 8080 9010

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
