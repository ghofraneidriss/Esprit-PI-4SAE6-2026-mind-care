FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/volunteer-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8085

CMD ["java", "-jar", "app.jar"]
