# 07 · 考生志愿模拟器（QUALITY / SIMULATOR）

> 约束对象：阶段 2「模拟每个考生，结合自身情况自动填报志愿」。
> 对应业务流：基础数据 → **志愿模拟** → 录取引擎 → 查看各校录取情况。
> 本模块解决原系统缺口：此前志愿是种子写死 / 手动提交，没有「按考生自身情况」的自动填报。

## 7.1 目标与定位

- 给定考生档案（总分 / 初中校 / 校额资格）与学校参考数据（校额名额 / 控制线 / 高中层次 / 片区），**自动产出**每个考生的校额到校志愿 + 统招平行志愿。
- 策略：**理性考生**（用户裁定 2026-07-12）。不追求随机分布，而追求「真实合理的填报」，使录取结果能反映分数段向各校的流动。
- 纳入因子（用户裁定 2026-07-12）：**总分（必选）** + **校额资格 / 初中校** + **区域 / 离家距离** + **学校层次偏好**。

## 7.2 数据模型新增（见 02 §2.1，MUST）

| 实体 | 字段 | 类型 | 约束 |
|------|------|------|------|
| HighSchool | `tier` | String | MUST，`'KEY'`(重点) / `'NORMAL'`(普通)，默认 `'NORMAL'` |
| HighSchool | `zone` | int | MUST，所属片区(东城区内 1~N)，默认 1 |
| JuniorSchool | `zone` | int | MUST，所属片区(东城区内 1~N)，默认 1 |

- 「区域 / 离家距离」因子 = `|考生初中校.zone − 高中.zone|`（同片区=0，越近越优先）。
- 种子数据（school-service）须为 5 所高中、4 所初中校分配 tier / zone（见 02 §2.4）。
- 志愿模拟另依赖 **一分一段表 `ScoreSegment`（2025/2026）**：用于把分数换算成区排名（见 02 §2.1、§7.3.2）；`school-service` 在种子期幂等导入（见 02 §2.4）。

## 7.3 模拟算法（MUST，可复现、确定性）

实现位于 `student-service`：`simulator/ApplicationSimulator.java`（`simulate` 为纯函数，可单测）。
参考数据 `ReferenceData` 由 `SchoolDataFetcher` 从 `school-service GET /school/export` 拉取（经 admin-JWT）。

### 7.3.1 校额到校志愿
- **资格门槛（MUST）**：仅当 `hasQuotaEligibility == true` **且** `totalScore >= controlLine(430)` 才填报校额志愿；否则该考生无校额志愿。
- **候选校（MUST）**：该考生初中校在对 `quota_seat` 中「有名额(quota>0)」的高中集合。
- **排序（MUST）**：按 `tier` 优先（KEY 在前），同层次按离家距离升序。序号 1..k 写入 `Application.priority`。

### 7.3.2 统招平行志愿（按区排名冲 / 稳 / 保，拉开档次，不全填重点）
- **普高线（MUST）**：仅当 `totalScore >= controlLine(430)` 才填报统招志愿（低于普高线不录取，理性考生不填报）。
- **排名换算（MUST）**：用两套一分一段表把「分数」换算成「区排名」（累计人数 = ≥该分数人数，与 Excel「25 年统招区排名」口径一致）：
  - 考生区排名：`studentRank = cumulativeRank(2026, totalScore)`（用当年 2026 表）。
  - 学校录取排名：`schoolRank = cumulativeRank(2025, line2025)`，其中 `line2025` 为该校 2025 统招线（取自 `score_line` 表）。
  - 缺 2025 线学校回退到 `estimateLine = controlLine + tierBoost + compBoost`，再换算 2025 区排名；`tierBoost`：`KEY`+40；`compBoost = max(0,(120−tongzhaoQuota))/4`。
- **分档（MUST）**：`diff = schoolRank − studentRank`（<0 表示学校录取排名高于考生，学校更难）：
  - 冲 `REACH`：`−700 ≤ diff < −60`（学校比考生高 60~700 名）
  - 稳 `MATCH`：`−60 ≤ diff ≤ +400`
  - 保 `SAFETY`：`+400 < diff ≤ +1100`
  - 其余（`diff < −700` 太冒险，或 `diff > +1100` 过于保底）不填报。
- **排序（MUST）**：先 `REACH` → `MATCH` → `SAFETY`；组内 `REACH` 按 `diff` 从大到小（最接近考生、最不冒险的冲在前），`MATCH`/`SAFETY` 按 `|diff|` 升序（最接近考生的稳/保在前）。
- **上限（MUST）**：配额 `冲≤3、稳≤3、保≤2`，共 **≤8** 个（序号 1..8）。
- **不盲目全填重点（MUST）**：普通校按排名自然落入稳/保档；若最终 8 个志愿均为 `KEY`，则强制加入最接近的一所 `NORMAL` 作保底（贴近真实考生行为）。

### 7.3.3 提交锁（MUST）
- `regenerateAll()` 跳過 `submitted == true` 的考生，**不覆盖**已提交锁定的志愿（与 04 §4.4 一致）。

## 7.4 端点契约（见 04 §4.7）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/student/applications/simulate` | 为所有未锁定考生重新生成志愿，返回 `{"generated": <总数>}` |
| `GET` | `/admission/results/summary-by-school` | 最近一次运行的各校录取汇总（计划/录取/分数线/满额率），支撑「看各校录取情况」 |

调用链：前端/演示先 `POST /student/applications/simulate` → `POST /admission/run/full` → `GET /admission/results/summary-by-school`。

## 7.5 与录取引擎的衔接（见 03）

- 模拟器**只负责产出志愿**；录取仍由 `AdmissionEngine` 按 03 规则执行（校额到校 → 统招平行志愿）。
- 模拟器产出的志愿结构（batch / priority / highSchoolId）即为引擎 `StudentSnapshot.ApplicationInfo` 的输入，二者契约一致。
- ⚠️ **贯通批次（GUANTONG）为演示占位，本期不录取**：单考生生成器 `GeneratorController`（`POST /student/generator/generate`，见 04 §4.4）会一并返回 `guantongPlan`，但录取引擎（03）只消费 QUOTA+TONGZHAO，`Application.batch` 仅 `QUOTA`/`TONGZHAO`，`guantongPlan` 不落志愿库、不进录取（与 02 §2.2 一致）。

## 7.6 测试（见 06 §6.1）

- `ApplicationSimulatorTest`（单测纯函数，无需 Spring）：
  1. 高分 + 有资格 → 校额对口(层次/距离序) + 统招按 冲→稳→保 拉开档次，普通校自然落入保底；
  2. 无资格 → 无校额志愿，仍有统招；
  3. 低于控制线(430) → 无任何志愿；
  4. 保底档优先普通校（非全重点，例：`chongWenBao_spreadByRank` 中 h3/h5 普通校被纳入）；
  5. 冲档(`REACH`)排在最前（例：465 分考生顺序 `[3,2,1,4,5]`）；
  6. 统招平行志愿不超过 8 个（`tongzhaoNeverExceedsEight`，含 9 校场景验证上限）。

## 7.7 开放问题（待拷问）
1. 区域因子当前用「片区差」近似，是否需要真实经纬度 + haversine 距离？
2. 缺 2025 线学校的回退 `estimateLine`（tier +40 / comp /4）系数是否要随种子规模可调？（仅作回退，主路径用真实 2025 线换算区排名）
3. 前端「各校录取可视化」(05 DRAFT) 是否在本轮补做？

## 7.8 单考生志愿生成器（GeneratorController，本期已实现）
> 对应「手填/逐考生辅助」场景，区别于 7.4 的「全量自动模拟」。代码位置：`student-service/.../controller/GeneratorController.java` + `generator/dto/GenerateRequest.java` + `GenerateResponse.java`。

- 端点：`POST /student/generator/generate`（经网关 `/api/student/generator/generate`，见 04 §4.4）。
- 入参 `GenerateRequest` 关键字段：`commuteLimit`（通勤上限）、`gaokaoTierPref`（高考出口梯队 TOP/HEAD/MID）、`includeCrossDistrict`（跨区投放占位）、`comprehensiveEval`（综合素质评价占位，默认 "B"）、`sprintWeight`/`steadyWeight`/`safetyWeight`（梯度权重，和须=100）、`commutePref`/`gaokaoOutputPref`（偏好权重 0~100），及对比字段 `prev*`（权重联动对比）。
- 返回 `GenerateResponse`：`quotaPlan`/`tongzhaoPlan`/`guantongPlan`（三批次方案，**guantongPlan 为演示占位、不录取**）、`issues`（校验：通勤超限/梯队不符/贯通门槛 380/梯度权重合计≠100 等）、`filtered`（被过滤学校）、`weightComparison`（权重联动对比）。
- ⚠️ 半实现项（本期占位，详见 `08 §5` Q2~Q4）：`gaokaoTierPref`/`includeCrossDistrict`/`comprehensiveEval` 在真实数据流未落地，仅作生成器入参与展示。
