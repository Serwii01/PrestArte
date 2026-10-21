# ===== Compilación =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Primero solo el pom: las dependencias quedan en caché entre builds.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Los tests de integración necesitan una base de datos: se ejecutan aparte.
RUN mvn -q -B package -DskipTests

# ===== Ejecución =====
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 prestarte \
    && mkdir -p /app/data/files && chown -R prestarte /app/data
COPY --from=build /app/target/*.jar app.jar
USER prestarte
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
