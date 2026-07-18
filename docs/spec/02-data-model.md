# 02 · 数据模型约束（DATA MODEL）

> 约束对象：实体结构与种子数据。字段名以代码实体为准。

## 2.1 实体清单
### HighSchool（高中）`high_school`
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long (PK, 自增) | MUST |
| name | String | MUST，唯一可读名 |
| district | String | DEFAULT '东城区' |
| tongzhaoQuota | int | MUST，统招招生计划数（≥0） |
| tier | String | MUST，`'KEY'`(重点)/`'NORMAL'`(普通)，默认 `'NORMAL'`（见 07 §7.2 志愿模拟「学校层次偏好」因子） |
| zone | int | MUST，所属片区(东城区内 1~N)，默认 1（见 07 §7.2「区域/离家距离」因子） |

### JuniorSchool（初中校）`junior_school`
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long (PK) | MUST |
| name | String | MUST |
| district | String | DEFAULT '东城区' |
| zone | int | MUST，所属片区(东城区内 1~N)，默认 1（见 07 §7.2「区域/离家距离」因子） |
| classCount | int | SHOULD，班数（阶段1 起按 `junior_school.csv` 回填，`SeedDataService.backfillJuniorSchoolStats` 幂等；SQLite 不支持 NOT NULL 加列，故 `nullable=true`）|
| gradCount | int | SHOULD，毕业生数（同上回填）|

### QuotaSeat（校额到校名额）`quota_seat`
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long (PK) | MUST |
| juniorSchoolId | Long | MUST，FK→junior_school |
| highSchoolId | Long | MUST，FK→high_school |
| quota | int | MUST，名额数（≥0） |
| 唯一约束 | (juniorSchoolId, highSchoolId) | MUST，一对初中-高中仅一条名额 |

### ControlLine（控制线）`control_line`
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long (PK) | MUST |
| type | String | MUST，当前仅 `'QUOTA'`（校额到校全区最低控制线） |
| value | int | MUST，控制线分数 |

### ScoreSegment（一分一段表）`score_segment`（school 库）
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long (PK, 自增) | MUST |
| year | int | MUST，`2025`(历史对照，用于换算学校录取排名) / `2026`(当年，用于换算考生排名) |
| score | int | MUST，分数（510 量纲，430~500；"`500分及以上`"记 500，"300以下"不纳入） |
| headcount | int | SHOULD，该分数段人数（每 1 分段人数），用于按比例生成考生 |
| cumulative | Integer | MUST，累计人数（≥该分数的人数）= **区排名**，用于志愿模拟「分数→区排名」换算 |

### ScoreLine（统招线）`score_line`（school 库）
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long (PK, 自增) | MUST |
| highSchoolId | Long | MUST，FK→high_school |
| year | int | MUST，年份（2025 历史统招线，用于换算学校录取排名） |
| line | int | MUST，该校该年统招录取线（510 量纲） |
> 代码位置：`school-service/.../entity/ScoreLine.java` + `repository/ScoreLineRepository.java`；被 `07 §7.3.2` 引用换算 2025 区排名。

### Student（考生）`student`
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long (PK) | MUST |
| name | String | MUST |
| ticketNo | String | MUST，准考证号 |
| juniorSchoolId | Long | MUST，FK→junior_school |
| chinese/math/english/physics/politics/pe | int | MUST，单科成绩 |
| totalScore | int | **派生值**，见 2.3 |
| hasQuotaEligibility | boolean | MUST，是否具备校额到校资格。**不再是「总分≥430」的单一布尔，而是结合初中校名额总数派生**：见 2.6「校额资格判定」，由 `POST /student/quota-eligibility/recompute` 重算。 |
| submitted | boolean | MUST，志愿提交锁（提交后不可增删改志愿）；由 `POST /student/students/{id}/submit` 置 true、`/reopen` 置 false（见 04 §4.4、06 §6.1）|

### Application（志愿）`application`
| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long (PK) | MUST |
| studentId | Long | MUST，FK→student |
| batch | String | MUST，`'QUOTA'` 或 `'TONGZHAO'` |
| priority | int | MUST，志愿序号（从 1 开始，统招通常 1~8） |
| highSchoolId | Long | MUST，FK→high_school |
| 唯一约束 | (student_id, batch, priority) | MUST，同批次同序号不重复 |

### AdmissionResult（录取结果）`admission_result`（admission 库）
| 字段 | 约束 |
|------|------|
| id, studentId, studentName, ticketNo | MUST |
| batch | `'QUOTA'`/`'TONGZHAO'` |
| highSchoolId, highSchoolName | 录取高中（落榜为 null） |
| totalScore | 快照考生总分 |
| status | `'ADMITTED'`/`'NOT_ADMITTED'` |
| note | 录取说明（含校内排名/志愿序号） |
| runId | 模拟运行批次号（历史快照，见 03 §3.8） |
| runAt | 本次模拟运行时间 |
| createdAt | 记录写入时间戳 |
| schoolRank | Integer | 校内排名：仅 `batch=QUOTA` 且 `ADMITTED` 有值（同初中校竞争同一高中校额名次），其余 null（见 04 §4.5） |
| juniorSchoolId / juniorSchoolName | Long / String | 来源初中校（反规范化，便于结果页直显，见 04 §4.5） |
| chinese/math/english/physics/politics/pe | int | 各科得分（反规范化自 student 快照，见 04 §4.5） |
> 代码位置：`admission-service/.../entity/AdmissionResult.java`；由 `runQuota`/`runTongzhao` 保存时写入。|

## 2.2 枚举值约定（MUST）
- `batch`：`QUOTA`（校额到校）、`TONGZHAO`（统招）。
- `status`：`ADMITTED`、`NOT_ADMITTED`。
- `ControlLine.type`：本期仅 `QUOTA`。
- ⚠️ **贯通批次（GUANTONG）为演示占位，本期不录取**：生成器 `GeneratorController` 会产出 `guantongPlan`（与 `quotaPlan`/`tongzhaoPlan` 并列），但录取引擎（03）仅消费 QUOTA+TONGZHAO；`Application.batch` 枚举仅 `QUOTA`/`TONGZHAO`，`guantongPlan` 不入志愿库、不进入录取。README 已据「演示占位」口径下调描述（见 `00 §0.5` 2026-07-18 一致性同步）。

## 2.3 分数结构与量纲（MUST，真实北京中考满分 510）
满分 510 = 卷面 400 + 附加 110：
- 卷面 400 = 语文100 + 数学100 + 英语笔试60 + 物理笔试70 + 道法笔试70
- 附加 110 = 体育50 + 道法附加10 + 物理附加10 + 英语听力40

实体单科字段（计入录取的总分，MUST）：
| 字段 | 满分 | 构成 |
|------|------|------|
| chinese | 100 | 纯卷面 |
| math | 100 | 纯卷面 |
| english | 100 | 笔试60 + 听力40 |
| physics | 80 | 笔试70 + 附加10 |
| politics | 80 | 笔试70 + 附加10 |
| pe | 50 | 体育 |
| totalScore | 510 | 六科之和（派生，见 2.4） |

区分度约定（SHOULD，用于种子生成）：
- 附加 110（体育/道法附加/物理附加/英语听力）学生基本满分，区分度低；
- 道法基本满分（卷面70+附加10≈80）；
- 主要区分度在：语文、数学、物理笔试(0~70)、英语笔试(0~60)、道法笔试(0~70)。

## 2.4 派生值与数据来源
- `Student.totalScore` **MUST** 由 `@PrePersist/@PreUpdate` 计算：`chinese+math+english+physics+politics+pe`，代码不得直接赋值绕过。
- 系统 `SHOULD` 内置种子数据：`school-service` 播种高中/初中校/校额名额/控制线 + **一分一段表（2025/2026，来自 `backend/data/score_segment_2025.csv` / `score_segment_2026.csv`，幂等）**；`student-service` 由 `StudentGenerator` **按各初中校班数 × 班额（默认 40）** 生成 **约 10120 名**考生（班数合计 254 × 40，见 `00 §0.5` 2026-07-13 修订；早期「5729（按一分一段表≥430 比例）」口径已弃用）。志愿不写死，由模拟器 `POST /applications/simulate` 或单考生生成器 `POST /generator/generate` 生成（见 07 §7.4 / §7.8）。
- 种子生成 **MUST** 体现 2.3 区分度分布：`pe=50`；`politics≈80`（偶尔小扣）；`physics=70+（附加10基本满）`；`english=60+（听力40基本满）`；`chinese/math/物理笔试/英语笔试/道法笔试`按随机分布拉开区分度。
- `SHOULD` 支持 JSON 整体导入导出（`/school/export`、`/school/import`、`/student/export`、`/student/import`）；`MAY` 支持 CSV 导入考生。

## 2.5 开放问题
1. ✅ 已定：实体保持单科总分值，不拆"卷面/听力/附加"字段。
2. ✅ 已定：校额到校全区最低控制线 = 430（510 量纲，430 分以上具备资格）。
3. ✅ 已定：新增 `Student.submitted` 志愿提交锁（提交后志愿不可增删改，见 04 §4.4）；历史模拟快照通过 `admission_result.run_id` + `/runs` 实现（见 03 §3.8）。
4. ✅ **已定**：校额到校资格 `hasQuotaEligibility` **结合初中校名额总数**判定（**按初中校取前 N 名**），不再是单纯「总分≥430」布尔。规则见 2.6。

## 2.6 校额到校资格判定（MUST，结合初中校名额总数）
校额到校名额是**按初中校分配**的：某初中校被分配的校额名额总数 `N = Σ QuotaSeat.quota（该初中校对所有高中的名额之和）`。资格判定：
1. **控制线前置**：`totalScore >= ControlLine(QUOTA).value`（430）为必要条件，低于控制线一律无资格。
2. **校内排名取前 N**：对**同一初中校**内满足控制线的考生，按 `TieBreakComparator` 降序（总分优先，`ticketNo` 兜底，口径同 03 §3.4）排名；**排名前 `N` 名** `hasQuotaEligibility = true`，其余（含超出前 N 名、低于控制线者）为 `false`。
3. **边界**：若该初中校满足控制线的考生数 `< N`，则这些考生全部具资格（资格数 = `min(N, 达线人数)`）；若 `N = 0`（该校未分配名额），则该校无人具资格。
4. **归属与触发**：`hasQuotaEligibility` 由 `student-service` 计算并持久化；名额（`quota_seat`）与控制线来自 `school-service GET /school/export`。通过 `POST /student/quota-eligibility/recompute` 重算（见 04 §4.4）。种子期先以「总分≥430」作为初始值，**须运行 recompute 使其结合名额**；名额/控制线/成绩变动后应重算。
5. **与录取的关系**：本判定决定「谁有资格填报并参与校额到校投档」；实际录取仍按 03 §3.2 在 `(初中校,高中)` 组内按 `QuotaSeat.quota` 取前几名。资格是**入围前置**，录取是**分校额投档**，二者口径一致但阶段不同。
