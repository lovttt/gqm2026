# 00 · 总纲与文档地图（INDEX）

> 本目录是「北京中考东城区填报志愿与录取模拟系统」的**约束契约（spec/harness）**。
> 规则：文档先于并约束代码。任何代码改动若偏离本契约，**必须先更新对应文档并说明理由**，再改代码。
> **工程宪法（最高约束）**：见 `../CONSTITUTION.md` —— **Doc-First：先改文档，再改代码**，优先级高于本目录所有契约。

## 0.1 项目定位
- 名称：北京中考东城区 · 填报志愿与录取模拟系统
- 性质：教学/模拟系统，不考虑盈利
- 业务边界：**仅覆盖两个批次** —— 校额到校（含校内排名限制）、统一招生（平行志愿）
- 非目标（本期不做）：名额分配、贯通培养、跨区招生、学籍年限真实校验、真实中考成绩对接

## 0.2 Harness 工程原则
0. **文档先于代码（Doc-First 宪法）**：所有交互改动 **必须先改文档、再改代码**（详见 `../CONSTITUTION.md`）。这是最高原则，凌驾本目录其余条款。
1. **契约优先**：本目录文档是系统唯一权威约束源，代码、接口、UI 都必须可追溯到具体条款。
2. **可审查**：每条约束标注强度 —— `MUST`（必须满足）/`SHOULD`（应当满足）/`MAY`（可选）。
3. **偏离即修订**：实现偏离文档时，先改文档、再改代码，并在本文档 `0.5 修订记录` 留痕。
4. **逐节拷问**：本文档采用「先出稿、后逐节拷问细化」流程，每节状态见 `0.4`。
5. **可验证**：`06-quality-harness.md` 定义验收标准，实现必须能被这些标准检验（含自动化测试与手动清单）。

## 0.3 文档地图
| 文件 | 主题 | 约束对象 |
|------|------|----------|
| `../CONSTITUTION.md` | **工程宪法·Doc-First（最高约束）** | 全部 |
| `00-INDEX.md` | 总纲、原则、地图、修订记录 | 全部 |
| `01-architecture.md` | 微服务拆分、路由、鉴权、数据隔离 | 后端架构 |
| `02-data-model.md` | 实体、字段、枚举、唯一约束、派生值 | 后端+种子数据 |
| `03-admission-algorithm.md` | 录取顺序、两批次规则、同分比较器、不变量 | 录取引擎 |
| `04-api-contract.md` | 路由前缀、端点契约、鉴权、错误码 | 后端 API |
| `05-frontend-contract.md` | 技术栈、页面、角色视图、与 API 映射 | 前端 |
| `06-quality-harness.md` | 验收标准、测试、手动清单、一致性检查 | 全部 |
| `07-simulator.md` | 考生志愿模拟器：理性考生策略、因子、算法、端点 | 阶段2 / student-service |
| `08-doc-code-review-2026-07-18.md` | 【评审稿】文档—代码契约对齐审查（待确认清单 Q1~Q9） | 后端+文档 |
| `09-ddd-refactor-design-2026-07-18.md` | 【设计稿】DDD 四层重构设计（充血模型+微服务纯粹性+G7 落库） | 后端重构 |
| `10-system-overview.md` | 当前系统功能全景描述（业务能力/角色/流程/依赖/数据模型/实现状态） | 现状快照·核对 |
| `11-requirements-implementation-matrix.md` | 需求-实现对照表（12 模块 × 需求预期/当前实现/状态 + 差异说明） | 现状快照·核对 |

## 0.4 章节状态
| 章节 | 状态 | 待拷问重点 |
|------|------|------------|
| 工程宪法 | LOCKED | Doc-First：所有交互先改文档再改代码（见 `../CONSTITUTION.md`） |
| 01 架构 | DRAFT | 无注册中心是否够用？SQLite 并发是否需补锁？ |
| 02 数据模型 | REVIEWED | 量纲/控制线/提交锁已定（见 02 §2.3、§2.5） |
| 03 录取算法 | REVIEWED | 同分兜底/平行志愿无罚分/历史快照已定（见 03 §3.4、§3.3、§3.8） |
| 04 API | REVIEWED | 新增 submit/reopen/runs 端点（见 04 §4.4、§4.5） |
| 05 前端 | DRAFT | 页面拆分是否合理？是否需要录取可视化？ |
| 06 质量 | REVIEWED | 自动化测试已补齐（见 06 §6.1） |
| 07 模拟器 | REVIEWED | 理性考生策略/冲稳保/因子(层次+片区)已定（见 07 §7.3） |
| 08 评审稿 | REVIEWED | Q2-Q9 待确认（见 08 §5），Q1 已裁决 |
| 09 重构设计 | DRAFT | 四层落地后是否需补 L4 统一异常/Request DTO 规范？ |
| 10 全景描述 | SNAPSHOT | 代码现状快照，供逐项核对；含前端 UI 接线补齐（注册/用户管理/数据备份/历史对比/提交锁定）；待裁决项指向 08 §5 |
| 11 需求实现对照 | SNAPSHOT | 12 模块对照；✅#8 同分比较器已补 10 级（03 §3.4）；✅#11 前端 UI 接线补齐（§2 #11）；✅#12 网关无守卫为 Q5 裁决接受；✅#7/#9 共享池口径已文档同步（03 §3.2/04 §4.4）；详见 §2 |

状态图例：`DRAFT`（初稿，待拷问）→ `REVIEWED`（已拷定）→ `LOCKED`（冻结，改动需评审）

## 0.5 修订记录
| 日期 | 章节 | 变更 |
|------|------|------|
| 2026-07-12 | 全部 | 初稿生成，对齐已实现的 5 服务 + Vue 前端 |
| 2026-07-12 | 02,03 | 校准分数量纲为真实北京中考 510（卷面400+附加110） |
| 2026-07-12 | 02 | 开放问题1、2已定：控制线430、不细分字段；种子代码按510分布重写 |
| 2026-07-12 | 03,04,06,02 | 拷定：ticketNo确定兜底/平行志愿无罚分/无补录/不按多序号择优；补齐提交锁+历史快照+自动化测试 |
| 2026-07-12 | 06,00 | 重审 harness 工程充分性：补追溯矩阵(6.2)/边界负向用例(6.3→6.1)/控制器契约测试/绿灯定义(6.6)；新增 AdmissionControllerTest、ApplicationControllerTest 与引擎边界测试 |
| 2026-07-12 | 02,07,04 | 新增考生志愿模拟器(阶段2)：HighSchool.tier/zone、JuniorSchool.zone；理性考生+冲稳保算法；POST /applications/simulate 与 GET /results/summary-by-school；移除种子朴素志愿生成 |
| 2026-07-12 | 02,03,04,07,06,00 | 文档-代码一致性同步：录取引擎确认为平行志愿（分数优先、看完一考生全部志愿再下一个，非顺序志愿）；志愿模拟器改为按 2025/2026 一分一段表换算区排名的冲/稳/保；新增 `ScoreSegment` 实体(02 §2.1)与 `/export` 的 `scoreSegments`(04 §4.3)；校准种子数据(5729 考生/一分一段表)与测试/追溯矩阵描述(06/07) |
| 2026-07-12 | 00,../CONSTITUTION | 立工程宪法（Doc-First）：所有交互必须先改文档再改代码；0.2 增最高原则、0.3/0.4 纳入宪法，宪法为最高约束（LOCKED） |
| 2026-07-12 | 02,03,04,05,06,00 | 校额资格结合初中校名额：`hasQuotaEligibility` 改为「按初中校名额总数 N 取前 N 名（且过控制线）」派生（02 §2.6）；新增 `POST /student/quota-eligibility/recompute`（04 §4.4）；考生管理加「按名额重算校额资格」按钮（05）；补追溯矩阵（06） |
| 2026-07-12 | 06,00 | 验收门槛强化：`06 §6.3` 文档-代码一致性检查由 `SHOULD` 升为 `MUST`（发版门禁），并补当前一致性含校额资格改造 |
| 2026-07-12 | 06,00,build | 测试运行环境修复与验证：`backend/pom.xml` 覆盖 `byte-buddy 1.16.1` + `mockito 5.18.0`（JDK 25 下 `student-service` 13/13 全过，含 `QuotaEligibilityServiceTest`）；`admission-service` **12/12 在 JDK 17**（Microsoft Build 17.0.14）下全过——其在 JDK 25 下因 Mockito「non-public parent」硬限制预存失败，属运行 JDK 与项目目标 JDK 17 不匹配、非本次回归。**建议 CI/本地测试统一用 JDK 17**（本机已置于 `D:\jdk\msjdk17\jdk-17.0.14+7`） |
| 2026-07-13 | 04,06,00,frontend | 列表分页落地（解 04 原开放问题 4.6-1）：04 §4.6 定为 MUST 并定义 `page/size/sort` + `Page<T>` 信封契约；auth/school/student/admission 共 8 个列表端点改 `Page<T>(Pageable)`，仓库加 `Page` 查询；前端 Admin/Student 消费 `.content`，Admin 考生/结果表加 `el-pagination`；student 13/13(JDK25)、admission 12/12(JDK17) 全过，前端 `npm run build` 通过 |
| 2026-07-13 | 04,05,06,00,frontend,admission | 校额校内排名 + 各校录取可视化：`AdmissionResult` 新增 `schoolRank`（runQuota 录取时写入，同初中校竞争同一高中名次），Admin/Student 模拟录取结果表加「校内排名」列；Admin 新增「各校录取可视化」Tab（基于 `GET /admission/results/summary-by-school`，纯 CSS 柱状图：各校录取人数堆叠 + 两批次占比 + 明细表），无新依赖；05 §5.5、04 §4.5、06 §6.5 留痕；admission 12/12(JDK17) 全过、前端 build 通过 |
| 2026-07-13 | 00,backend,frontend | 全服务重启（落地校内排名+可视化）：离线 `mvn package` 重打 `admission-service` jar（含 `schoolRank` 列，Hibernate `ddl-auto:update` 自动建列），其余 4 后端用现有 jar 原地重启（未清库）；前端 `npm run dev` 重启；`POST /admission/run/full` 生成最新运行 **runId=4**（录取 4999 = 校额 335 + 统招 4664）。校验：QUOTA 结果 `schoolRank` 已返回、网关 8080 / 前端 5173 就绪 |
| 2026-07-13 | 04,05,06,00,backend,frontend | 录取结果页补「来源初中 + 各科得分」：`AdmissionResult` 反规范化加 `juniorSchoolId/juniorSchoolName` + `chinese/math/english/physics/politics/pe`，`runQuota/runTongzhao` 保存时写入；Admin/Student 结果表加「来源初中」列与「语文/数学/英语/物理/道法/体育」六列，Student「我的成绩」卡片补「初中校」。`admission 12/12(JDK17)` 测试通过、前端 `npm run build` 通过。**坑**：SQLite 下 `ddl-auto:update` 未给旧表自动加新列（SQLITE_ERROR no such column），改用 `--spring.jpa.hibernate.ddl-auto=create` 一次性重建 admission 表后 `run/full` 重新填充（runId=1，录取 4999）；后续普通 `update` 启动因列已存在故正常 |
| 2026-07-13 | 04,06,00,backend,frontend | 模拟录取结果页新增筛选栏：`GET /results` 增加可选过滤参数 `juniorSchoolId`(毕业学校)/`highSchoolId`(录取学校)/`minScore`+`maxScore`(分数范围)/`status`(ADMITTED\|NOT_ADMITTED)/`studentName`(姓名模糊)，后端用 JPA `Specification` 组合；`Admin.vue`「模拟录取」Tab 顶部加筛选表单（毕业学校/录取学校/分数范围/录取状态/考生姓名 + 查询/重置），`loadResults` 经 `URLSearchParams` 拼参。admission 重建(JDK17) + 前端 `npm run build` 通过；校验：status/juniorSchoolId/highSchoolId/分数范围过滤均生效（最新运行 5729 条：录取 4999 / 未录取 730） |
| 2026-07-13 | 04,00,backend,frontend | school-service 重建对齐分页契约（解下拉框空）：此前运行的是旧 jar（列表端点返回 `List`/数组），前端 `loadAll` 用 `.content` 取到 `undefined`、毕业/录取学校下拉框为空。用 JDK17 重建 jar（JDK17 编译可在 JDK25 运行，与 admission 一致）后，`GET /school/high-schools`、`/junior-schools`、`/quota-seats` 均返回 `Page<T>`（高中 29 / 初中 32，含 `content`），与 04 §4.6 契约及 admission 统一。**坑**：`school-service` 启动工作目录须为 `backend/school-service`（`SeedDataService.seedDir` 默认 `../data` 指向 `backend/data` 的 csv），若设为 `backend` 会 `FileNotFoundException: ..\data\high_school_seed.csv` 启动崩溃。前端 `loadAll` 已加 `Array.isArray(x)?x:x.content` 兼容，现走 `.content` 分支，下拉框正常 |
| 2026-07-13 | 04,06,00,student,backend,frontend | 考生管理页空 → 按 2023 小升初班数估算生成考生：根因 student-service 旧 jar 的 `GET /student/students` 返回数组，前端 `loadStudents` 用 `.content` 取 `undefined` 致表格空（数据其实已有 5729）。新增 `StudentGenerator`：读 `junior_school.csv` 的**班数**，每校生成 `班数×班额(默认40)` 名考生（分数分布取自 2026 一分一段表 334~500），初中校 id 优先按 school-service `/school/export` 的 name 对齐（不可达回退本地种子顺序）；`SeedDataService` 首次启动改调它；`StudentController` 新增 `POST /student/generate?perClass=40`（先清空考生与志愿再生成）。前端 `Admin.vue`「考生管理」加「按班数生成考生」按钮 + 班额输入框，`loadStudents` 与 `Student.vue` 的 `.content` 取值均加 `Array.isArray` 兼容。student-service 用 JDK17 重建 + 前端 `npm run build` 通过；实测重建后接口返回 `Page`、调用生成得 **10120 名考生（班数合计 254×40）**，并重新生成志愿 120214 条 |
| 2026-07-13 | 04,06,00,student,backend,frontend | 考生管理页新增搜索条件：后端 `StudentController.listStudents` 加可选参数 `juniorSchoolId`/`minTotal`/`maxTotal`/`quotaEligibility`，用 `JpaSpecificationExecutor` 构建可空组合筛选（`StudentRepository` 改继承 `JpaSpecificationExecutor<Student>`）；前端 `Admin.vue`「考生管理」Tab 顶部加行内表单（初中校下拉 + 总分区间 + 校额资格下拉【已获/无】+ 查询/重置），`loadStudents` 改为带 `URLSearchParams` 拼参、`searchStudents`/`resetStudentFilters` 重置回第 1 页。student-service JDK17 重建 + 前端 build 通过；实测：`quota=true` 8007 / `quota=false` 2113（合计 10120）、`minTotal=480` 1228、`juniorSchoolId=1` 120，且学校+分数区间组合正常 |
| 2026-07-13 | 04,06,00,school,backend,frontend | 学校管理展示补全：① 初中校管理加 **班数**(`classCount`)/**毕业生数**(`gradCount`) 列 + 编辑表单字段；`JuniorSchool` 实体加两字段（SQLite 不支持 `ALTER ... ADD COLUMN ... NOT NULL`，故标 `@Column(nullable=true)`），`SeedDataService.backfillJuniorSchoolStats()` 每次启动按 `junior_school.csv`（初中校,班数,毕业生数）**名称对齐**回填（幂等），首次播种后亦回填；② 高中校管理表加 **校额到校** 列，前端 `quotaForHs(id)` 对 `quotaSeats` 按 `highSchoolId` 聚合名额合计（与现有统招列并列）。school-service JDK17 重建 + 前端 build 通过；**坑**：改 entity 加 NOT NULL 列致旧 `school.db` 启动崩溃（`Cannot add a NOT NULL column with default value NULL`），需删 `backend/school-service/data/school.db` 重建 schema 并由 CSV 重新播种。实测：初中校 32/32 均有班数+人数（一中 3/120、二分 21/840…），高中校 统招+校额（二中 166/61、五中 170/44…） |
| 2026-07-18 | 06,00,repo | 阶段0·恢复 Harness 完整性（Doc-First 漂移修复）：① 上轮已落地的 `.github/workflows/ci.yml`（push/PR 至 master 自动跑后端 `mvn -B package` JDK17 + 前端 `npm run build` Node20）此前未在文档留痕，构成 Doc-First 漂移；现 06 §6.8-2 由「建议下一轮补 CI」改为「已定」对齐现状；② 清理 7 个编辑器锁/备份残留 `.LCK*.java~`（admission/auth/student/school 各 security/config 包），仓库噪音消除。文档先改、再清文件，符合 00 §0.2 / 06 §6.3 |
| 2026-07-18 | 02,06,00,auth,school,student | 阶段1·补齐测试安全网（TDD）+ 修偏离：① school/auth 两服务补 `spring-boot-starter-test` 并新增校额名额分组合并、控制线读取/upsert、种子回填幂等、认证登录/JWT/注册、JWT round-trip 共 7 类测试；② student 补 `ApplicationController.simulateApplications` 控制器契约 + `ApplicationSimulator.regenerateAll` 提交锁跳过已提交考生契约（解 D6 🔶）；③ **修偏离**：`QuotaEligibilityService.recompute` 此前达线即给资格、未限「按初中校名额总数 N 取前 N 名」（违反 02 §2.6 / 06 §6.2），改为 `eligibleHere = min(达线人数, N)`；DoD 由 2/5 服务扩至 5/5（`mvn -o test` JDK17 全绿：auth 7 / school 9 / student 24） |
| 2026-07-18 | 01,02,04,05,06,07,00,README | 文档-代码契约对齐（grill-with-docs 审查，见 `08-doc-code-review-2026-07-18.md`）：① 修错误 E1-E4——01 §1.1 进程数 6→5、04 §4.3 控制线 `/control-lines`→`/control-line`、04 §4.4 整页保存 `/applications/batch`→`/applications/student/{studentId}`（删 `/students/{id}/applications`）、04 §4.1 错误响应补 400/404 结构化；② 同步过时 O1-O6——02 §2.4 考生数 5729→10120（按班数）、补 Student.submitted / JuniorSchool.classCount+gradCount / AdmissionResult 反规范化列、06 §6.6 绿灯扩至 5 服务、06 §6.1 阶段1 🔶→✅；③ 补遗漏 M1-M8——02 新增 ScoreLine 实体、04 补生成器 `/generator/generate` 与 `/generate` 端点、05 §5.2 补 `/applications`(Application.vue) 与 `/generator`(Generator.vue) 两页、07 §7.8 补单考生生成器描述、README 考生数同步；④ **Q1 已裁决**：贯通批次为演示占位、不录取，README 下调描述、02 §2.2 / 07 §7.5 标注 `guantongPlan` 未接入录取。待确认 Q2-Q9 见 `08 §5` |
| 2026-07-18 | 00,10（新增现状快照） | 系统功能全景描述落盘（10-system-overview.md）：基于实时代码扫描，按业务能力/角色/流程/外部依赖/数据模型/实现状态六维呈现现状；待裁决项指向 08 §5。DDD 重构（09）已逐服务落地、5 服务单测全绿（auth 7 / school 9 / student 24 / admission 12） |
| 2026-07-18 | 08,04,05,start-all,00 | 08 评审稿收口：Q2/Q3/Q4/Q6/Q8 标 ✅resolved（随 09 落地：gaokaoTier/comprehensiveEval/crossDistrict 落库、控制线仅 QUOTA、统一错误体 `{message}`）；裁决 Q5（生成器 ADMIN/STUDENT 双开放、后端不额外加角色限制，与前端零改动一致）、Q7（学生数统一 10120 班数口径）、Q9（校额分组本期硬编码）。同步正式 spec：05 §5.2 双开放、04 §4.1/§4.7-2 错误体 `{message}`、start-all.ps1「5729」→「约 10120」；README/02 §2.4 已于 2026-07-13 先行同步。08 全文由「草稿待确认」转为「评审稿·Q1-Q9 已裁决收口」 |
| 2026-07-18 | 00,11（新增对照表） | 需求-实现对照表落盘（11-requirements-implementation-matrix.md）：12 模块逐项反推"当前代码实现"并对照 docs/spec/01~07 需求预期。状态：#1-#4/#6/#10/#11 ✅一致；#5/#12 ⚠️部分一致（生成器占位项、网关无服务端守卫）；#7/#9 ✅*（引擎共享名额池与 03 §3.2 字面口径漂移、schoolRank 实记共享组排名）；**#8 ❌关键缺失**：`TieBreakComparator` 仅 4 级，违反 03 §3.4 的 10 级链（缺语数外三科/物理+道法/物理/道法/体育），属 Doc-First 触发项，须先修订 03 或代码再留痕 |（见 `09-ddd-refactor-design-2026-07-18.md`，**设计稿/未改代码**）：经 grill 逐层确认三总原则——**微服务纯粹性优先**（不建 gqm-common，`Score`/`Batch`/`AdmissionStatus`/`TieBreakComparator` 各上下文各一份）、**充血模型**（规则内聚实体/VO/领域服务，controller 瘦化）、**G7 本轮落库**；并落地 08 §5 裁决——**Q2** HighSchool 加 `gaokaoTier(TOP/HEAD/MID)`、**Q3** Student 加 `comprehensiveEval(A/B/C/D)` 且纳入真实门槛（**C/D 校额失格、统招不受影响**）、**Q4** Student 加 `crossDistrict`（占位不生效）、**Q6** `guantong` 不落库仅生成器演示输出；四层设计：L1 领域模型（充血行为表）、L2 应用服务（每聚合一 AppService + 显式事务、HTTP 拉取移出 TX）、L3 基础设施（SimulationRun 维持 `max(runId)+1` 派生、配置维持 `@Value`、外部调用端口化 ACL）、L4 接口层（成功响应直通不包装、错误体固定 `{message}` 匹配前端契约、各服务自校验鉴权）——**前端零改动**。实现待确认后逐服务落地（school→student→admission→auth），落地后再按 Doc-First 同步 01/02/04 并留痕 |
| 2026-07-18 | 11,00 | **#8 修复（Doc-First 代码侧）**：`TieBreakComparator.STEP` 由 4 级补全为与 `03 §3.4` 一致的 **10 级链**（总分→语数外三科→语文→数学→英语→物理+道法→物理→道法→体育→ticketNo），第 10 级 `ticketNo` 确定性兜底保留。`TieBreakComparatorTest` 新增 语数外三科(级2)/物理+道法(级6)/物理(级7)/全序确定性 4 例（共 7 例全绿），`AdmissionEngineTest` 6 例不回归，admission-service `BUILD SUCCESS`。spec `03 §3.4` 本就正确，本次为代码对齐 spec，非 spec 修订；对照表 11 的 #8 状态 ❌→✅，§0.4 章节状态同步更新 |
| 2026-07-18 | 05,10,11,00,frontend | **前端 UI 接线补齐 + 现状快照同步（Doc-First）**：将此前仅后端存在、前端无入口的契约端点补齐为可操作 UI（纯前端接线、无后端改动）——`Login.vue` 注册入口（`/auth/register`）；`Admin.vue` 新增「用户管理」（`/auth/users`）/「数据备份」（`/school|/student export|import`）/「历史运行对比」（`/admission/runs`+`/runs/{id}`+A/B 对比）三 Tab，「考生管理」加单考生增/删/改+志愿状态列+管理员撤回（`/reopen`）+独立志愿模拟按钮（`/applications/simulate`）；`Student.vue` 提交志愿锁定（`/submit`）。同步现状快照：`10 §1/§6` 补账号管理能力与前端接线清单、`11 §1 #11` 与 `§2 #11` 补 UI 接线说明、`§0.4` 章节状态更新；顺修 `11 #11` 原笔误 `router.beforeEmbed`→`router.beforeEach`。前端 `npm run build` 通过、无 lint 错误；无新增后端端点，不涉及 spec 契约（04/05）变更 |
| 2026-07-18 | 03,04,11,00 | **#7/#9 文档口径同步（Doc-First）**：`03 §3.2` 校额到校分组改写为「`quotaGroups` 共享名额池（Q9 硬编码）」口径——同一组内多所初中校共用名额池、按 `(共享组 id, highSchoolId)` 汇总 `QuotaSeat.quota`，`schoolRank` 实为「共享组内分数排名」（非单所初中校内排名）；`04 §4.4` 的 `schoolRank` 描述同步改为「共享组内排名」。消除 spec 字面 `(juniorSchoolId,highSchoolId)` 与引擎实现（`AdmissionEngine.runQuota` 的 `groupKeyOf` 共享池聚合）的漂移，对照表 11 的 #7/#9 状态由漂移提示改为已对齐。**#12 裁决留痕**：网关无服务端角色守卫为 Q5 有意裁决（后端不额外加角色限制、前端零改动），对照表 11 #12 状态 ⚠️→✅（裁决接受），代码不改；若未来需防越权直调再评估补服务端守卫（架构增强、非本期契约强制） |

## 0.6 技术栈总览（约束基线）
- 前端：Vue 3 + Vite + Pinia + Vue Router + Element Plus + Axios
- 后端：Java 17+ / Spring Boot 3 / Spring Cloud Gateway / Spring Security
- 数据：SQLite（每服务独立库），JPA + Hibernate
- 鉴权：JWT（jjwt），共享密钥，无注册中心
- 构建：Maven（后端多模块）、npm（前端）
