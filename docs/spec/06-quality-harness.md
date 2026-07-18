# 06 · 质量约束（QUALITY HARNESS）

> 约束对象：验收标准。本文档定义「实现必须满足的可检验条件」，是 `00 §0.2` Harness 工程原则的落地。
> 核心：文档即可执行规格，以下条目是代码是否达标的判据；每条标注强度 `MUST`/`SHOULD`/`MAY`。

## 6.0 Harness 工程自检（对齐 00 §0.2）
| 00 §0.2 原则 | 本文件落实 | 满足 |
|------|------|------|
| 1 契约优先（可追溯到条款） | 见 6.2 追溯矩阵，每条 harness 检查映射到 spec 章节 | ✅ |
| 2 可审查（MUST/SHOULD/MAY） | 全文标注强度 | ✅ |
| 3 偏离即修订 | 代码改动须先更新本文件并在 00 §0.5 留痕 | ✅ |
| 4 逐节拷问 | 6.8 开放问题均经拷定或显式挂起 | ✅ |
| 5 可验证（含自动化测试） | 6.1 自动化测试 + 6.3 边界负向 + 6.4 手动清单 | ✅ |

## 6.1 后端自动化测试（MUST）
### TieBreakComparatorTest（纯单测）
- 同分时按 STEP 链逐级比较，校验 10 级优先级顺序正确（含第 10 级 `ticketNo` 确定性兜底）。
- 总分高者恒优先。
- 全相同字段时结果由 `ticketNo` 决定，比较器为全序（对应 3.4 / 3.7-3）。

### AdmissionEngineTest（Mock 集成，无 Spring 上下文）
用内存数据集 + Mock `RestTemplate`/`AdmissionResultRepository` 断言不变量：
1. 校额到校仅在 `hasQuotaEligibility && totalScore>=控制线` 时录取。
2. 同 `(junior,high)` 校额录取数 ≤ `QuotaSeat.quota`，由 `ticketNo` 确定性兜底。
3. 统招录取数每校 ≤ `tongzhaoQuota`。
4. 校额已录取者不出现在统招 ADMITTED（6.3 边界用例 `quotaAdmittedExcludedFromTongzhao`）。
5. 分数优先：高总分考生优先占统招计划。
6. ✅ 平行志愿无罚分：低分考生将学校填为 2 志愿仍可在高分者未占该计划时被录取。
7. ✅ 确定性：同一数据集运行两次，录取结果（studentId+batch+status）完全一致。
8. ✅ **边界** 控制线精确判定：`totalScore==430` 录取、`429` 不录取（对应 3.2 / 3.5-4）。
9. ✅ **负向** 滑档 `NOT_ADMITTED`：计划被占满时低分考生 `NOT_ADMITTED`、不落入任何高中、无补录/二次投档（对应 3.3 / 3.7-1）。

### AdmissionControllerTest（控制器契约，Mock Engine）
- `GET /runs` 返回历史运行列表，含 `runId` 等统计字段（对应 04 §4.5 / 03 §3.8）。
- `GET /runs/{runId}` 透传引擎结果。
- `POST /run/full` 返回含 `runId` 的 stats。

### ApplicationControllerTest（控制器契约，Mock Repo）
- `POST /students/{id}/submit` 将 `Student.submitted` 置 true；`/reopen` 置 false。
- ✅ **负向** 已提交后 `POST /applications` 抛出 `400`，且绝不写入（对应 04 §4.4 / 02 §2.5-3）。
- 未提交时 `POST /applications` 正常保存。

### ApplicationSimulatorTest（纯单测，无 Spring 上下文）
校验阶段 2「理性考生」志愿模拟（对应 07 §7.3，基于 2025/2026 一分一段表换算区排名）：
1. 高分 + 有资格 → 校额对口（层次/距离序）+ 统招按 冲→稳→保 拉开档次，普通校自然落入保底。
2. 无资格 → 无校额志愿，仍有统招。
3. 低于控制线(430) → 无任何志愿。
4. 保底档优先普通校（非全重点，`chongWenBao_spreadByRank` 中 h3/h5 普通校被纳入）。
5. 冲档(`REACH`)排在最前（例：465 分考生统招顺序 `[3,2,1,4,5]`）。
6. 统招平行志愿不超过 8 个（`tongzhaoNeverExceedsEight`，含 9 校场景验证上限）。

### QuotaEligibilityServiceTest（纯单测，无 Spring 上下文）
校验「校额资格结合初中校名额总数」（对应 02 §2.6 / 04 §4.4）：
1. 某初中校名额总数 N，达线考生 > N → 仅校内排名前 N 名 `hasQuotaEligibility=true`。
2. 超出前 N 名者被剔除（即使总分≥430）。
3. 达线考生数 < N → 达线者全部具资格（不越界）。
4. N=0（该校无名额）→ 该校无人具资格。
5. 低于控制线者一律无资格（不计入前 N）。

### QuotaSeatControllerTest（school-service，纯 Mock，阶段1 新增（已实现））
> 前提：school-service `pom.xml` 补 `spring-boot-starter-test`（当前缺失，见阶段1）。
> Mock `QuotaSeatRepository` + `JuniorSchoolRepository`，`@InjectMocks QuotaSeatController`，直接调 `listQuotaSeats(...)`（合并为私有方法，经公开入口触发）。对应 `04 §4.6` / `QuotaGroupConfig`：
1. 查组内初中校（如「东直门中学」id=1，与「第一六五中学」id=2 同组）→ 返回按 `highSchoolId` 合并的行，`quota` 求和，`juniorSchoolNames` 含两校名且被查校（东直门）排在组标签最前。
2. 查非组初中校 → 返回该校独立名额行（不合并）。
3. 查某高中且含组初中校 → 组内行合并到 `组标签` 行（`repId` 取组内首个存在 id），其余单列。
4. `juniorSchoolId + highSchoolId` 同时给 → 走精确 `findByJuniorSchoolIdAndHighSchoolId`，不合并。

### ControlLineControllerTest（school-service，纯 Mock，阶段1 新增（已实现））
> Mock `ControlLineRepository`。对应控制线 430（`02`/`03`）：
1. `GET /school/control-line?type=QUOTA` → 返回种子值 `430`（repo 命中）。
2. 缺失类型 → 返回 `value=0` 的默认 `ControlLine`（不抛异常、不空指针）。
3. `POST /school/control-line` upsert：已存在则改值保存、不存在则新建。

### SeedDataServiceBackfillTest（school-service，阶段1 新增（已实现））
> 校验 `backfillJuniorSchoolStats()` 幂等回填初中校 `classCount/gradCount`（按 `junior_school.csv` 名称对齐）。对应 `02 §2.6` / 初中校统计：
1. 首次回填：CSV 含「一中,3,120」→ 该初中校 `classCount=3`、`gradCount=120`。
2. 重复调用幂等：值不变、不重复插入、不抛异常。

### AuthControllerTest（auth-service，纯 Mock，阶段1 新增（已实现））
> 前提：auth-service `pom.xml` 补 `spring-boot-starter-test`（当前缺失，见阶段1）。
> Mock `UserRepository` + `JwtUtil` + `PasswordEncoder`，`@InjectMocks AuthController`。对应 `04` 鉴权：
1. `POST /auth/login` admin/admin123 → 返回含 `token`（JWT 串）、`role=ADMIN` 的 Map。
2. 错误密码 → `login` 抛 `RuntimeException`（无 `@ExceptionHandler`，默认 HTTP 500）。
3. 不存在用户 → 抛 `RuntimeException`。
4. `POST /auth/register` 新用户 → 返回带 `id` 的 `User`；重名 → 抛 `RuntimeException`；role 默认 `STUDENT`。

### JwtUtilTest（auth-service，纯单测 round-trip，阶段1 新增（已实现））
> `new JwtUtil()` 经 `ReflectionTestUtils` 注入 `secret`/`expirationMs`，不依赖 Spring 上下文。对应 `04` 鉴权：
1. `generate(username, role)` → `parse(token).getSubject()==username`、`get("role")==role`。
2. 非法/被篡改 token → `parse` 抛异常。

### ApplicationSimulatorControllerTest（student-service，纯 Mock，阶段1 新增（已实现，解 D6）
> Mock 模拟器引擎/数据获取，`@InjectMocks` 模拟器控制器。对应 `07 §7.3.3` / `07 §7.4`：
1. `POST /applications/simulate` → 返回 `{generated:N}`，为未锁定考生生成志愿。
2. `GET /results/summary-by-school` → 返回各校计划/录取/分数线/满额率统计。
3. 提交锁：已 `submitted` 考生跳过、不被覆盖（`07 §7.3.3` 控制器契约待补 → 本测试补）。

> 运行：`cd backend && mvn -o test`（依赖 `spring-boot-starter-test`，阶段1 后 admission/student/school/auth 四服务引入；gateway 无业务逻辑，仅路由）。

## 6.2 需求追溯矩阵（spec 条款 → harness 检查）
| spec 条款 | 要求 | harness 检查 | 类型 | 状态 |
|------|------|------|------|------|
| 03 §3.1 | 校额先于统招、不重复录取 | `quotaAdmittedExcludedFromTongzhao` | 自动 | ✅ |
| 03 §3.2 | 校额候选资格（资格+过线+填报） | `quotaControlLineAndTieBreak...`、`quotaControlLineBoundary` | 自动 | ✅ |
| 02 §2.6 | 校额资格=按初中校名额总数取前 N 名（过控制线） | `QuotaEligibilityServiceTest`（前N入围/超额剔除/达线不足全入围/N=0无资格） | 自动 | ✅ |
| 04 §4.4 | `POST /quota-eligibility/recompute` 重算并返回按校统计 | `QuotaEligibilityServiceTest` + 手动清单 6.4 | 自动/手动 | ✅ |
| 03 §3.3 | 平行志愿、滑档 NOT_ADMITTED、无补录 | `tongzhaoParallelVolunteer...`、`tongzhaoNotAdmittedWhenSeatsExhausted` | 自动 | ✅ |
| 03 §3.4 | 同分比较器 10 级、ticketNo 兜底全序 | `TieBreakComparatorTest` | 自动 | ✅ |
| 03 §3.5 | 不变量（唯一/计划上限/控制线/runId 隔离） | `AdmissionEngineTest` 1-3,8,9 + `repeatedRunsAreDeterministic` | 自动 | ✅ |
| 03 §3.7-1 | 滑档不进入补录 | `tongzhaoNotAdmittedWhenSeatsExhausted` | 自动 | ✅ |
| 03 §3.7-3 | ticketNo 显式兜底确定性 | `TieBreakComparatorTest` | 自动 | ✅ |
| 03 §3.8 | 历史快照 runId 隔离、/runs 对比 | `AdmissionControllerTest.runs*`、`repeatedRunsAreDeterministic` | 自动 | ✅ |
| 04 §4.4 | 提交锁：已提交不可改、submit/reopen | `ApplicationControllerTest` | 自动 | ✅ |
| 04 §4.5 | /runs、/runs/{id} 端点 | `AdmissionControllerTest` | 自动 | ✅ |
| 02 §2.5-3 | Student.submitted 字段 | `ApplicationControllerTest.submitLocksStudent` | 自动 | ✅ |
| 07 §7.2 | HighSchool.tier/zone、JuniorSchool.zone 字段 | 实体字段 + 种子赋值 + `ApplicationSimulatorTest`（因子生效） | 自动 | ✅ |
| 07 §7.3.1 | 校额资格门槛（资格+过线）与对口候选 | `ApplicationSimulatorTest`(用例2/3) | 自动 | ✅ |
| 07 §7.3.2 | 统招按区排名冲稳保分档、不盲目全填重点、≤8 志愿 | `ApplicationSimulatorTest`(用例1/4/5/6) | 自动 | ✅ |
| 07 §7.3.3 | 提交锁跳过已提交考生 | `regenerateAll` 跳过 `submitted`（控制器契约已由 `ApplicationSimulatorControllerTest` 覆盖） | 自动 | ✅ |
| 07 §7.4 | POST /applications/simulate、GET /results/summary-by-school | 端点已落地（`ApplicationSimulatorControllerTest` 覆盖） | 自动 | ✅ |
| 04 §4.6 | 列表端点分页（page/size/sort，返回 `Page<T>` 信封，前端读 `content`） | 6.4 手动清单分页项 + 前端翻页 UI（Admin 考生/结果表） | 手动 | ✅ |

## 6.3 文档-代码一致性检查（MUST）
- 每次改动代码，`MUST` 用 grep 校验：实体字段、枚举值（`QUOTA`/`TONGZHAO`/`ADMITTED`）、路由前缀与本文档一致；本检查为发版门禁（见 6.5）。
- 偏离时 `MUST` 先更新文档（见 `00-INDEX` 0.5）；这是工程宪法（Doc-First）的落地，禁止「先代码后补文档」作为交付态。
- 当前一致性：`runId/runAt` 贯穿 实体-仓库-引擎-控制器；`submitted` 贯穿 实体-控制器-测试，均一致；`hasQuotaEligibility` 已由「总分≥430 布尔」升级为「按初中校名额总数取前 N 名」派生（02 §2.6），并由 `POST /student/quota-eligibility/recompute`（04 §4.4）重算，`StudentTieBreakComparator` 与 03 §3.4 同口径。

## 6.4 端到端手动清单（MUST，每次发版执行）
- [ ] 启动 5 服务，访问 `http://localhost:8080`。
- [ ] `POST /api/auth/login`（admin/admin123）拿到 token。
- [ ] `POST /api/student/quota-eligibility/recompute` 返回 `{eligibleTotal,studentTotal,byJunior[...]}`，各初中校 `eligible == min(quotaTotal, 达线人数)`（校额资格结合名额，02 §2.6）。
- [ ] `POST /api/student/applications/simulate` 返回 `{"generated": N}`，为未锁定考生生成志愿（阶段2）。
- [ ] `POST /api/admission/run/full` 返回 stats（含 runId）。
- [ ] `GET /api/admission/stats` 不重跑，且 `admitted == quotaAdmitted + tongzhaoAdmitted`。
- [ ] `GET /api/admission/results` 含结果，无重复考生跨批次录取。
- [ ] `GET /api/admission/results/summary-by-school` 返回各校计划/录取/分数线/满额率。
- [ ] `GET /api/admission/runs` 返回多次运行列表，可对比。
- [ ] `POST /api/student/students/{id}/submit` 后，再 `POST /api/student/applications` 返回 400（提交锁生效）。
- [ ] `POST /api/student/students/{id}/reopen` 后可再次保存志愿。
- [ ] 前端 `/login` → admin 跳 `/admin`；触发模拟后统计卡有数据。
- [ ] 注册 student 用户并登录 → `/student` 可填志愿、查结果。
- [ ] 列表分页：`GET /api/student/students?page=0&size=2` 返回信封 `{content,totalElements,totalPages,number,size}`，且 `content.length ≤ 2`（04 §4.6）。

## 6.5 当前已实现状态（对齐核对）
- ✅ 5 服务编译/打包通过；端到端一键模拟已跑通。
- ✅ 前端依赖安装成功，可 `npm run dev`。
- ✅ 6.1 自动化测试覆盖：比较器（10 级+全序）、引擎（控制线/平行志愿/不变量/确定性/边界/负向）、录取控制器（/runs 契约）、提交锁控制器（submit/reopen/400 拦截）。
- ✅ 新增「志愿提交锁」与「历史模拟快照」（run_id/run_at + /runs）。
- ✅ 新增阶段 2「考生志愿模拟器」：理性考生 + 按 2025/2026 一分一段表换算区排名的冲稳保，因子含总分/校额资格+初中校/片区距离/学校层次；`POST /applications/simulate` 与 `GET /results/summary-by-school` 已落地，`ApplicationSimulatorTest` 覆盖资格/控制线/区排名冲稳保/层次/8 志愿上限。
- ✅ 列表端点统一分页（04 §4.6）：`GET /auth/users`、`/school/high-schools`、`/school/junior-schools`、`/school/quota-seats`、`/student/students`、`/student/students/junior/{id}`、`/student/applications`、`/admission/results` 返回 Spring `Page<T>` 信封（前端读 `content`）；聚合端点（`/runs`、`/results/student/{id}`、`/results/summary-by-school`）保持全量数组。
- ✅ 校额到校校内排名：`AdmissionResult.schoolRank` 由 `runQuota` 录取时写入（同初中校竞争同一高中的名次），`Admin.vue`/`Student.vue` 模拟录取结果表新增「校内排名」列（QUOTA 显示，统招为「—」）。
- ✅ 录取结果页展示「来源初中」+「各科得分」：`AdmissionResult` 反规范化写入 `juniorSchoolName` 与 `chinese/math/english/physics/politics/pe`（04 §4.5），`Admin.vue`/`Student.vue` 模拟录取结果表新增「来源初中」列与「语文/数学/英语/物理/道法/体育」六列；`Student.vue`「我的成绩」卡片也补「初中校」。`run/full` 重建后已填充（runId=1，录取 4999）。
- ✅ 模拟录取结果页筛选栏：`GET /results` 支持 `juniorSchoolId`/`highSchoolId`/`minScore`/`maxScore`/`status`/`studentName` 过滤（04 §4.5）；`Admin.vue`「模拟录取」Tab 顶部加筛选表单（毕业学校/录取学校/分数范围/录取状态/考生姓名 + 查询/重置按钮），`loadResults` 经 `URLSearchParams` 拼参。`admission 12/12(JDK17)` 此前全过，本次改动用 `Specification` 实现过滤；前端 `npm run build` 通过。
- ✅ 各校录取可视化（05 §5.5）：`Admin.vue` 新增「各校录取可视化」Tab，基于 `GET /admission/results/summary-by-school` 用 CSS 柱状图展示各校录取人数（校额+统招堆叠）与两批次占比，附各校明细表。
- ⚠️ 缺：错误响应结构化（4.7-2）。
- ✅ 阶段1 修复（2026-07-18）：`QuotaEligibilityService.recompute` 此前「达线即给资格、未限前 N 名」偏离 02 §2.6，已改为 `eligibleHere = min(达线人数, N)`（N=该校名额总数），`QuotaEligibilityServiceTest` 4/4 通过（JDK17）；并补齐 school/auth 测试与模拟器控制器契约（解 D6 🔶），DoD 扩至 5/5 服务。

## 6.6 绿灯定义（Definition of Green / DoD）
满足以下全部条件，harness 视为 **GREEN**，方可发版：
1. `mvn -o test`（JDK 17）在 **全部 5 个业务/引擎服务**（admission / auth / school / student；gateway 无业务逻辑不计入）均 **0 失败、0 错误**（6.1 全部自动测试通过，见 `00 §0.5` 2026-07-18 阶段1）。
2. 6.4 手动清单全部勾选通过（或显式记录豁免）。
3. 6.3 一致性 grep 无偏离；若有偏离，文档已先修订并留痕（00 §0.5）。
4. 任一 `MUST` 条款缺失对应 harness 检查 → 视为 **RED**，禁止发版。

## 6.7 度量（GQM 视角）
- 录取正确性度量：`admitted == quotaAdmitted + tongzhaoAdmitted`（无重复、无遗漏）。
- 确定性度量：同输入两次运行结果集合相等（`repeatedRunsAreDeterministic`）。
- 锁完整性度量：已提交考生在测试中 0 次成功写入志愿（`createApplicationBlockedWhenSubmitted`）。

## 6.8 开放问题
1. ✅ **已定**：自动化测试本期已补齐（见 6.1，含边界/负向与控制器契约）。
2. ✅ **已定**：CI（GitHub Actions）已在 `.github/workflows/ci.yml` 落地，push/PR 至 `master` 时自动跑后端 `mvn -B package`（JDK 17，temurin）与前端 `npm install && npm run build`（Node 20），使 6.6 绿灯可自动门禁（详见 00 §0.5 修订记录）。
