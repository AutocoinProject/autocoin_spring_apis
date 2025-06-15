# ====================================
# Multi-stage build for production
# ====================================

# Build stage
FROM eclipse-temurin:17-jdk as builder

# Install build dependencies
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy gradle files
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle .
COPY build.gradle .

# Download dependencies (layer caching)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build application
RUN ./gradlew bootJar --no-daemon -x test

# Production stage
FROM eclipse-temurin:17-jre

# Create non-root user
RUN groupadd -r autocoin && useradd -r -g autocoin autocoin

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    curl \
    dumb-init \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Create directories
RUN mkdir -p /app/logs /app/temp && \
    chown -R autocoin:autocoin /app

# Copy built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy configuration files
COPY docker/entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

# Set ownership
RUN chown -R autocoin:autocoin /app

# Switch to non-root user
USER autocoin

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimization for production
ENV JAVA_OPTS="-server \
    -Xms1g \
    -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Seoul"

# Expose port
EXPOSE 8080

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]
CMD ["./entrypoint.sh"]
