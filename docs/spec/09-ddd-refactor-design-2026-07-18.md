# 09 · DDD 四层重构设计（设计稿，待实现）

> 设计日期：2026-07-18
> 范围：`backend/`（5 服务）按 DDD 分层重构；`frontend/` 零改动（契约兼容）。
> 目的：将现有「贫血模型 + 控制器越权（业务逻辑写在 controller）」重构为 **充血模型 + 显式四层**，并落地 08 §5 中 Q2~Q4/Q6 的裁决（G7 字段落库）。
> **性质**：本文件为【设计稿/评审稿】，未并入正式 spec 亦未改动代码。按 Doc-First 宪法（见 `../CONSTITUTION.md`），实现须在你确认后逐服务落地，并在 `00 §0.5` 留痕。

---

## 0. 裁决基线（本设计的前提）

经 grill 逐条确认，三条总原则 + G7 字段落库如下：

| # | 决策项 | 裁决 | 影响 |
|---|--------|------|------|
| P1 | 架构风格 | **微服务纯粹性优先** | 不建 `gqm-common`；`Score`/`Batch`/`AdmissionStatus`/`TieBreakComparator` 各上下文**各自定义**一份 |
| P2 | 建模风格 | **充血模型** | 业务规则内聚到实体/值对象/领域服务；应用服务只做编排；controller 只做接收 |
| P3 | G7 字段 | **本轮落库** | 见下表逐字段 |

### G7 字段落库裁决（对应 08 §5 Q2/Q3/Q4/Q6）

| 字段 | 归属 | 类型 | 裁决 | 语义 |
|------|------|------|------|------|
| `gaokaoTier`（Q2） | **HighSchool（新增）** | 枚举 `TOP/HEAD/MID` | HighSchool 加字段 | 高中校固有「高考出口梯队」，种子/CSV 灌入，生成器读取匹配 |
| `comprehensiveEval`（Q3） | **Student（新增）** | 枚举 `A/B/C/D` | 纳入**真实门槛** | **低于 B（即 C/D）→ 校额资格失格；统招批次不受影响** |
| `crossDistrict`（Q4） | **Student（新增）** | boolean，默认 `false` | 仅占位不生效 | 落库但生成器不做跨区方案调整，与 02 非目标一致 |
| `guantongSchool`（Q6） | **不落库** | — | 不落库仅生成器输出 | `guantongPlan` 继续作生成器演示产出，不接入录取引擎 |

> 说明：`comprehensiveEval`/`crossDistrict` 随 `ddl-auto: update` 在 student.db 自动建列；`gaokaoTier` 在 school.db 自动建列。SQLite 下若 `update` 未自动加列，需按 `00 §0.5`（2026-07-13）经验用一次性 `create` 重建对应库并重播种。

---

## 1. 第一层 · 领域模型（充血）

### 1.1 设计原则
- 业务规则收进**实体 / 值对象（VO）/ 领域服务**；应用服务不写规则，只编排。
- VO 不可变（构造即校验）；实体行为方法表达业务动词（`submit()`/`admit()`），避免裸 `setXxx`。
- 微服务纯粹性：跨上下文共享概念（`Score`/`Batch`/`AdmissionStatus`）**各自定义**，不抽公共库。

### 1.2 各上下文领域模型

```
[auth]  User(AR): id,username,password(BCrypt),role,studentId

[school]  ← 招生资源，admission 经 ACL 拉快照
  HighSchool(AR): id,name,code,district,tongzhaoQuota,quotaAdmitted,
                  tier(KEY/NORMAL), zone,
                  gaokaoTier(TOP/HEAD/MID)【新, G7-Q2】
                  行为: isKeySchool(), seatsRemaining()
  JuniorSchool(AR): id,name,district,zone,classCount,gradCount
  QuotaSeat(AR): (juniorSchoolId,highSchoolId,quota), remaining()
  ControlLine(AR): type(QUOTA), value, 行为: isAbove(Score)
  ScoreLine(AR) / ScoreSegment(AR)
  《领域服务》QuotaGroupService — 共享名额分组合并（单一事实来源，替代 controller 私有方法）

[student]
  Student(AR) ──< Application(Entity)
    Student: id,name,ticketNo,juniorSchoolId,
             Score【VO,G1】, hasQuotaEligibility, submitted,
             comprehensiveEval(A/B/C/D)【新, G7-Q3 真实门槛】,
             crossDistrict(false)【新, G7-Q4 占位】
             行为: eligibleForQuota(), submit(), unsubmit()
    Application: batch(Batch【VO,G3】), priority, highSchoolId, 行为: inBatch()
  《VO》Score{chinese..pe, total(), compareTo()【G2】}
  《VO》Batch{QUOTA,TONGZHAO,GUANTONG, isAdmissible()}
  《VO》GaokaoTier(TOP/HEAD/MID)
  《领域服务》QuotaEligibilityPolicy【G5】— 前N + 控制线 + 综合素质门槛
  《领域服务》GeneratorService / ApplicationSimulator

[admission]
  AdmissionResult(AR): studentId,name,ticketNo, batch(Batch),
        status(AdmissionStatus【G4】), Score, schoolRank,
        highSchool*/juniorSchool*(反规范化), runId, runAt
        行为: admit(rank), reject()
  《VO》AdmissionStatus{ADMITTED,NOT_ADMITTED}
  《VO》TieBreakComparator【G2, admission 侧真身，与 student 侧各一份】
  《领域服务》AdmissionEngine
  《ACL》StudentSnapshot / SchoolSnapshot
  SimulationRun: 【维持现状】不建实体，runId 由 max(runId)+1 派生、runAt 反规范化挂 AdmissionResult
```

### 1.3 关键充血行为（规则内聚点）

| 归属 | 行为 | 规则 |
|------|------|------|
| `Student` | `eligibleForQuota()` | `hasQuotaEligibility && comprehensiveEval ∈ {A,B}`（**C/D 失格**，落实 G7-Q3） |
| `Student` | `submit()` / `unsubmit()` | 提交锁：`submitted=true` 后拒绝志愿增删改 |
| `Score`（VO） | `total()` / `compareTo()` | 总分 + 10 级同分比较链（落实 G2，见 03 §3.4） |
| `Batch`（VO） | `isAdmissible()` | `GUANTONG → false`（落实 Q1 演示占位不录取） |
| `ControlLine` | `isAbove(Score)` | 控制线达线判定 |
| `HighSchool` | `seatsRemaining()` | 剩余名额派生 |
| `QuotaEligibilityPolicy` | recompute | 结合初中名额取前 N + 控制线 + `comprehensiveEval` 门槛 → 产出 `hasQuotaEligibility` |

### 1.4 本轮新增/变更清单
- **新增 VO**：`Score`、`Batch`、`AdmissionStatus`（各上下文各一份，不共享）
- **新增实体字段**：`HighSchool.gaokaoTier`、`Student.comprehensiveEval`、`Student.crossDistrict`
- **升级为充血**：`Student` / `Score` / `Batch` 内聚规则；`QuotaEligibilityPolicy` 吸收综合素质门槛
- **不落库**：`guantongSchool`（仅生成器输出）

---

## 2. 第二层 · 应用服务

### 2.1 设计原则
- 每个**聚合根**对应一个应用服务；controller 仅接收参数/鉴权，业务逻辑全部下沉。
- 跨服务调用一律经 **ACL 端口**（Layer 3 用 RestTemplate 实现），应用服务只依赖接口。
- 事务边界 = 一个应用服务公开方法 = 一个用例 = 一个事务；**外部 HTTP 调用放在事务外**（防 SQLite 持锁）。

### 2.2 auth 上下文 · `AuthAppService`（聚合 User）
| 用例 | 入/出参 | 依赖(L3) | 事务 |
|------|---------|----------|------|
| login | (username,password) → {token,role,studentId} | UserRepository, JwtUtil, PasswordEncoder | 只读 |
| register | (username,password,role,studentId) → User | UserRepository, PasswordEncoder | `@Transactional` |
| listUsers | Pageable → Page<User> | UserRepository | 只读 |

### 2.3 school 上下文
- **`SchoolAdminAppService`**（HighSchool / JuniorSchool / ControlLine）：CRUD 与 upsert；`upsertControlLine` 用 `ControlLine.isAbove(Score)`。写 `@Transactional`，读只读。
- **`QuotaSeatAppService`**（QuotaSeat）：名额 CRUD + 分组查询；**分组合并下沉为领域服务 `QuotaGroupService.mergeForHighSchool/mergeForJunior`**（替代 controller 私有方法，解 08 M3）。写 `@Transactional`，分组查询只读（内存合并）。
- **`SchoolDatasetAppService`**：`exportDataset()`→SchoolDataset；`importDataset(ds)` 先删后写（含 quotaGroups）。import `@Transactional`。
- **`SeedDataAppService`**：`seedIfEmpty()` + `backfillJuniorSchoolStats()`（幂等回填班数/毕业生数）。`@Transactional`。

### 2.4 student 上下文
- **`StudentAppService`**（Student）：考生 CRUD + `generateStudents(perClass)`（按班数生成，先清后生成，分块 flush 沿用防写锁策略）。写 `@Transactional`。
- **`ApplicationAppService`**（Application）：
  - `listApplications(filters)`→Page<ApplicationView>：只读（组合考生名/校名）
  - `create/update/deleteApplication`：`@Transactional`，**提交锁委托 `Student.isSubmitted()`**（替代 controller `setSubmitted`）
  - `submit(studentId)` / `reopen(studentId)`：调 `Student.submit()/unsubmit()`，`@Transactional`
  - `saveStudentApplications(studentId, list)`：先删后写，`@Transactional`（校验提交锁）
  - `simulateApplications()`：委托 `ApplicationSimulator.regenerateAll()`，`@Transactional`
  - `recomputeQuotaEligibility()`：委托 `QuotaEligibilityService.recompute()`，`@Transactional`（**已吸收 comprehensiveEval C/D 失格**）
  - 依赖：ApplicationRepository, StudentRepository, ApplicationSimulator, QuotaEligibilityService, `SchoolReferencePort`(ACL)
- **`GeneratorAppService`**：`generate(GenerateRequest)`→GenerateResponse（三批次方案+校验+对比）。**不落库**，纯计算。依赖 GeneratorService(领域), `SchoolReferencePort`(取高中 gaokaoTier)。无事务。
- **`StudentDatasetAppService`**：export / import / importCsv。import/importCsv `@Transactional`。

### 2.5 admission 上下文 · `AdmissionAppService`（聚合 AdmissionResult）
| 用例 | 事务 | 说明 |
|------|------|------|
| runFull / runQuotaOnly / runTongzhaoOnly | `@Transactional`（仅包裹计算+落库） | **先经 ACL 端口拉 Snapshot（TX 外）**，再调 `AdmissionEngine` 计算并持久化（结果带 runId/runAt）；豁免集取「最新 runId 的校额已录」考生 |
| results / resultsByStudent / runs / resultsByRun / currentStats | 只读 | 查询仓储；runs/stats 对 result 分组聚合（维持现状） |
| summaryBySchool | 只读 | 读 + 经 `SchoolSnapshotPort` 取计划数 |

依赖：AdmissionResultRepository, AdmissionEngine(领域), `SchoolSnapshotPort`, `StudentSnapshotPort`。

---

## 3. 第三层 · 基础设施（维持现状为主）

### 3.1 Repository 增补（最小，配合充血/G7）
| 上下文 | Repository | 增补 |
|--------|-----------|------|
| school | HighSchoolRepository | `findByGaokaoTier(GaokaoTier)`（按出口梯队筛，配合 G7-Q2）|
| school | JuniorSchoolRepository | `findByNameIn(List<String>)`（分组合并批量取，替代循环单查）|
| school | ScoreLineRepository | `findByYearAndBatch(int,String)`（换算区排名用，可选）|
| student | StudentRepository | `findBySubmittedFalse()`（模拟器只重算未提交）|
| admission | AdmissionResultRepository | 维持（runId/Spec 齐全）|

> 新字段随 `ddl-auto: update` 建列，无需迁移脚本（SQLite 例外见 §0 说明）。

### 3.2 外部调用端口化（落实 L2 决策）
现状 RestTemplate 直连散落在 `AdmissionEngine` 与 student `SchoolDataFetcher`。抽象为端口接口 + REST 实现，放各上下文 `infrastructure/acl` 包：

| 端口接口（app/domain 依赖） | 实现（infrastructure） | provider | 用途 |
|--------------------|----------------------|----------|------|
| `SchoolSnapshotPort`（admission） | `SchoolSnapshotRestClient` | `GET {school}/school/export` | 录取拉高中/名额/控制线快照 |
| `StudentSnapshotPort`（admission） | `StudentSnapshotRestClient` | `GET {student}/student/export` | 录取拉考生/志愿快照 |
| `SchoolReferencePort`（student） | `SchoolReferenceRestClient`（现 `SchoolDataFetcher` 重命名） | `GET {school}/school/export` | 模拟器/生成器/资格重算取参考 |
| `AuthTokenPort`（student/admission） | `AuthTokenRestClient`（现 `AuthTokenProvider`） | `POST {auth}/auth/login` | 服务间调用换 admin JWT |

要点：应用/领域服务只依赖**接口**（便于单测 mock）；RestTemplate 仅在实现类出现；保留 `authedRestTemplate` 拦截器附带 admin JWT。**已知债**：`AuthTokenProvider` token 只取一次、过期不刷新，建议实现里对 401 触发一次 `refresh()` 重试（演示 JWT 长有效期暂缓）。

### 3.3 配置管理（维持现状）
- 保持各服务 `application.yml` 的 `app.*` 命名空间 + `@Value` 逐个注入（**不引入 `@ConfigurationProperties`**）。
- SQLite 连接参数（`busy_timeout=60000&journal_mode=WAL`）保持，配合分块写防写锁。
- `QuotaGroupConfig`（Q9）保持硬编码常量，作 school 上下文领域配置单一事实来源。

### 3.4 SimulationRun（G6，维持现状）
- **不建 `SimulationRun` 实体**；`runId` 由 `max(runId)+1` 派生、`runAt` 反规范化挂每条 `AdmissionResult`；`runs()`/`currentStats` 靠对 result 分组聚合。改动最小。

---

## 4. 第四层 · 接口层（维持现状，零前端改动）

### 4.1 端点前缀与网关（不变）
- 网关 8080，`/api/*` 经 `StripPrefix=1` → 各服务根路径（`/auth` `/school` `/student` `/admission`），路由到 8101~8104。controller 挂根路径不变，前端无需变动。

### 4.2 REST 端点映射（controller 瘦化）
controller 只做：解析路径/Query/Body → 调应用服务 → 返回结果。**不含业务判断、循环、事务注解**。端点集合维持 08 §4.3/4.4 已对齐的现状（含 `/generator/generate`、`/applications/student/{studentId}`、`/control-line` 等）。

### 4.3 入参校验
- **写请求用 Request DTO + `@Valid`**（`CreateStudentRequest`/`GenerateRequest`/`SaveApplicationsRequest` 等），controller 入参拦截基础校验（`@NotBlank`/`@Min`/`@NotNull`）。
- **领域/业务校验**（提交锁、名额超额、综合素质失格）由领域层/应用服务抛异常，不在 controller 判断。

### 4.4 统一异常处理（新增 `@RestControllerAdvice`）
- 全局捕获 `DomainException`（提交锁冲突/校额失格/名额不足）、`MethodArgumentNotValidException`、`AccessDeniedException`、`RuntimeException`。
- **错误响应体固定为 `{ "message": "..." }`** —— 严格匹配前端 `request.js` 的 `err.response?.data?.message` 契约（见 `frontend/src/utils/request.js:27`），**前端零改动**。
- HTTP 状态：业务冲突 409、校验失败 400、未授权 401、其他 500。

### 4.5 响应策略（维持直通）
- **成功响应数据直通**（`resp.data` 直接用），**不引入 `ApiResult` 包装**（前端 `request.js:19` 已 `resp => resp.data`）。
- **读响应**保持返回实体/View（`ApplicationView`/`AdmissionResult`/`Page<T>`）。
- **写请求**引入轻量 Request DTO + `@Valid`（仅写接口）。

### 4.6 鉴权位置（维持现状）
- 网关不做鉴权（`StripPrefix` 透传）；四业务服务沿用 `SecurityConfig + JwtFilter` 自校验 Bearer；服务间调用走 ACL 实现内换 admin JWT。**不引入网关统一鉴权**。

---

## 5. 前端契约兼容性核对（零改动结论）

| 前端约束 | 来源 | 本设计是否兼容 |
|----------|------|----------------|
| 成功响应 `resp => resp.data` 直通 | `request.js:19` | ✅ 不包装，直通 |
| 错误体读 `err.response?.data?.message` | `request.js:27` | ✅ `{message}` 固定错误体 |
| 401 清 localStorage 跳登录 | `request.js:22-25` | ✅ 未授权仍返回 401 |
| 列表消费 `Page<T>.content` | Admin/Student.vue | ✅ 读响应保持 `Page<T>` |
| baseURL `/api` + 网关路由 | `vite.config.js` / gateway yml | ✅ 端点前缀不变 |

**结论**：四层重构对前端**零改动**。

---

## 6. 实现落地计划（确认后执行，逐服务）

> 按依赖从底向上、风险从小到大，建议顺序：school → student → admission → auth，最后统一异常处理器。

1. **school-service**：加 `HighSchool.gaokaoTier`；`QuotaGroupService` 领域服务化；抽 `SchoolAdminAppService`/`QuotaSeatAppService`/`SchoolDatasetAppService`/`SeedDataAppService`；controller 瘦化。
2. **student-service**：加 `Student.comprehensiveEval`/`crossDistrict`；`Student`/`Score`/`Batch` 充血；`QuotaEligibilityPolicy` 吸收 C/D 失格；`SchoolDataFetcher`→`SchoolReferencePort`；抽 5 个应用服务；controller 瘦化。
3. **admission-service**：`AdmissionResult`/`Batch`/`AdmissionStatus` 充血；RestTemplate 直连→`SchoolSnapshotPort`/`StudentSnapshotPort`；`AdmissionAppService` 包裹 run（HTTP 拉取移出 TX）；controller 瘦化。
4. **auth-service**：`AuthAppService` 抽取；controller 瘦化。
5. **全服务**：新增 `@RestControllerAdvice` + `DomainException` 体系，错误体 `{message}`；写接口补 Request DTO + `@Valid`。
6. **验证**：`mvn -o test`（JDK 17）5 服务全绿；前端 `npm run build` 通过；`run/full` 冒烟（校额资格含 C/D 失格生效）。
7. **落盘**：实现完成后按 Doc-First 将受影响契约同步进 02（新字段）/04（错误体+DTO）/01（分层）等，并在 `00 §0.5` 留痕。

---

## 7. 与 08 待确认清单的关系

本设计**落地了 08 §5 的以下裁决**（grill 结论）：
- **Q2**（gaokaoTier 来源）→ HighSchool 加 `gaokaoTier(TOP/HEAD/MID)` 字段（见 §0 / §1.2）
- **Q3**（comprehensiveEval）→ Student 加字段并纳入**真实门槛（C/D 校额失格）**（见 §1.3 / §2.4）
- **Q4**（crossDistrict）→ Student 加字段但**仅占位不生效**（见 §0 / §1.2）
- **Q6**（guantong）→ **不落库**，`guantongPlan` 仅生成器演示输出（见 §0）

08 §5 其余 **Q5 / Q7 / Q8 / Q9** 未被本设计覆盖（Q8 错误体在本设计 §4.4 已给方案，可与实现同批落地；Q5 路由守卫、Q7 学生数口径、Q9 名额分组配置化仍待你单独裁决）。
