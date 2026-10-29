# DUID 分布式唯一 ID — 工程规划

> 本文档为已确认的工程方案归档，供后续查阅与演进参考。

## 1. 目标

构建基于 Java 21 + Maven + Spring Boot + Redis + Redisson 的分布式唯一 ID 服务：

- 对外提供 **HTTP 接口**（`GET /long-id`，返回纯十进制数字）
- 提供 **SPI Jar**，业务系统引入 Jar + 配置服务端地址即可使用
- **高性能**：发号热路径纯内存，P99 < 1ms
- **高可靠**：处理服务端异常、时钟回拨，保证 ID 不重复
- **可部署**：Docker 多副本；Redis 支持单机与集群

## 2. 技术决策

| 决策 | 选择 | 原因 |
|------|------|------|
| ID 形态 | 64 位有符号 long（正数，≤19 位十进制） | 满足「纯数字且不超过 32 位十进制」 |
| 默认位布局 | 方案4：41+6+13+3 | 64 副本、8192/ms、回拨 7 次 |
| 位布局 | 可配置，启动自检 **恰好 64 bit** | 灵活调整且占满 long 空间 |
| 时钟回拨 | rollbackGen 代际位，不停发 | gen 用尽时自旋等待 |
| WorkerId | Redis 租约分配（0..2^workerBits-1） | 适配 Docker 多副本，不绑宿主机 IP |
| Redis 用途 | 租约 + 水位，不参与每次发号 | 保证毫秒级性能 |

## 3. 模块结构

```
duid/
├── duid-common/     # 位布局、校验、发号器核心
├── duid-spi/        # 对外 SPI Jar
├── duid-server/     # Spring Boot 服务端
├── docker/          # Dockerfile、compose、bat 脚本
├── docs/            # 规划与说明文档
└── README.md
```

## 4. ID 位布局（默认方案4）

```text
| 0 | 41bit 时间戳 | 6bit worker | 13bit 序列 | 3bit 回拨 |
```

拼装：`ID = (ts<<22) | (worker<<16) | (seq<<3) | gen`

约束：`1 + timestamp + worker + sequence + rollback == 64`

备选方案参考见 [BIT_LAYOUT.md](BIT_LAYOUT.md)。

## 5. 核心机制

### 5.1 WorkerId 租约

- Key：`duid:worker:{id}`，SET NX + TTL + 心跳续约
- 丢租约：停止发号，尝试重新注册
- readiness：未拿到 workerId 时不对外服务

### 5.2 时钟回拨

| 场景 | 策略 |
|------|------|
| now > lastTs | rollbackGen=0，序列归零 |
| now == lastTs | 序列+1，溢出等下一毫秒 |
| now < lastTs | rollbackGen++，序列归零；gen 用尽则自旋等待 |
| 进程重启 | 从 Redis 恢复 `{lastTs, rollbackGen}` |

Redis 水位 Key：`duid:clock:{workerId}`

### 5.3 发号热路径

启动抢 worker → 内存发号（不每次访问 Redis）→ 异步刷新水位

## 6. 对外接口

| 路径 | 说明 |
|------|------|
| 路径 | 说明 |
|------|------|
| `GET /long-id` | 纯十进制 long 型 ID |
| `GET /base62-id` | Base62 压缩 ID（0-9、A-Z、a-z） |
| `GET /base36-upper-id` | Base36 大写压缩 ID（仅 0-9、A-Z） |
| `GET /ready` | 就绪探针 |
| `GET /actuator/health` | 健康检查 |
| `GET /ready` | 已就绪（含 workerId）才返回 200 |

## 7. SPI

```java
DuidClient client = DuidClients.create("http://duid-server:8080");
long id = client.nextId();
String base62 = client.nextBase62Id();
String base36Upper = client.nextBase36UpperId();
```

## 8. 验证计划

1. BitLayoutValidator：64 bit 恰好校验
2. 并发唯一性测试
3. 时钟回拨 gen 递增测试
4. 多 worker 租约测试
5. Redis single/cluster 启动

## 9. 明确不采用

- 32-bit 整型作全局主键
- 每次发号 Redis INCR
- SPI 直连 Redis

---

*文档版本：1.0 | 确认日期：2026-08-22 | 作者：gclcode@126.com*
