# 多阶段构建 - 第一阶段：构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /build

# 复制pom文件进行依赖缓存
COPY ./pom.xml /build/
COPY ./shopping_common/pom.xml /build/shopping_common/

# 尝试解析并下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码
COPY ./shopping_common/src /build/shopping_common/src/

# 构建应用，跳过测试
RUN mvn clean package -DskipTests -pl shopping_common -am

# 第二阶段：运行阶段 - 使用更小的基础镜像
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 添加标签
LABEL maintainer="Shopping Team"
LABEL description="Common Library for Shopping Platform"

# 由于这是通用库模块，通常不会直接运行，仅作为依赖被其他服务使用
# 这里仅为了构建镜像，实际不会单独启动

# 从构建阶段复制JAR包
COPY --from=builder /build/shopping_common/target/*.jar /app/app.jar

# 设置入口点（通常不会被调用）
ENTRYPOINT ["java", "-jar", "app.jar"]