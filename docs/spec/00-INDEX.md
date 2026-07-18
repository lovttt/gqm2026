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

## 0.6 技术栈总览（约束基线）
- 前端：Vue 3 + Vite + Pinia + Vue Router + Element Plus + Axios
- 后端：Java 17+ / Spring Boot 3 / Spring Cloud Gateway / Spring Security
- 数据：SQLite（每服务独立库），JPA + Hibernate
- 鉴权：JWT（jjwt），共享密钥，无注册中心
- 构建：Maven（后端多模块）、npm（前端）
