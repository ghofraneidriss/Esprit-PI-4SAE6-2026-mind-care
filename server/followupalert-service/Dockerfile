# Runtime stage using pre-built JAR
FROM eclipse-temurin:17.0.11_9-jre-alpine

LABEL maintainer="MindCare Team"
LABEL description="Follow-up Alert Microservice for MindCare - Patient monitoring and alerts"

# Create app user for security
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser

WORKDIR /app

# Copy pre-built jar from target directory
COPY target/followup-alert-service-0.0.1-SNAPSHOT.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher || exit 1

# Expose port
EXPOSE 8080 9010

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
