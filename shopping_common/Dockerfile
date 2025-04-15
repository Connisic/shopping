FROM openjdk:17-jdk-slim

WORKDIR /app

# 由于这是通用库模块，通常不会直接运行，仅作为依赖被其他服务使用
# 这里仅为了构建镜像，实际不会单独启动
COPY target/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]