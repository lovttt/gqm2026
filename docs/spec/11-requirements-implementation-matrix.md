# 11 · 需求-实现对照表（Requirements–Implementation Matrix）

> 性质：**现状快照（SNAPSHOT）**，非约束契约。用于逐项核对「需求预期（docs/spec/01~07）」与「当前代码实现（实时代码反推）」。
> 生成日期：2026-07-18（含前端 UI 接线补齐同步）。需求预期以 `docs/spec/` 各章为权威源；代码实现以本仓库 `backend/`、`frontend/` 当前工作树为准。
> 状态图例：✅ 一致 / ⚠️ 部分一致 / ❌ 缺失或不一致。带 `*` 的项见 §2 差异说明。

## 1. 对照表

| # | 功能模块 | 需求预期（spec 提取） | 当前代码实现 | 状态 |
|---|---------|----------------------|-------------|------|
| 1 | 用户认证与注册 | `POST /auth/login`（返回 token/role/studentId）、`/auth/register`（默认 STUDENT）、`/auth/users` 分页；JWT+BCrypt、24h | `AuthController` 三端点齐全，各后端服务自校验 JWT | ✅ 一致 |
| 2 | 学校数据管理 | 高中/初中/校额名额/控制线 CRUD（分页）、`/control-line` 单数路径、JSON 导出导入 | `QuotaSeatController`/学校控制器齐全，`DataIoController` 含 `/export` `/import`，控制线为单数路径 | ✅ 一致 |
| 3 | 考生与志愿管理 | Student CRUD+筛选；`/applications/student/{id}` 整页保存（先删后写）；`submit`/`reopen` 提交锁（提交后 400） | `StudentController`/`ApplicationController` 齐全，提交锁逻辑落地 | ✅ 一致 |
| 4 | 校额资格重算 | `POST /quota-eligibility/recompute`：按初中校名额总数 N 取前 N 名（非单纯达线） | 端点存在，且已修"达线即给"bug（`eligibleHere=min(达线人数,N)`） | ✅ 一致 |
| 5 | 智能志愿生成器（单考生） | `POST /generator/generate` 返回 quota/tongzhao/guantong 三方案+校验 issues+权重对比；校额 C/D 失格；guantong 演示占位 | `GeneratorController`+`GenerateResponse` 三批次齐全；但 `gaokaoTier/comprehensiveEval/crossDistrict` 真实数据流为占位（已裁决 Q2/Q3/Q4） | ⚠️ 部分一致 |
| 6 | 志愿模拟器（理性考生全量） | `POST /applications/simulate`：按区排名冲/稳/保、跳过已提交、统招≤8、不盲目全填重点 | `ApplicationSimulator` 纯函数+端点存在，单测覆盖（student 24 测） | ✅ 一致 |
| 7 | 录取模拟引擎（双批次） | 校额→统招；平行志愿分数优先遵循志愿、一次投档、不重复；新 runId+历史快照；不变量（≤计划） | `runFull/runQuotaOnly/runTongzhaoOnly`+`nextRunId`+豁免集+`/runs` 全有 | ✅ 一致* |
| 8 | 同分比较器 | `03 §3.4` **10 级链**：总分→语数外三科→语文→数学→英语→物理+道法→物理→道法→体育→ticketNo | `TieBreakComparator.STEP` 已补齐 **10 级**（含语数外三科/物理+道法/物理/道法/体育），第 10 级 `ticketNo` 兜底；`TieBreakComparatorTest` 专项断言 7 例 | ✅ 一致 |
| 9 | 录取结果查询与可视化 | `/results` 分页+5 维筛选、`/results/student/{id}`、`/stats`、`/runs`、`/runs/{id}`、`/summary-by-school`；反规范化初中+各科；`schoolRank` 仅 QUOTA | 全部端点存在，反规范化字段写入，Specification 筛选，`summaryBySchool` 柱状数据齐全 | ✅ 一致* |
| 10 | 数据导入导出 | JSON 全量 `/export` `/import`（SHOULD）；CSV 批量考生（MAY） | 各服务 JSON 导出导入齐全；`/import/csv` 考生导入存在 | ✅ 一致 |
| 11 | 前端角色视图与路由 | 5 页；未登录→login、角色不符→首页；登录按 role 重定向；`/generator` ADMIN/STUDENT 双开放 | `Admin/Student/Application/Generator/Login.vue` 齐；守卫在前端 `router.beforeEach`；本轮补齐 UI 接线（见 §2 #11） | ✅ 一致 |
| 12 | 网关路由 | `01`：网关 `/api/*`→各服务 `StripPrefix=1` | `GatewayApplication` 仅路由（无鉴权/角色守卫）；鉴权由各后端服务自校验 JWT，角色限制经 Q5 裁决「后端不额外加角色限制」 | ✅ 一致（Q5 裁决） |

## 2. 差异说明

### ✅ #8 同分比较器（已修复，2026-07-18）
`03 §3.4`（MUST）要求 10 级比较链，原 `TieBreakComparator` 仅实现 4 级，缺失 **语数外三科总分、物理+道法、物理、道法、体育** 5 级。已于 2026-07-18 按 spec 补全 `STEP` 列表为完整 10 级（第 10 级 `ticketNo` 兜底保留），`TieBreakComparatorTest` 新增 语数外三科(级2)/物理+道法(级6)/物理(级7) 及全序确定性断言，连同原 3 例共 7 例全绿，`AdmissionEngineTest` 6 例不回归，admission-service 模块 `BUILD SUCCESS`。
- 影响：当两名考生 `总分/语文/数学/英语` 全相等、但在物理/道法/体育上有区分时，现按 spec 的语数外三科→物理→道法→体育细分，与 `03 §3.4` 一致（末级不纳入综合素质评价）。
- 注：spec `03 §3.4` 原本即已正确描述 10 级链，本次是**代码对齐 spec**（非 spec 修订），属 Doc-First 触发项的"代码侧修正"，已在 `00 §0.5` 留痕。

### ✅ #11 前端 UI 接线补齐（本轮，2026-07-18）
此前若干契约端点仅后端存在、前端无入口，本轮补齐为可操作 UI（前端接线，无后端改动）：
- `Login.vue`：登录/注册切换，注册表单（用户名/密码/角色 STUDENT|ADMIN/考生 id）→ `POST /auth/register`。
- `Admin.vue`：新增「用户管理」Tab（`GET /auth/users` 分页）；「数据备份」Tab（`/school/export|import`、`/student/export|import` 接线，导出为浏览器下载 JSON）；「历史运行对比」Tab（`GET /admission/runs` 列表 + `/runs/{id}` 查看结果 + 选 A/B 两轮统计对比）；「考生管理」加单考生新增/编辑/删除、志愿「已提交/未提交」状态列、管理员「撤回」（`POST /student/students/{id}/reopen`）、独立「模拟未提交考生志愿」按钮（`POST /student/applications/simulate`）。
- `Student.vue`：「提交志愿」按钮（`POST /student/students/{id}/submit`）→ 锁定后表单禁用并提示，管理员可在 Admin 端撤回。

结论：#1–#11 契约端点均有前端入口消费，与 04/05 契约一致；无新增后端端点、无 lint 错误、前端 `npm run build` 通过。

### ⚠️ #5 生成器占位项
`gaokaoTierPref`、`comprehensiveEval`、`includeCrossDistrict` 在生成器中仅作入参与展示，真实数据流未全接入（Q2/Q3/Q4 已裁决落库但生成器消费路径仍为默认）。`guantongPlan` 为演示占位、不落志愿库、不录取（与 `02 §2.2`/`07 §7.5` 一致，非 bug）。

### ✅ #12 网关无服务端守卫（设计裁决，非缺陷）
网关仅做路径路由（`/api/*`→各服务 `StripPrefix=1`），不在网关层校验 JWT/角色。鉴权由各后端服务自校验 JWT（见 04/05 各服务契约），角色访问限制经 **Q5 裁决「后端不额外加角色限制、与前端零改动一致」**，由前端 `router.beforeEach` 客户端拦截。这是本期**有意的设计决策**（非待修复 bug）：若未来须防越权直调 API，再评估补服务端角色守卫（属架构增强，非本期契约强制）。本次不改动代码，仅留痕明确其裁决来源。

### ✅* #7 引擎"共享名额池"与 #9 `schoolRank` 口径
`AdmissionEngine.runQuota` 采用 `quotaGroups` **共享名额池（组）聚合**投档，而非 `03 §3.2` 字面描述的"按 `(juniorSchoolId, highSchoolId)` 逐组"。该实现与 Q9（硬编码分组）一致，但 `03 §3.2` 文档未同步此口径；`schoolRank` 因此记录的是"共享组内排名"而非 spec `04 §4.5` 的"同初中校竞争同一高中名次"。功能可运行、单测通过，但**文档与代码存在漂移**，建议补 `03 §3.2` 的共享池说明。

### 其余项（#1–#4、#6、#10、#11）
均与 spec 一致，且 5 服务单测全绿（auth 7 / school 9 / student 24 / admission 12），可按现状核对通过。

## 3. 待办（不阻塞本次落盘）
- **#8（✅ 已修复）**：`TieBreakComparator` 已补全至 10 级链（2026-07-18），单测 7 例全绿；无需 spec 修订（spec `03 §3.4` 本就正确）。
- **#7/#9（✅*）**：`03 §3.2` 已补「共享名额池」描述、`04 §4.4` 的 `schoolRank` 已对齐为共享组内排名（2026-07-18 文档同步，非代码改动）。
- **#12（✅ 裁决接受）**：网关无服务端角色守卫为 Q5 有意裁决（后端不额外加角色限制、前端零改动），非缺陷；代码不改，仅留痕明确裁决来源（2026-07-18）。
