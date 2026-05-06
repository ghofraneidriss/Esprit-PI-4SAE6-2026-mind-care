FROM eclipse-temurin:17-jre
WORKDIR /app
COPY ordonnance_et_medicaments/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
