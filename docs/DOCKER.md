# Docker 部署说明

Docker 相关文件均在 `docker/` 目录。`.env` / `.env.example` 仍放在仓库根目录（compose 通过 `--env-file .env` 读取）。

## 启动方式一览

| 场景 | Profile | Compose 文件 | 对外端口 |
|------|---------|--------------|----------|
| 本地 IDE / Jar | `dev` | 无 | `18086` |
| Docker 单副本 | `prod` | `docker/docker-compose.yml` | `18086` |
| Docker 多副本 + Nginx | `prod` | `docker/docker-compose.multi.yml` | `18086` |

> **注意**：多副本必须用 `docker/docker-compose.multi.yml`。`docker/docker-compose.yml` 每个实例都映射 `18086`，**不能** `--scale`。

---

## 0. 前置：构建 Jar / 镜像脚本（Windows）

`docker/Dockerfile` 复制本地已打包的 Fat Jar（Alpine JRE、非 root），构建镜像前必须先打包。

**推荐（`docker/` 下独立 bat，会自动切到仓库根目录）：**

```bat
docker\docker-rmi.bat      rem 停容器 + 删除旧镜像 duid-server:1.0.0 + prune
docker\docker-build.bat    rem mvn package + 按 docker/docker-compose.yml 构建新镜像
docker\docker-build.bat --pull   rem 构建时同时拉取最新基础镜像
docker\docker-run.bat      rem 按 docker/docker-compose.yml 启动单副本（profile=prod；需已有镜像与 .env）
docker\docker-run.bat multi      rem 按 docker/docker-compose.multi.yml 启动多副本
docker\docker-export.bat   rem 导出镜像为 dist/duid-server-1.0.0.tar（可 docker load）
```


手动等价命令（在仓库根目录执行）：

```bash
mvn clean package -DskipTests -pl duid-server -am
docker compose -f docker/docker-compose.yml --env-file .env build
```

未打包直接 `docker compose build` 会报错：`duid-server/target: no such file or directory`。

---

## 1. 本地启动（不用 Docker）

**开发环境（默认 dev，端口 18086）：**

```bash
mvn -pl duid-server spring-boot:run
```

或打包后启动：

```bash
mvn clean package -DskipTests
java -jar duid-server/target/duid-server-1.0.0-SNAPSHOT.jar
```

**生产 profile（端口 8080，Redis 走环境变量）：**

```bash
# Linux / macOS
export SPRING_PROFILES_ACTIVE=prod
export DUID_REDIS_ADDRESS=redis://127.0.0.1:6379
export DUID_REDIS_PASSWORD=你的密码
java -jar duid-server/target/duid-server-1.0.0-SNAPSHOT.jar

# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DUID_REDIS_ADDRESS="redis://127.0.0.1:6379"
$env:DUID_REDIS_PASSWORD="你的密码"
java -jar duid-server/target/duid-server-1.0.0-SNAPSHOT.jar
```

---

## 2. Docker 单副本（开发/测试）

使用 `docker/docker-compose.yml`，`SPRING_PROFILES_ACTIVE=prod`，仅 **1 个** 实例。在仓库根目录执行：

```bash
# 可选：复制并编辑环境变量
cp .env.example .env        # Windows: copy .env.example .env

mvn clean package -DskipTests
docker compose -f docker/docker-compose.yml build
docker compose -f docker/docker-compose.yml up -d
```

带 `.env` 启动：

```bash
docker compose -f docker/docker-compose.yml --env-file .env up -d
```

停止：

```bash
docker compose -f docker/docker-compose.yml down
```

查看日志：

```bash
docker compose -f docker/docker-compose.yml logs -f duid-server
```

---

## 3. Docker 多副本（生产）

使用 `docker/docker-compose.multi.yml`，`SPRING_PROFILES_ACTIVE=prod`，Nginx 统一入口 `18086`。

```bash
cp .env.example .env        # Windows: copy .env.example .env
# 编辑 .env：DUID_REDIS_ADDRESS、DUID_REDIS_PASSWORD 等

mvn clean package -DskipTests
docker compose -f docker/docker-compose.multi.yml build
docker compose -f docker/docker-compose.multi.yml --env-file .env up -d --scale duid-server=3 --remove-orphans
```

调整副本数（例如 5 个）：

```bash
docker compose -f docker/docker-compose.multi.yml --env-file .env up -d --scale duid-server=5 --remove-orphans
```

停止：

```bash
docker compose -f docker/docker-compose.multi.yml down
```

查看状态 / 日志：

```bash
docker compose -f docker/docker-compose.multi.yml ps
docker compose -f docker/docker-compose.multi.yml logs -f duid-server
docker compose -f docker/docker-compose.multi.yml logs -f duid-lb
```

重新构建并滚动重启：

```bash
mvn clean package -DskipTests
docker compose -f docker/docker-compose.multi.yml build
docker compose -f docker/docker-compose.multi.yml --env-file .env up -d --scale duid-server=3 --force-recreate
```

---

## 4. Docker 网络

默认接入已有外部网络 **`app-net001`**（与 Redis 等同网，无需 `docker network connect`）。

`.env` 中 Redis 用**容器名**连接：

```env
DUID_REDIS_ADDRESS=redis://redis-local2:6379
DUID_REDIS_PASSWORD=你的密码
```

**单副本：**

```bash
mvn clean package -DskipTests
docker compose -f docker/docker-compose.yml --env-file .env build
docker compose -f docker/docker-compose.yml --env-file .env up -d
```

**多副本：**

```bash
mvn clean package -DskipTests
docker compose -f docker/docker-compose.multi.yml --env-file .env build
docker compose -f docker/docker-compose.multi.yml --env-file .env up -d --scale duid-server=3 --remove-orphans
```

> 容器内不要用 `localhost` 连 Redis，应使用 Redis 容器名（如 `redis-local2`）。

---

## 5. 环境变量（`.env`）

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DUID_REDIS_ADDRESS` | Redis 地址 | 单副本 `host.docker.internal`；多副本 `redis:6379` |
| `DUID_REDIS_PASSWORD` | Redis 密码 | 空 |
| `DUID_SERVER_MEM_LIMIT` | server 容器内存 | 单副本 `192m`；多副本 `256m` |
| `DUID_SERVER_JAVA_OPTS` | JVM 参数 | 见 `.env.example` |
| `DUID_LB_MEM_LIMIT` | Nginx 容器内存 | `64m` |
| `SPRING_PROFILES_ACTIVE` | 由 compose 注入 | 单/多副本均为 `prod` |
| `DUID_IMAGE_TAG` | 镜像标签 | `1.0.0` |
| `DUID_BASE_IMAGE` | Dockerfile 基础镜像 | `eclipse-temurin:21-jre-alpine` |
| `DUID_LOG_HOST_DIR` | 宿主机日志目录，映射到容器 `/home/worker/duid` | `/home/worker/duid` |

### 日志目录说明

| 场景 | 目录 |
|------|------|
| `dev`（logback-spring.xml） | `d:/worker/duid/duid-server.<hostname>.log` |
| `prod`（logback-spring.xml） | `/home/worker/duid/duid-server.<hostname>.log` |
| 宿主机（compose 映射） | `${DUID_LOG_HOST_DIR}` → 容器 `/home/worker/duid`，默认同路径 |

> 容器以非 root 用户 `duid` 跑应用；`docker/docker-entrypoint.sh` 启动时会 `chown` 日志目录。宿主机请先创建可写目录，例如：`mkdir -p /home/worker/duid && chmod 777 /home/worker/duid`（或按实际用户授权）。

---

## 6. 验证

```bash
curl http://localhost:18086/ready
curl http://localhost:18086/long-id
```

---

## 7. 常见错误

| 错误 | 原因 | 处理 |
|------|------|------|
| `duid-server/target: no such file` | 未执行 `mvn package` | 先打包再 `docker compose -f docker/docker-compose.yml build` |
| `port 18086 already allocated` | 用单副本 compose 做了 `--scale` | 改用 `docker/docker-compose.multi.yml` |
| `Network is unreachable` Redis | 使用了 `host.docker.internal` 或 Redis 不在同一网络 | 用容器名 + `docker network connect` |
| `请在 .env 中设置 DUID_REDIS_ADDRESS` | 未提供 `.env` 或未配置 Redis 地址 | `copy .env.example .env` 并填写 |

---

## 8. 开机自启（Windows）

1. Docker Desktop 勾选 **Start Docker Desktop when you log in**
2. compose 中 `restart: unless-stopped` 会在 Docker 启动后恢复容器

---

*作者：gclcode@126.com*
