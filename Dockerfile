FROM eclipse-temurin:21-jre

WORKDIR /app
COPY build/libs/learning-platform-backend.jar /app/app.jar

EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
