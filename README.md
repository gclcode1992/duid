# DUID — 分布式唯一 ID 服务

基于 Java 21 + Spring Boot + Redis + Redisson 的高性能分布式唯一 ID 生成服务。

## 特性

- **毫秒级发号**：热路径纯内存，不每次访问 Redis
- **全局唯一**：时间戳 + workerId + 序列 + 回拨代际
- **时钟回拨保护**：rollbackGen 代际位，基本不停发
- **可配置位布局**：默认方案4（6+13+3），启动自检恰好 64 bit
- **Docker 多副本**：Redis 租约分配 workerId
- **Redis 单机/集群**：Redisson 统一抽象
- **HTTP + SPI**：服务端提供 `/long-id`、`/base62-id`、`/base36-upper-id`；SPI 仅 HTTP 调用

## 快速开始

### 前置条件

- JDK 21
- Maven 3.9+
- Redis（已部署，默认 `127.0.0.1:6379`）

### 构建

```bash
mvn clean package -DskipTests
```

### 启动服务端

**本地开发（dev，端口 18086）：**

```bash
mvn -pl duid-server spring-boot:run
```

或：

```bash
mvn clean package -DskipTests
java -jar duid-server/target/duid-server-1.0.0-SNAPSHOT.jar
```

**本地生产 profile：**

```bash
# Linux / macOS
SPRING_PROFILES_ACTIVE=prod DUID_REDIS_ADDRESS=redis://127.0.0.1:6379 java -jar duid-server/target/duid-server-1.0.0-SNAPSHOT.jar

# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="prod"; java -jar duid-server/target/duid-server-1.0.0-SNAPSHOT.jar
```

### Docker 启动

详见 **[Docker 部署](docs/DOCKER.md)**。

**单副本（prod）：**

```bash
mvn clean package -DskipTests
docker compose -f docker/docker-compose.yml build
docker compose -f docker/docker-compose.yml --env-file .env up -d
```

**多副本（prod，3 实例 + Nginx）：**

```bash
mvn clean package -DskipTests
docker compose -f docker/docker-compose.multi.yml build
docker compose -f docker/docker-compose.multi.yml --env-file .env up -d --scale duid-server=3 --remove-orphans
```

> 不要用 `docker compose up --scale` 搭配默认 `docker/docker-compose.yml`，会端口冲突。

### 获取 ID

```bash
curl http://localhost:18086/long-id
curl http://localhost:18086/base62-id
curl http://localhost:18086/base36-upper-id
```

## 模块说明

| 模块 | 说明 |
|------|------|
| `duid-common` | 位布局配置、校验、发号器核心 |
| `duid-server` | Spring Boot 服务端 |
| `duid-spi` | 对外 SPI 客户端 Jar |

## SPI 使用

详见 **[SPI 使用指南](docs/SPI_USAGE.md)**。

```xml
<dependency>
    <groupId>com.duid</groupId>
    <artifactId>duid-spi</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

```yaml
# application.yml
duid:
  client:
    server-url: http://localhost:18086   # DUID 服务端地址
```

```java
@Service
public class MyService {
    @Autowired
    private DuidClient duidClient;

    public void demo() {
        long id = duidClient.nextId();
        String orderNo = duidClient.nextLongIdWithPrefix("ORD");
    }
}
```

## 文档

- [工程规划](docs/PLAN.md)
- [位布局说明](docs/BIT_LAYOUT.md)
- [配置说明](docs/CONFIGURATION.md)
- [SPI 使用指南](docs/SPI_USAGE.md)
- [Docker 部署](docs/DOCKER.md)

## 默认位布局（方案4）

```text
| 0 | 41bit 时间戳 | 6bit worker | 13bit 序列 | 3bit 回拨 |
```

- 最大 64 个并发副本
- 单实例 8192 ID/毫秒
- 可区分 7 次时钟回拨

## License

MIT

## 作者

gclcode@126.com
