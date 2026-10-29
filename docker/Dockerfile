# 快速构建：宿主机先 mvn package，镜像只复制 Fat Jar（不在镜像内拉 Maven 依赖）
# 用法：docker compose -f docker/docker-compose.yml build
# 构建上下文为仓库根目录
# syntax=docker/dockerfile:1

ARG BASE_IMAGE=eclipse-temurin:21-jre-alpine
FROM ${BASE_IMAGE}

ARG APP_VERSION=1.0.0
LABEL org.opencontainers.image.title="duid-server" \
      org.opencontainers.image.description="DUID distributed unique id server" \
      org.opencontainers.image.authors="gclcode@126.com" \
      org.opencontainers.image.version="${APP_VERSION}"

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms64m -Xmx192m -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m" \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai" \
    LOGGING_FILE_PATH=/home/worker/duid

# 时区 + 健康检查 + su-exec（entrypoint 降权）+ 应用用户
RUN apk add --no-cache curl tzdata su-exec \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && addgroup -S -g 10001 duid \
    && adduser -S -u 10001 -G duid duid \
    && mkdir -p /app /home/worker/duid \
    && chown -R duid:duid /app /home/worker/duid

WORKDIR /app

COPY --chown=duid:duid duid-server/target/duid-server-*.jar app.jar
COPY docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# 以 root 进入 entrypoint，修正日志目录权限后 su-exec 降权为 duid
USER root

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/ready" || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
