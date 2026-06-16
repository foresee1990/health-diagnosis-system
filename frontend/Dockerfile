# ===== 第一阶段：构建 =====
FROM node:18-alpine AS builder
WORKDIR /app
# 先复制依赖文件
COPY package.json package-lock.json ./
RUN npm ci
# 再复制源码并构建
COPY . .
RUN npm run build

# ===== 第二阶段：Nginx 运行 =====
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80