# 멀티 스테이지 빌드로 최적화
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Gradle 실행 권한 부여 및 빌드
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

# 런타임 이미지
FROM openjdk:17-jre-slim

WORKDIR /app

# 시스템 패키지 업데이트 및 필수 도구 설치
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 애플리케이션 사용자 생성
RUN groupadd -r autocoin && useradd -r -g autocoin autocoin

# 애플리케이션 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 로그 디렉토리 생성 및 권한 설정
RUN mkdir -p /app/logs && chown -R autocoin:autocoin /app

# 사용자 전환
USER autocoin

# 포트 노출
EXPOSE 8080

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 옵션 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
