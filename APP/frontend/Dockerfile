# ============================================
# 食库管家前端 Dockerfile
# 多阶段构建 - 最小化镜像体积
# ============================================

# ---------- 阶段一：构建阶段 ----------
FROM node:20-alpine AS builder

WORKDIR /app

# 设置 npm 镜像
RUN npm config set registry https://registry.npmmirror.com

# 复制 package.json 和依赖锁定文件
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production=false

# 复制源代码
COPY . .

# 构建生产版本
RUN npm run build

# ---------- 阶段二：运行阶段 ----------
FROM nginx:1.25-alpine AS runtime

# 安装时区和健康检查工具
RUN apk add --no-cache tzdata bash curl

# 设置时区
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 删除默认配置
RUN rm -rf /etc/nginx/conf.d/default.conf

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 创建日志目录
RUN mkdir -p /var/log/nginx && \
    chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/log/nginx

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

# 暴露端口
EXPOSE 80

# 启动命令
CMD ["nginx", "-g", "daemon off;"]