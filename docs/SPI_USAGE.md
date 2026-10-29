# DUID SPI 客户端使用指南

## 1. 引入依赖

### 方式 A：安装到本地 Maven 仓库

在 duid 工程根目录执行：

```bash
mvn clean install -DskipTests
```

业务项目 `pom.xml`：

```xml
<dependency>
    <groupId>com.duid</groupId>
    <artifactId>duid-spi</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 方式 B：安装到私服

将 `duid-spi-1.0.0-SNAPSHOT.jar` 部署到公司 Nexus / Artifactory 后，按私服坐标引用。

---

## 2. Spring Boot 项目（推荐）

### 配置文件 `application.yml`

```yaml
duid:
  client:
    enabled: true                              # 默认 true，可关闭自动装配
    server-url: http://192.168.1.100:18086     # DUID 服务端地址（必填）
    connect-timeout-ms: 200                    # 连接超时（可选）
    read-timeout-ms: 200                       # 读取超时（可选）
    prefetch: 64                               # long 型 ID 预取数量（可选）
```

### 环境变量（Docker / K8s 常用）

| 环境变量 | 对应配置 |
|----------|----------|
| `DUID_CLIENT_SERVER_URL` | `duid.client.server-url` |
| `DUID_CLIENT_CONNECT_TIMEOUT_MS` | `duid.client.connect-timeout-ms` |
| `DUID_CLIENT_READ_TIMEOUT_MS` | `duid.client.read-timeout-ms` |
| `DUID_CLIENT_PREFETCH` | `duid.client.prefetch` |
| `DUID_CLIENT_ENABLED` | `duid.client.enabled` |

### 代码中使用（注入即可）

```java
@Service
public class OrderService {

    private final DuidClient duidClient;

    public OrderService(DuidClient duidClient) {
        this.duidClient = duidClient;
    }

    public void createOrder() {
        long id = duidClient.nextId();
        String orderNo = duidClient.nextLongIdWithPrefix("ORD");
        String shortId = duidClient.nextBase62Id();
        String upperId = duidClient.nextBase36UpperIdWithPrefix("INV");
    }
}
```

启动类无需额外 `@Import`，Jar 内已包含 Spring Boot 自动配置。

---

## 3. 非 Spring 项目（纯 Java）

```java
DuidClient client = DuidClients.builder()
        .serverUrl("http://192.168.1.100:18086")
        .connectTimeoutMs(200)
        .readTimeoutMs(200)
        .prefetch(64)
        .build();

try {
    long id = client.nextId();
    String withPrefix = client.nextLongIdWithPrefix("ORD");
} finally {
    client.close();
}
```

---

## 4. API 对照表

| SPI 方法 | HTTP 接口 |
|----------|-----------|
| `nextId()` | `GET /long-id` |
| `nextLongIdWithPrefix(prefix)` | `GET /long-id?prefix=xxx` |
| `nextBase62Id()` | `GET /base62-id` |
| `nextBase62IdWithPrefix(prefix)` | `GET /base62-id?prefix=xxx` |
| `nextBase36UpperId()` | `GET /base36-upper-id` |
| `nextBase36UpperIdWithPrefix(prefix)` | `GET /base36-upper-id?prefix=xxx` |

---

## 5. 常见问题

**Q：服务端多副本怎么配地址？**  
A：`server-url` 填负载均衡地址，如 `http://nginx-host/duid` 或 K8s Service 名 `http://duid-server:8080`。

**Q：连接失败？**  
A：确认服务端已启动且 `/ready` 返回 200：`curl http://host:port/ready`

**Q：只想手动创建 Bean？**  
A：设置 `duid.client.enabled: false`，自行 `@Bean` 调用 `DuidClients.builder()`。

---

*作者：gclcode@126.com*
