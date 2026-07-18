# 08 · 文档—代码契约对齐审查（评审稿·Q1-Q9 已裁决收口）

> 审查日期：2026-07-18
> 范围：`docs/`（CONSTITUTION + 00~07 spec + README）+ `backend/`（5 服务 main 实现）+ `frontend/`
> 目的：按 `grill-with-docs` 目标，对照需求文档与已实现代码，列出不一致、产出修正草稿、汇总待人工确认的模糊点。
> **性质**：本文件为【草稿/评审稿】，未并入正式 spec。按 Doc-First 宪法，正式 spec 的修订须在你确认后再落盘，并在 `00 §0.5` 留痕。

---

## 1. 执行摘要

- 文档整体与代码**高度一致**（Doc-First 执行良好），但存在 **11 处描述错误/过时** 与 **9 处代码已实现但文档遗漏**（详见 §2，合计 20 处）。
- 最严重的一致性问题：**「贯通批次」在 README 被宣传为核心三批次特性，但 00/02 明确将其列为非目标，且录取引擎（03）只实现校额+统招两批次**；生成器已产出 `guantongPlan` 但录取不消费它（见 §4 / §5 Q1）。
- 次严重：控制线路径、整页保存路径、学生数（5729 vs 10120）、若干实体字段（submitted / schoolRank / 反规范化列 / classCount / gradCount）、前端 2 个页面（Application.vue / Generator.vue）均在代码中存在但文档缺失。
- ✅ **2026-07-18 已落盘**：Q1（演示占位）已裁决并合并；E1-E4 错误、O1-O6 过时、M1-M8 遗漏中的多数已并入正式 spec（01/02/04/05/06/07/00/README），并在 `00 §0.5` 留痕。
- ✅ **2026-07-18 收口**：Q2/Q3/Q4/Q6 已随 `09-ddd-refactor-design-2026-07-18.md` 的 DDD 四层重构**落地实现**（G7 字段落库 + 统一异常）；Q5/Q7/Q8/Q9 已裁决（详见 §5，裁决原则与「前端零改动」「微服务纯粹性」一致）。DDD 重构已逐服务落地，`mvn -o test` 五服务全绿（auth 7 / school 9 / student 24 / admission 12）。

---

## 2. 不一致清单（错误 / 过时 / 遗漏）

> 标记：🔴 错误描述（与代码冲突）｜🟡 过时（数字/状态未同步）｜🟢 遗漏（代码有、文档无）

### 2.1 🔴 描述错误（与代码直接冲突）

| # | 位置 | 文档现状 | 代码事实 | 代码位置 |
|---|------|----------|----------|----------|
| E1 | `01-architecture.md §1.1` | 「系统由 **6 个进程**组成」 | 实际 **5 个**（gateway + 4 业务服务） | `backend/{gateway,auth-service,school-service,student-service,admission-service}` |
| E2 | `04-api-contract.md §4.3` | 控制线 CRUD 路径 `/control-lines`（复数） | 实际 `/school/control-line`（单数，GET+POST） | `school-service/.../controller/SchoolController.java:71-87` |
| E3 | `04-api-contract.md §4.4` | 整页保存 `POST /applications/batch`；读取 `GET /students/{id}/applications` | 实际整页保存为 `POST /student/applications/student/{studentId}`；单考生读取走 `GET /applications?studentId=`（分页），无 `{id}/applications` 端点 | `student-service/.../controller/ApplicationController.java:40-80, 142-153` |
| E4 | `04-api-contract.md §4.1` | 「业务异常返回 500 + 文本消息」 | 提交锁返回 **400**（BAD_REQUEST），不存在考生返回 **404**；尚缺统一错误体 | `ApplicationController.java:99,116` |
| E5 | `README.md` 核心特性 | 志愿生成器「按 **校额到校 / 统招 / 贯通** 三批次」 | 录取引擎（03）只实现 QUOTA+TONGZHAO；生成器产 `guantongPlan` 但录取不消费 | `GeneratorController.java`；`GenerateResponse.java:34,37` |

### 2.2 🟡 过时（数字 / 状态未同步）

| # | 位置 | 文档现状 | 代码事实 |
|---|------|----------|----------|
| O1 | `02-data-model.md §2.4` | 「生成 **约 5729 名**考生（按一分一段表≥430 比例）」 | 实际按初中校**班数×班额(40)** 生成 **10120 名**（见 `00 §0.5` line 68） | `student-service/.../service/StudentGenerator`（或 `SeedDataService`）|
| O2 | `02-data-model.md §2.1` Student 表 | 未列 `submitted` 字段（仅 §2.5-3 / 04 §4.4 引用） | 实体含 `submitted`（提交锁） | `Student.java:41` |
| O3 | `02-data-model.md §2.1` AdmissionResult 表 | 仅列基础字段 | 缺 `schoolRank` / `juniorSchoolId`+`juniorSchoolName` / 六科 `chinese..pe` / `runId`+`runAt`（04 §4.5 已描述添加，02 未同步） | `AdmissionResult.java:32-60` |
| O4 | `02-data-model.md §2.1` JuniorSchool 表 | 未列 `classCount` / `gradCount` | 阶段1 已加两列（班数/毕业生数） | `JuniorSchool.java`（阶段1 加）|
| O5 | `06-quality-harness.md §6.6` | 绿灯仅要求 admission/student **两服务**测试 0 失败 | 阶段1 后 **5 服务**均有测试（auth 7 / school 9 / student 24） | 见 `06 §6.1` |
| O6 | `06-quality-harness.md §6.1` | 阶段1 新增测试标 🔶（计划） | 这些测试**已实现**（QuotaSeatControllerTest 等） | `school/auth/student .../src/test` |

### 2.3 🟢 遗漏（代码已实现，文档未记录）

| # | 主题 | 代码位置 | 说明 |
|---|------|----------|------|
| M1 | **单考生志愿生成器端点** `POST /student/generator/generate` | `student-service/.../controller/GeneratorController.java:22` + `generator/dto/GenerateRequest.java` + `GenerateResponse.java` | 完全未在 04/07 记录（仅 README 提）。参数：通勤上限、高考出口梯队偏好 TOP/HEAD/MID、跨区占位、梯度权重(sprint/steady/safety，和=100)、偏好权重(commute/gaokaoOutput 0-100)、综合素质评价、权重联动对比(prev*)；返回 三批次方案(quota/tongzhao/guantong)+校验 issues+过滤信息+对比 |
| M2 | **ScoreLine 实体**（school 库，2025 统招线） | `school-service/.../entity/ScoreLine.java` + `repository/ScoreLineRepository.java` | 07 §7.3.2 引用，但 02 §2.1 未定义 |
| M3 | **校额名额分组** `QuotaGroupConfig.JUNIOR_GROUPS`（5 组共享名额池） | `school-service/.../config/QuotaGroupConfig.java` + `controller/QuotaSeatController.java:46-147` | 分组合并逻辑在 Controller 私有方法；04/02 均未记录 |
| M4 | **前端两个页面缺失** | `frontend/src/router/index.js:7-9`；`views/Application.vue`、`views/Generator.vue` | 05 §5.2 仅列 Login/Admin/Student；实际还有 `/applications`→Application.vue（ADMIN 志愿管理）与 `/generator`→Generator.vue（生成器，**路由无 role meta**） |
| M5 | **高考出口梯队** `gaokaoTierPref`(TOP/HEAD/MID) | `GenerateRequest.java:27` | 生成器使用，但 02 数据模型未定义 HighSchool「高考出口梯队」来源（仅 `tier` KEY/NORMAL） |
| M6 | **综合素质评价** `comprehensiveEval`（占位默认"B"，校额门槛拦截演示） | `GenerateRequest.java:52` | 03 §3.4 明确「末级是否纳入综合素质评价本期不纳入」；属占位/演示，未在数据流落地 |
| M7 | **按班数生成考生** `POST /student/generate?perClass=40` | `StudentController.java:49-56` | 04 §4.4 未列 |
| M8 | **导入导出已落地** | `school-service/.../controller/DataIoController.java`（`/school/export`、`/school/import`）；`student-service` 同 | 04 §4.3/4.4 标 MAY/SHOULD，应改「已实现」；导出含 `quotaGroups` |

---

## 3. 已确认一致（无需改动，供安心）

- 网关路由 `/api/{svc}` + `StripPrefix=1`、端口（8080/8101~8104）与 01/04 一致（`gateway/.../resources/application.yml`）。
- 录取引擎两批次顺序、平行志愿、不变量、同分比较器 10 级、历史快照 runId 隔离 —— 与 03 一致（`AdmissionEngine`）。
- 校额资格 `min(达线人数, N)`、控制线 430、提交锁、分页 `Page<T>` 信封 —— 与 02/04/06 一致。
- 四业务服务均含 `SecurityConfig`+`JwtFilter`（auth/school/student/admission），gateway 仅路由 —— 与 01 §1.3 一致。

---

## 4. 更新后的需求文档草稿（修正段落，带代码位置标注）

> 以下为可直接合并回正式 spec 的修正内容。保留原文档结构与风格。

### 4.1 修正 `01-architecture.md §1.1`

```
## 1.1 服务拆分（MUST）
系统由 5 个进程组成，职责严格分离：

| 服务 | 端口 | 职责 | 独立 SQLite |
|------|------|------|-------------|
| gateway | 8080 | 统一入口，按 `/api/{svc}/**` 路由并 StripPrefix=1 | 否 |
| auth-service | 8101 | 用户/角色、JWT 签发与校验 | 是（auth.db） |
| school-service | 8102 | 高中/初中校/校额名额/控制线/一分一段表/统招线 CRUD + 导入导出 | 是（school.db） |
| student-service | 8103 | 考生/成绩/志愿/志愿生成器/模拟器 CRUD + 导入导出 | 是（student.db） |
| admission-service | 8104 | 录取引擎：聚合数据→校额到校→统招→结果 | 是（admission.db） |
```
（端口与职责参见各服务 `src/main/resources/application.yml`；路由见 `gateway/src/main/resources/application.yml`）

### 4.2 修正 `02-data-model.md`

**§2.1 Student** —— 补充 `submitted`：
```
| submitted | boolean | MUST，志愿提交锁（提交后不可增删改志愿）；代码位置：`student-service/.../entity/Student.java:41` |
```

**§2.1 JuniorSchool** —— 补充阶段1 字段：
```
| classCount | int | SHOULD，班数（阶段1 加，按 junior_school.csv 回填，`SeedDataService.backfillJuniorSchoolStats`）|
| gradCount  | int | SHOULD，毕业生数（同上）|
```

**§2.1 新增 ScoreLine 实体**（school 库）：
```
### ScoreLine（统招线）`score_line`（school 库）
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long (PK) | MUST |
| highSchoolId | Long | MUST，FK→high_school |
| year | int | MUST，年份（2025 历史统招线，用于志愿模拟换算区排名）|
| line | int | MUST，该校该年统招录取线（510 量纲）|
> 代码位置：`school-service/.../entity/ScoreLine.java`；被 `07 §7.3.2` 引用换算 2025 区排名。
```

**§2.1 AdmissionResult** —— 补全反规范化字段与运行标识：
```
| schoolRank | Integer | 校内排名：仅 QUOTA 且 ADMITTED 有值（同初中校竞争同一高中校额名次）；其余 null |
| juniorSchoolId / juniorSchoolName | Long / String | 来源初中校（反规范化，便于结果页直显）|
| chinese/math/english/physics/politics/pe | int | 各科得分（反规范化自 student 快照）|
| runId | Long | 模拟运行批次号（历史快照，见 03 §3.8）|
| runAt | LocalDateTime | 本次模拟运行时间 |
> 代码位置：`admission-service/.../entity/AdmissionResult.java:32-60`；由 `runQuota/runTongzhao` 保存时写入。
```

**§2.4 种子考生数** —— 修正：
```
- 种子生成 **MUST** 体现 2.3 区分度分布（pe=50；politics≈80；physics=70+；english=60+；语数/物理笔试/英语笔试/道法笔试按随机分布拉开区分度）。
- 考生由 `student-service` **按初中校班数 × 班额(默认 40)** 生成（约 **10120 名**，见 `00 §0.5` 2026-07-13 修订；`StudentGenerator` 读 `junior_school.csv` 班数）。早期「5729（按一分一段表≥430 比例）」口径已弃用。
- 志愿不写死，由模拟器 `POST /applications/simulate` 或单考生生成器 `POST /student/generator/generate` 产生（见 04 §4.4、07）。
```

### 4.3 修正 `04-api-contract.md`

**§4.1 错误响应**（部分结构化已落地）：
```
- 错误：提交锁/不存在资源返回结构化 HTTP 状态（提交锁 **400**、不存在 **404**，经 `ResponseStatusException`）；其余业务异常仍返回 500 + 文本。统一错误体 `{code,message}` 仍待规范（见 4.7-2）。
```

**§4.3 School 服务** —— 控制线路径改为单数：
```
| GET/POST | `/control-line` | 控制线读取/upsert（注意：单数路径，非 /control-lines）|
| GET | `/export` | 整体导出 SchoolDataset：含 highSchools/juniorSchools/quotaSeats/controlLine/scoreLines/scoreSegments/quotaGroups（quotaGroups 为共享分组，见 M3）|
```

**§4.4 Student 服务** —— 修正保存路径、补生成器与生成端点：
```
| POST | `/generator/generate` | 单考生志愿生成器（参数见 GenerateRequest；返回三批次方案+校验+过滤+权重对比，见 07 §7.6）|
| POST | `/applications/student/{studentId}` | 整页保存某考生志愿（先删后写，提交锁生效）|
| GET  | `/applications` | 志愿查询（支持 studentId/batch/studentName/juniorSchoolId 过滤，分页）|
| POST | `/generate?perClass=40` | 按初中校班数生成考生（先清空再生成）|
```
（删除原 `/applications/batch` 与 `/students/{id}/applications` 两行；代码位置：`student-service/.../controller/ApplicationController.java`、`GeneratorController.java`、`StudentController.java`）

### 4.4 修正 `05-frontend-contract.md §5.2`

```
| `/applications` | Application.vue | ADMIN | 志愿管理（按考生查看/编辑志愿，提交锁校验）|
| `/generator`    | Generator.vue   | ADMIN/STUDENT | 单考生志愿生成器：参数交互→三批次方案+校验+权重联动对比 |
```
> 路由定义：`frontend/src/router/index.js:5-10`。**注意**：`/generator` 路由当前**无 `meta.role`**（任何登录用户可进），与 05 §5.2「角色不符跳首页」的 MUST 约定存在落差，建议补 `meta:{role:'STUDENT'}` 或明确其为双角色页。

### 4.5 修正 `06-quality-harness.md`

**§6.1** —— 阶段1 新增测试 🔶 全部改为 ✅（已实现）。
**§6.6 绿灯定义** —— 第 1 条改为：
```
1. `mvn -o test`（JDK 17）在 **全部 5 个服务**（admission/auth/school/student；gateway 无业务逻辑）均 0 失败、0 错误。
```

---

## 5. 待确认清单（需你人工裁决的模糊点）

> 这些点文档与代码存在真实张力或代码自身「半实现」，需你拍板后我再据 Doc-First 落盘。

**Q1（✅ 已裁决：演示占位）· 贯通批次的定位**
- 开发者裁决：**贯通批次为演示占位，本期不录取**。已据此落盘：README 下调贯通描述、02 §2.2 / 07 §7.5 标注 `guantongPlan` 为未接入录取的演示输出；00/02 §0.1「贯通培养=非目标」保持不变。

**Q2（✅ 已 resolved · 已落地 09 §2）· 生成器的「高考出口梯队」(TOP/HEAD/MID) 数据从哪来？**
- 裁决：梯队定义为 `HighSchool.gaokaoTier`（枚举 TOP/HEAD/MID），**已落库**。种子由 `tier` 派生（KEY→TOP、NORMAL→MID），可在高中管理页编辑；生成器经 `GaokaoTierResolver` 优先读 DB 值、回退 tier 映射。见 `02 §2.2` / `09 §2.5`。

**Q3（✅ 已 resolved · 已落地 09 §2）· 综合素质评价 `comprehensiveEval` 是否本期落地？**
- 裁决：本期落地为真实门槛。**`Student.comprehensiveEval`(A/B/C/D) 已落库**，默认 "B"。纳入校额到校门槛——C/D 校额失格（由 `Student.eligibleForQuota()` 判定，落实于 `QuotaEligibilityService` 重算与 `GeneratorService` 生成期校验）；**统招不受影响**（录取引擎未改）。见 `02 §2.4` / `09 §2.5`。

**Q4（✅ 已 resolved · 已落地 09 §2）· 跨区投放 `includeCrossDistrict` 是否要启用？**
- 裁决：保持占位、不启用。`Student.crossDistrict`（boolean）已落库但**占位不生效**，与 `00 §0.1` 非目标「跨区招生」一致；生成器 `GaokaoTierResolver`/`StudentAttributes` 的跨区相关项维持默认 false。见 `02 §2.4` / `09 §2.5`。

**Q5（✅ 已裁决 · 维持现状/双开放）· `/generator` 路由是否需加角色守卫？**
- 裁决：**对 ADMIN/STUDENT 双开放，后端不额外加角色限制**。理由与「前端零改动」原则一致——前端 `Generator.vue` 既可由 Student 页（`Student.vue` 的「志愿生成器」按钮）也可由 Admin 触发，当前 `05 §5.2` 已声明路由 `meta.role` 客户端拦截；后端维持 09 既定「各服务自校验 JWT」即可覆盖鉴权，无需新增角色维度（否则须同步前端，违反零改动）。**动作**：将 `05 §5.2` 中生成器相关描述由「限定 STUDENT」改为「ADMIN/STUDENT 双开放」。见 `09 §4`（L4 自校验鉴权）。

**Q6（✅ 已 resolved · 随 Q1 演示占位）· 控制线是否仅 QUOTA 一种 type？**
- 02 §2.1 写 `ControlLine.type` 仅 `'QUOTA'`。生成器又用「贯通门槛 380」（GenerateResponse.guantongHidden 当 总分<380）。若贯通真做（Q1），控制线 type 是否要扩 `GUANTONG`？请确认。

**✅ 已 resolved（随 Q1 演示占位）**：`ControlLine.type` **维持仅 `QUOTA`**，不扩 `GUANTONG`。贯通为演示占位（Q1 已裁决、不录取），其「门槛 380」仅是生成器展示用的内部提示，不构成录取批次控制线。见 `02 §2.1` / `09 §2`。

**Q7（✅ 已裁决 · 以 10120 班数口径为准）· 学生生成口径 5729 vs 10120 以哪个为准对外？**
- 裁决：**统一以 10120（按班数×班额）为对外口径**。5729 为早期朴素种子（未含班数估算），已过时；`02 §2.4` 已于 2026-07-13 同步为 10120，README 考生数措辞亦于同期改为 10120（仅 `start-all.ps1` 启动提示残留「5729」，本次一并修正）。见 `02 §2.4`。

**Q8（✅ 已 resolved · 已落地 09 §4）· 错误响应是否本期统一为 `{code,message}`？**
- 裁决：本期统一为 **`{message}`**（去掉 `code`），与「前端零改动」契约一致。`09 §4`（L4 接口层）已落地：四业务服务新增 `@RestControllerAdvice` + `DomainException`，错误体固定 `{message}`，成功响应直通不包装。**动作**：将 `04 §4.7-2` 由「`{code,message}` 开放」改为「`{message}` 已定」，并据 09 落地状态同步。见 `09 §4` / `04 §4.1`。

**Q9（✅ 已裁决 · 本期保持硬编码）· 校额名额分组（QuotaGroupConfig 5 组）是否要可配置化？**
- 裁决：**本期保持硬编码**，不配置化。理由与「微服务纯粹性」「前端零改动」一致——分组属稳定教育政策配置、当前规模小、无 DB 配置诉求；且分组经 `SchoolSnapshot.quotaGroups` 由 school-service 快照传入 admission（已端口化），后续如需配置化仅改 school-service 数据来源，不波及录取引擎契约。见 `09 §3.2`（ACL 端口化）/ `03 §3`。

---

## 6. 建议的后续动作（确认后执行）

1. ✅ 已完成：§5 的 Q1~Q9 全部裁决/收口（Q1 演示占位；Q2/Q3/Q4/Q6/Q8 随 09 落地；Q5/Q7/Q9 裁决维持现状/双开放/班数口径/硬编码）。
2. ✅ 已完成：§4 修正段落多数已并入 01/02/04/05/06/07/00/README（2026-07-18 留痕，见 `00 §0.5`）。
3. ✅ 已完成：Q1 贯通本期不做——README 贯通宣传已下调、生成器 `guantongPlan` 标注为未接入录取的演示输出。
4. ✅ 已完成：`mvn -o test`（JDK 17）五服务全绿（auth 7 / school 9 / student 24 / admission 12），06 §6.1 阶段1 🔶→✅。
5. ✅ 已同步正式 spec（Doc-First 留痕）：`05 §5.2` 生成器改为 ADMIN/STUDENT 双开放（Q5 已裁决）、`04 §4.1/§4.7-2` 错误体定为 `{message}`（Q8 已落地）、`start-all.ps1` 启动提示「5729」改为「约 10120」（Q7 残留文案）；README/`02 §2.4` 已于 2026-07-13 先行同步，无需再改。
