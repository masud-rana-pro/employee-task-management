# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom first so Docker caches the dependency download layer.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Do not run the app as root.
RUN useradd --system --uid 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

# Free hosting plans give ~512 MB RAM: keep the JVM small.
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=70 -Xss512k -Djava.awt.headless=true"

# The platform injects PORT (Render / Railway); the app reads it (see application.properties).
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
