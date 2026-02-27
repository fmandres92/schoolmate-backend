# ============================================================
# Dockerfile — SchoolMate Hub API
# Multi-stage build: Maven build + JRE slim runtime
# Optimizado para Render free tier (512 MB RAM)
# ============================================================

# --- Stage 1: Build ---
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar solo POM primero (cache de dependencias)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código y compilar (sin tests — se corren en CI)
COPY src ./src
RUN mvn package -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiar JAR del stage de build
COPY --from=build /app/target/schoolmate-hub-api-*.jar app.jar

# Configuración JVM para ambiente con RAM limitada
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Render inyecta PORT en runtime; fallback local 8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
