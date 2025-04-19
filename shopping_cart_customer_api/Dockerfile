# 多阶段构�?- 第一阶段：构建阶�?
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /build

# 首先复制pom文件进行依赖缓存
COPY ./pom.xml /build/
COPY ./shopping_common/pom.xml /build/shopping_common/
COPY ./shopping_cart_customer_api/pom.xml /build/shopping_cart_customer_api/

# 尝试解析并下载依赖（如果pom文件没有变化，会使用缓存�?
RUN mvn dependency:go-offline -B

# 复制通用模块源代�?
COPY ./shopping_common/src /build/shopping_common/src/

# 复制API源代�?
COPY ./shopping_cart_customer_api/src /build/shopping_cart_customer_api/src/

# 构建应用，跳过测�?
RUN mvn clean package -DskipTests -pl shopping_cart_customer_api -am

# 第二阶段：运行阶�?- 使用更小的基础镜像
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 添加标签
LABEL maintainer="Shopping Team"
LABEL description="Cart Customer API for Shopping Platform"

# 创建非root用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 设置JVM运行参数，限制内存使�?
ENV JAVA_OPTS="-server -Xms96m -Xmx192m -XX"

# 设置时区
ENV TZ=Asia/Shanghai

# 安装基本工具（可选，如需更小镜像可以移除�?
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apk del tzdata

# 从构建阶段复制JAR�?
COPY --from=builder /build/shopping_cart_customer_api/target/*.jar /app/app.jar

# 切换到非root用户
USER appuser

# 暴露服务端口
EXPOSE 8080

# 健康检�?
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/actuator/health || exit 1

# 设置入口�?
ENTRYPOINT ["sh", "-c", "java $-server -Xms96m -Xmx192m -XX -jar app.jar"]
