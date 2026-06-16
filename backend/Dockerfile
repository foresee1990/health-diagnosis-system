# ===== 第一阶段：构建 =====
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
# 先只复制依赖文件，利用 Docker 缓存层，依赖没变就不重新下载
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# 再复制源码并打包，跳过测试
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ===== 第二阶段：运行 =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]