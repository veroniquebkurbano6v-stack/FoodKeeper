# ============================================
# 食库管家后端 Dockerfile
# 多阶段构建 - 最小化镜像体积
# ============================================

# ---------- 阶段一：构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 设置 Maven 镜像加速
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

# 复制 pom.xml 和依赖
COPY pom.xml .

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建 JAR 包
RUN mvn clean package -DskipTests -B

# ---------- 阶段二：运行阶段 ----------
FROM eclipse-temurin:17-jre-alpine AS runtime

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建应用目录
RUN mkdir -p /app/logs && \
    chown -R 1000:1000 /app

WORKDIR /app

# 从构建阶段复制 JAR 包
COPY --from=builder /app/target/food-inventory-manager.jar app.jar

# 设置非 root 用户运行
USER 1000:1000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM 优化参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${PROFILE:-prod}"]

# 容器停止前等待时间
STOPSIGNAL SIGTERM