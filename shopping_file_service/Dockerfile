FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/app.jar app.jar

EXPOSE 9003

ENTRYPOINT ["java", "-jar", "app.jar"]