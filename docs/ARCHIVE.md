# 将 Cursor 规划方案归档副本（与 docs/PLAN.md 同步）

本文件为方案确认时的完整规划归档，详细设计见：

- [docs/PLAN.md](PLAN.md) — 工程规划总览
- [docs/BIT_LAYOUT.md](BIT_LAYOUT.md) — 位布局与方案对照
- [docs/CONFIGURATION.md](CONFIGURATION.md) — 配置项说明

## 已确认决策

- 默认位布局：**方案4（41+6+13+3）**
- 位布局：**可配置**，启动自检总位数 **恰好 64**
- 时钟回拨：**rollbackGen 代际位**，不停发，gen 用尽自旋等待
- 部署：Docker 多副本 + Redis 单机/集群
- 对外接口：`GET /long-id`、`GET /base62-id`、`GET /base36-upper-id`
- SPI：HTTP 预取 + Base62 / Base36 大写压缩端 ID

*归档日期：2026-08-22 | 作者：gclcode@126.com*
