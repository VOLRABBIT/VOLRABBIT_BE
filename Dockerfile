# 1. 빌드 단계 (Builder Stage)
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# [최적화] Gradle 설정 파일들만 먼저 복사합니다.
# (라이브러리 설치 내역을 캐싱하여, 코드 한 줄 고칠 때마다 라이브러리를 다시 받는 것을 방지함)
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# 이제 나머지 소스 코드를 복사합니다.
COPY src ./src

# 빌드 실행 (테스트 생략)
RUN ./gradlew clean build -x test

# -----------------------------------------------------------

# 2. 실행 단계 (Run Stage)
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일만 복사 (경로 주의: build/libs/*.jar)
COPY --from=builder /app/build/libs/*.jar app.jar

# 스프링 부트 기본 포트
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]