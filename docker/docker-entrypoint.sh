#!/bin/sh
# 以 root 启动：确保挂载的日志目录对应用用户可写，再降权启动 JVM
set -e

LOG_DIR="${LOGGING_FILE_PATH:-/home/worker/duid}"
APP_USER="duid"
APP_GROUP="duid"

mkdir -p "$LOG_DIR"

# 卷挂载后目录常为 root 所有，非 root 进程无法创建日志文件
if chown -R "${APP_USER}:${APP_GROUP}" "$LOG_DIR" 2>/dev/null; then
  chmod -R u+rwX,g+rwX "$LOG_DIR" 2>/dev/null || true
else
  echo "[entrypoint] WARN: chown $LOG_DIR failed; trying chmod a+rwx"
  chmod -R a+rwX "$LOG_DIR" 2>/dev/null || true
fi

# 冒烟：确认当前用户可写（降权前用 su-exec 测一次）
if ! su-exec "${APP_USER}:${APP_GROUP}" sh -c "touch '$LOG_DIR/.write-test' && rm -f '$LOG_DIR/.write-test'"; then
  echo "[entrypoint] ERROR: $LOG_DIR is not writable by ${APP_USER}. Check host mount permissions for DUID_LOG_HOST_DIR."
  exit 1
fi

cd /app
exec su-exec "${APP_USER}:${APP_GROUP}" sh -c "exec java \$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"
