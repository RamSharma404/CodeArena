# ─── Stage 1: Build ───────────────────────────────────
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# ─── Stage 2: Runtime ─────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Render injects PORT as an environment variable
ENV PORT=8080
EXPOSE ${PORT}

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]