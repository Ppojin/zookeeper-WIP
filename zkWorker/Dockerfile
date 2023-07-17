### BUILD
FROM gradle:8.2-jdk17 as builder
WORKDIR /build

# 그래들 파일이 변경되었을 때만 새롭게 의존패키지 다운로드 받게함.
COPY build.gradle settings.gradle /build/
RUN gradle build -x test

COPY . /build/
RUN gradle build -x test

### APP
FROM openjdk:17-ea-slim
WORKDIR /app

# 빌더 이미지에서 jar 파일만 복사
COPY --from=builder /build/build/libs/zkWorker-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

ENV SERVER_PORT=8080
ENV ZOOKEEPER_HOST_NAMES=zk1:2181,zk2:2181,zk3:2181

# root 대신 nobody 권한으로 실행
USER nobody
ENTRYPOINT "java" "-jar" \
    "-Dserver.port=${SERVER_PORT}" \
    "-Dzookeeper.host_names=${ZOOKEEPER_HOST_NAMES}" \
    "zkWorker-0.0.1-SNAPSHOT.jar"