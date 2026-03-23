# Runtime-only image (JAR is pre-built by CI or locally via ./gradlew bootJar)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Update Alpine packages and install curl
RUN apk update && apk add --no-cache curl

# Download OpenTelemetry Java agent at build time (initContainer 불필요)
RUN curl -fsSL \
    -o /app/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

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
