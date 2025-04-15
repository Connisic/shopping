# 设置JAVA版本
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/app.jar app.jar

EXPOSE 9005

ENTRYPOINT ["java", "-jar", "app.jar"]