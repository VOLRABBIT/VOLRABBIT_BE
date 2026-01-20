# 1. 실행에 필요한 자바 환경(JRE)만 가져옵니다.
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/*.jar app.jar

# 3. 스프링 부트 실행
ENTRYPOINT ["java", "-jar", "app.jar"]