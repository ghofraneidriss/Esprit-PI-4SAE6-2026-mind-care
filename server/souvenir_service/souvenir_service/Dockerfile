# Stage 1 : Runtime JRE allégé
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY target/*.jar app.jar
USER appuser
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
