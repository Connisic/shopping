# 多阶段构建 - 第一阶段：构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /build

# 首先复制全部的源代码到构建目录
COPY . /build/

# 不使用go-offline，直接使用针对单个模块的构建命令
# 这样避免Maven尝试解析所有模块
RUN mvn clean package -DskipTests -pl shopping_goods_service -am

# 第二阶段：运行阶段 - 使用更小的基础镜像
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 添加标签
LABEL maintainer="Shopping Team"
LABEL description="Goods Service for Shopping Platform"

# 创建非root用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 设置JVM运行参数，限制内存使用
ENV JAVA_OPTS="-server -Xms128m -Xmx256m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"

# 设置时区
ENV TZ=Asia/Shanghai

# 安装基本工具（可选，如需更小镜像可以移除）
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apk del tzdata

# 从构建阶段复制JAR包
COPY --from=builder /build/shopping_goods_service/target/*.jar /app/app.jar

# 切换到非root用户
USER appuser

# 暴露服务端口
EXPOSE 9001

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:9001/actuator/health || exit 1

# 设置入口点
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
