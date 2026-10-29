# 配置说明

## 1. 服务端配置（duid-server）

### 配置文件分离

| 文件 | 环境 | 说明 |
|------|------|------|
| `application.yml` | 公共 | 位布局、worker、watermark 等共享配置 |
| `application-dev.yml` | 开发 | 端口 18086、本地 Redis |
| `application-prod.yml` | 生产 | 端口 8080、Redis 走环境变量 |
| `logback-spring.xml` | 全部 | 日志格式/滚动/第三方降噪；dev=`com.duid` DEBUG，prod=WARN+仅控制台 |

默认激活 **dev**（`spring.profiles.active`），可通过环境变量覆盖：

```bash
# 本地开发（默认 dev）
mvn -pl duid-server spring-boot:run

# 生产模式
SPRING_PROFILES_ACTIVE=prod java -jar duid-server/target/duid-server-1.0.0-SNAPSHOT.jar
```

Docker 由 compose 注入：`docker/docker-compose.yml` / `docker/docker-compose.multi.yml` 均为 `prod`。

### application.yml（公共部分）

```yaml
duid:
  epoch: "2024-01-01T00:00:00Z"
  id-layout:
    timestamp-bits: 41
    worker-bits: 6
    sequence-bits: 13
    rollback-bits: 3
  worker:
    lease-ttl-seconds: 30
    heartbeat-interval-seconds: 10
  watermark:
    flush-interval-ms: 1000
```

### application-dev.yml

```yaml
server:
  port: 18086
duid:
  redis:
    address: redis://127.0.0.1:6379
    password: rds123456
```

### application-prod.yml

```yaml
server:
  port: 8080
duid:
  redis:
    address: ${DUID_REDIS_ADDRESS:redis://redis:6379}
    password: ${DUID_REDIS_PASSWORD:}
```

### 环境变量

| 变量 | 说明 |
|------|------|
| `DUID_EPOCH` | 时间戳起点 |
| `DUID_ID_LAYOUT_TIMESTAMP_BITS` | 时间戳位数 |
| `DUID_ID_LAYOUT_WORKER_BITS` | worker 位数 |
| `DUID_ID_LAYOUT_SEQUENCE_BITS` | 序列位数 |
| `DUID_ID_LAYOUT_ROLLBACK_BITS` | 回拨位数 |
| `DUID_REDIS_MODE` | single / cluster |
| `DUID_REDIS_ADDRESS` | Redis 地址 |
| `DUID_REDIS_PASSWORD` | Redis 密码 |
| `SPRING_PROFILES_ACTIVE` | Spring 环境：`dev` / `prod` |
| `DUID_LOG_HOST_DIR` | Docker 宿主机日志映射（默认 `/home/worker/duid` → 容器同路径） |
| `DUID_SERVER_MEM_LIMIT` | duid-server 容器内存上限（如 `256m`） |
| `DUID_SERVER_JAVA_OPTS` | JVM 参数（`-Xmx` 建议约为容器内存 75%） |
| `DUID_LB_MEM_LIMIT` | Nginx 负载均衡容器内存上限 |

## 2. SPI 客户端配置（Spring Boot）

```yaml
duid:
  client:
    enabled: true
    server-url: http://duid-server:8080   # 服务端地址（必填）
    connect-timeout-ms: 200
    read-timeout-ms: 200
    prefetch: 64
```

环境变量：`DUID_CLIENT_SERVER_URL=http://host:port`

完整说明见 [SPI_USAGE.md](SPI_USAGE.md)。

### 编程式创建（非 Spring）

```java
DuidClient client = DuidClients.builder()
    .serverUrl("http://localhost:8080")
    .connectTimeoutMs(200)
    .readTimeoutMs(200)
    .prefetch(64)
    .build();
```

## 3. Docker Compose 启动命令

默认接入外部网络 **`app-net001`**（与 Redis 同网）。

| 文件 | Profile | 用途 |
|------|---------|------|
| `docker/docker-compose.yml` | `prod` | 单副本，端口 `18086` |
| `docker/docker-compose.multi.yml` | `prod` | 多副本 + Nginx，端口 `18086` |

内存与 JVM 在 `.env` 中配置（见 `.env.example`）。完整说明见 [DOCKER.md](DOCKER.md)。

**单副本：**

```bash
mvn clean package -DskipTests
docker compose -f docker/docker-compose.yml build
docker compose -f docker/docker-compose.yml --env-file .env up -d
```

**多副本（3 实例）：**

```bash
mvn clean package -DskipTests
docker compose -f docker/docker-compose.multi.yml build
docker compose -f docker/docker-compose.multi.yml --env-file .env up -d --scale duid-server=3 --remove-orphans
```

> 不要用 `docker compose up --scale` 搭配默认 `docker/docker-compose.yml`，每个实例都会抢占 `18086` 端口。

## 4. Redis Key 说明

| Key | 用途 | 值 |
|-----|------|-----|
| `duid:worker:{id}` | worker 租约 | 实例标识（hostname:uuid） |
| `duid:clock:{workerId}` | 发号水位 | `lastTs:rollbackGen` |

## 5. 切换位布局示例

### 方案1（256 副本，4096/ms，回拨 3 次）

```yaml
duid:
  id-layout:
    timestamp-bits: 41
    worker-bits: 8
    sequence-bits: 12
    rollback-bits: 2
```

验证：41+8+12+2+1 = 64 ✓
