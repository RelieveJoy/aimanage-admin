# ============================================================
#  爱管理 · 后端镜像（多阶段构建）
#  镜像内用 JDK 17，与本机 JAVA_HOME 保持一致，避免版本错配
# ============================================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# 容器内构建不跑测试：测试依赖数据库，交给 CI 或本机执行
RUN mvn -B clean package -DskipTests

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区与后端时间字段对齐，否则审计时间会差 8 小时
ENV TZ=Asia/Shanghai

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
