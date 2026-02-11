# Runtime-only image (JAR is pre-built by CI or locally via ./gradlew bootJar)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy pre-built jar
COPY build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Set JVM options via environment variable
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
