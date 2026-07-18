# 10 · 当前系统功能全景描述（System Overview）

> 本文档基于 `src/main/java/` 与全部 `application.yml` 的实时代码扫描生成（2026-07-18），用于逐项核对系统现状。
> 定位：**现状快照**（非约束契约），与 00/01~09 互为参照；其中"待裁决项"指向 `08-doc-code-review-2026-07-18.md §5`。
> 架构/数据/算法/接口的权威约束仍以 01~06 为准，DDD 重构基线见 `09-ddd-refactor-design-2026-07-18.md`。

## 1. 核心业务能力
系统模拟**北京市东城区中考招生录取全流程**，推演"校额到校 + 统招"双批次平行志愿录取规则。

- **学校数据管理**：高中（招生计划/层次/高考梯队）、初中校（班数/毕业人数）、校额到校名额分配、全区最低控制线 CRUD。
- **考生与志愿管理**：考生档案、志愿填报（校额≤10 / 统招≤8）、提交锁定、校额资格重算。
- **智能志愿生成器**：按考生成绩、片区、离家距离、学校层次/高考梯队偏好，"理性考生"策略一键生成志愿方案（含校验拦截）。
- **录取模拟引擎**：分数优先、遵循志愿平行投档；校额按"共享名额池(组)"聚合竞争，统招全局平行投档；保留多次模拟历史。
- **数据导入导出**：各服务 JSON 全量数据集导入/导出，考生 CSV 批量导入。

## 2. 用户角色
| 角色 | 来源 | 权限与操作范围 |
|------|------|----------------|
| **ADMIN** | `User.role=ADMIN` | 全部管理功能：学校/考生/名额/控制线 CRUD、志愿模拟、录取模拟、全局录取统计与各校录取可视化。登录 → `/admin`。 |
| **STUDENT** | `User.role=STUDENT`，关联 `studentId` | 仅自身：查看/编辑本人志愿、提交锁定、查看本人录取结果、使用志愿生成器。登录 → `/student`。 |

**鉴权现状**：auth-service 发放 JWT（BCrypt 密码，有效期 24h）；各后端服务**自校验 JWT**（前端零改动原则）。
**注意**：gateway 仅做路径路由，无服务端角色守卫；角色限制目前由前端 `router.beforeEach` 的 `meta.role` 客户端拦截（见 §6 待裁决项 08-Q5）。

## 3. 主要业务流程
**① 录取模拟（核心）**
`/admission/run/full` → `AdmissionAppService` 在事务外经 `SchoolSnapshotPort`/`StudentSnapshotPort` 拉取 school/student 全量快照 → 派生新 runId → 引擎先跑校额到校（共享组分数排名、按计划池投档）→ 再以校额已录取者为豁免跑统招（全局平行投档）→ 结果反规范化落 `admission_result` → 返回统计。支持 `/run/quota`、`/run/tongzhao` 单批次与历史 `/runs` 对比。

**② 志愿填报 → 提交**
编辑 `POST /student/applications/student/{id}`（先清空再写）→ `submit` 调充血方法 `Student.submit()` 置 `submitted=true` 锁定禁改 → 可 `reopen` 解锁。

**③ 智能志愿生成**
`POST /student/generator/generate` 传考生 id + 偏好（层次/梯队/通勤/距离权重）→ `GeneratorService` 经 `SchoolReferencePort` 拉学校数据 → `StudentAttributes`/`GaokaoTierResolver` 解析（含综合素质 C/D 校额失格拦截）→ 返回校验问题 + 各批次方案。

**④ 考生批量生成**
`POST /student/generate` 按"初中校班数 × 班额(默认 40)"生成考生（默认 `comprehensiveEval="B"`、`crossDistrict=false`）→ `simulateApplications` 理性策略自动填志愿 → `recomputeQuotaEligibility` 结合名额重算资格。

**⑤ 数据集导入导出**
各服务 `/school/export`、`/student/export` 等返回全量 JSON；`/import` 还原；student 另支持 CSV 导入（`name,jsId,语数英物政体,eligibility`）。

## 4. 外部依赖
| 类型 | 实例 | 说明 |
|------|------|------|
| 数据库 | SQLite（每服务独立库） | `school.db`(8102) / `student.db`(8103) / `admission.db`(8104) / `auth.db`(8101)；JPA `ddl-auto: update`，WAL 模式。 |
| 服务间调用 | HTTP（RestTemplate `authedRestTemplate`） | admission → school/student 快照；student → school 参考；均附 admin JWT，已端口化（ACL Port 接口）。 |
| 网关 | Spring Cloud Gateway (8080) | 路由 `/api/*` 到各服务（`StripPrefix=1`），无鉴权过滤。 |
| 缓存/消息队列 | 无 | 无 Redis/Kafka。 |
| 第三方 API | 无 | 通勤/距离用占位估算（`ZoneCommuteEstimator`），非真实外部 API。 |

## 5. 数据模型概览
**school-service**
- `HighSchool`：name, code, district, tongzhaoQuota, quotaAdmitted, tier(KEY/NORMAL), zone, **gaokaoTier(TOP/HEAD/MID)** ← G7 落库
- `JuniorSchool`：name, district, zone, classCount, gradCount
- `QuotaSeat`：juniorSchoolId, highSchoolId, quota（唯一约束组对）
- `ControlLine`：type(QUOTA), value

**student-service**
- `Student`：name, ticketNo, juniorSchoolId, 语/数/英/物/政/体, totalScore, hasQuotaEligibility, submitted, **comprehensiveEval(A/B/C/D)**, **crossDistrict**；充血 `eligibleForQuota()/submit()/unsubmit()`
- `Application`：studentId, batch(QUOTA/TONGZHAO), priority, highSchoolId（唯一约束 student+batch+priority）

**admission-service**
- `AdmissionResult`：studentId, batch, highSchoolId(+name), juniorSchoolId(+name), 各科分, schoolRank, status(ADMITTED/NOT_ADMITTED), runId, runAt；充血 `isAdmitted()`

**auth-service**
- `User`：username, password(BCrypt), role(ADMIN/STUDENT), studentId

## 6. 当前实现状态
**已完成（可运行，单测覆盖）**
- 四业务服务 DDD 四层重构：L1 充血实体 / L2 AppService+事务 / L3 Repository+ACL 端口 / L4 瘦 controller + `@RestControllerAdvice` + `DomainException`。
- 录取引擎双批次算法（含同分比较器 `TieBreakComparator`，首级排序 bug 已修复）、志愿生成器、资格重算、导入导出、JWT 鉴权。
- G7 字段落库：`gaokaoTier`、`comprehensiveEval`、校额 C/D 失格（统招不受影响）。
- 全部 52 单测通过（auth 7 / school 9 / student 24 / admission 12）。

**骨架 / 占位（接口已定义，逻辑为合理默认）**
- `Student.crossDistrict`：占位字段，不生效（非目标"跨区招生"）。
- 志愿生成器：`StudentAttributes`、`GaokaoTierResolver`、`CommuteEstimator`/`ZoneCommuteEstimator`、`GaokaoTier` 枚举——按"接口占位"返回默认值（无真实交通/贯通/跨区数据）。
- `GUANTONG`(贯通培养)批次：常量占位，引擎未实现该批次录取（同 02-Q1 裁决）。
- `gateway`：仅路由，无鉴权/限流/角色守卫。

**明显 TODO / 待裁决（见 08 评审）**
- **08-Q5**：`/generator` 路由后端角色守卫缺失（仅前端拦截）。
- **08-Q7**：学生数 5729 vs 10120 统计口径差异（校额资格/统招范围口径）。
- **08-Q9**：校额分组（`quotaGroups`）配置化未落地（当前入参驱动）。
- `auth-service` 未引入 `jakarta.validation`，DTO 暂为普通 record（无 `@Valid` 校验）。
