# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (better caching)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install FFmpeg for video thumbnail generation
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Add a non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

# Create uploads directory with proper permissions BEFORE switching user
RUN mkdir -p /app/uploads/avatars /app/uploads/thumbnails && chown -R spring:spring /app/uploads

# Copy default teacher avatars
COPY --chown=spring:spring uploads/avatars/*.jpg /app/uploads/avatars/

USER spring:spring

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8282

# Use docker profile by default
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-Xms512m", "-Xmx1536m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "app.jar"]
