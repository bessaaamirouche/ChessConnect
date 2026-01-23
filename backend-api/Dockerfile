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

# Add a non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

# Create uploads directory with proper permissions BEFORE switching user
RUN mkdir -p /app/uploads/avatars && chown -R spring:spring /app/uploads

# Copy default teacher avatars
COPY --chown=spring:spring uploads/avatars/*.jpg /app/uploads/avatars/

USER spring:spring

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8282

# Use docker profile by default
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]
