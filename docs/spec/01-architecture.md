# 01 · 微服务架构约束（ARCHITECTURE）

> 约束对象：后端整体拓扑。MUST 条款为硬约束。

## 1.1 服务拆分（MUST）
系统由 5 个进程组成（gateway + 4 个业务服务），职责严格分离：

| 服务 | 端口 | 职责 | 独立 SQLite |
|------|------|------|-------------|
| gateway | 8080 | 统一入口，按 `/api/{svc}/**` 路由并 StripPrefix=1 | 否 |
| auth-service | 8101 | 用户/角色、JWT 签发与校验 | 是（`auth.db`） |
| school-service | 8102 | 高中/初中校/校额名额/控制线 CRUD + 导入导出 | 是（`school.db`） |
| student-service | 8103 | 考生/成绩/志愿填报 CRUD + 导入导出 | 是（`student.db`） |
| admission-service | 8104 | 录取引擎：聚合数据→校额到校→统招→结果 | 是（`admission.db`） |

**约束**
- `MUST`：录取引擎**不直接持有**学校/考生业务数据，仅通过 HTTP 从 school/student 拉取快照（`/school/export`、`/student/export`）。
- `MUST`：每个业务服务拥有**独立** SQLite 文件，禁止跨库共享表。
- `MUST NOT`：前端**不得**直连 8101~8104，所有流量经 8080 网关。

## 1.2 注册/配置中心（SHOULD）
- 采用**无注册中心**模式：各服务地址在 `application.yml` 中写死（见 `gateway/application.yml` 路由表与 `admission/application.yml` 的 `app.*.base-url`）。
- `SHOULD`：因无注册中心，服务间调用地址集中配置，避免散落硬编码。
- ⚠️ **待拷问**：无注册中心在演示/学习场景可接受；若未来要多实例或灰度，是否要补 Nacos？（当前 `MUST NOT` 引入额外中间件，除非你要求）

## 1.3 服务间鉴权（MUST）
- 所有业务服务（school/student/admission）`MUST` 校验 `Authorization: Bearer <jwt>`。
- JWT 使用**共享对称密钥**（`app.jwt.secret`，各服务 yml 一致），算法与 auth-service 的 `JwtUtil` 一致。
- admission-service 调用 school/student 时，`MUST` 自带一个 **admin 凭据获取的 JWT**（`AuthTokenProvider` 用 `app.admin.username/password` 登录换 token），因为 `/export` 端点受保护。
- `MUST NOT`：admin 凭据明文仅存于 admission 的 yml，生产应改为配置中心/密钥管理（本期模拟允许）。

## 1.4 数据隔离与一致性（SHOULD）
- SQLite 为文件库，`SHOULD` 控制并发写（Hibernate 默认连接池 + SQLite 单写）。模拟数据量小（百级），可接受。
- ⚠️ **待拷问**：是否需要为并发写加 `SQLITE_BUSY` 重试或显式文件锁？当前未加。

## 1.5 开放问题（待你拷问）
1. 无注册中心是否长期可接受，还是本期就预留 Nacos 接口？
2. 录取引擎每次模拟都 `deleteAll()` 重建结果，是否需要保留历史批次快照（多次模拟对比）？
3. 是否需要把 admin 凭据/密钥外置到环境变量而非 yml？
