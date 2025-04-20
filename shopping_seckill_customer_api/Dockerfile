# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Set working directory
WORKDIR /build

# Copy pom files first
COPY ./pom.xml /build/
COPY ./shopping_common/pom.xml /build/shopping_common/
COPY ./shopping_seckill_customer_api/pom.xml /build/shopping_seckill_customer_api/

# Configure Maven settings with Aliyun mirror
RUN mkdir -p /root/.m2 && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
    <mirrors> \
        <mirror> \
            <id>aliyun</id> \
            <mirrorOf>central</mirrorOf> \
            <name>Aliyun Maven Repository</name> \
            <url>https://maven.aliyun.com/repository/public</url> \
        </mirror> \
    </mirrors> \
</settings>' > /root/.m2/settings.xml

# Copy necessary source code
COPY ./shopping_common/src /build/shopping_common/src/
COPY ./shopping_seckill_customer_api/src /build/shopping_seckill_customer_api/src/

# Build application, skip tests
RUN mvn clean package -DskipTests -pl shopping_seckill_customer_api -am

# Stage 2: Extract JAR layers
FROM alpine:3.16 AS extractor
WORKDIR /extract
COPY --from=builder /build/shopping_seckill_customer_api/target/*.jar /extract/app.jar
RUN mkdir -p /extract/dependencies /extract/spring-boot-loader /extract/snapshot-dependencies /extract/application
RUN java -Djarmode=layertools -jar app.jar extract --destination /extract

# Stage 3: Final runtime image
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Add labels
LABEL maintainer="Shopping Team"
LABEL description="Service for Shopping Platform"

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set JVM runtime parameters
ENV JAVA_OPTS="-server -Xms128m -Xmx256m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseContainerSupport -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Set timezone
ENV TZ=Asia/Shanghai

# Copy application in layers (from less frequently changed to most frequently changed)
COPY --from=extractor /extract/dependencies/ ./
COPY --from=extractor /extract/spring-boot-loader/ ./
COPY --from=extractor /extract/snapshot-dependencies/ ./
COPY --from=extractor /extract/application/ ./

# Switch to non-root user
USER appuser

# Expose service port
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Set entrypoint with optimized parameters
ENTRYPOINT ["sh", "-c", "java \ org.springframework.boot.loader.JarLauncher"]
