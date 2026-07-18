# 03 · 录取算法约束（ADMISSION ALGORITHM）

> 约束对象：admission-service 引擎。本文件是录取正确性的唯一权威定义。

## 3.1 总体顺序（MUST）
```
1) 校额到校批次（QUOTA）—— 先于统招
2) 统一招生批次（TONGZHAO）—— 平行志愿
```
- 被校额到校 `ADMITTED` 的考生 `MUST` 从统招候选池中剔除（一次投档、不得重复录取）。
- 引擎 `runFull()` `MUST` 先 `runQuota` 再 `runTongzhao` 并复用豁免集合。

## 3.2 校额到校批次规则（MUST，含校内排名限制）
候选资格（全部满足才进入分组）：
1. `Student.hasQuotaEligibility == true`（**该资格已结合初中校名额总数取前 N 名**，判定规则见 02 §2.6）
2. `Student.totalScore >= ControlLine(type=QUOTA).value`（全区最低控制线，510 量纲下为 430）
3. 该考生填报了某高中的 `QUOTA` 志愿

> 说明：`hasQuotaEligibility` 在数据层已按「初中校名额总数 N + 校内排名前 N」派生（02 §2.6），因此进入本批次的都是各初中校校额入围者。**分段聚合（共享名额池）**：本期（Q9 裁决）校额分组为硬编码配置 `quotaGroups`，同一组内的多所初中校（如「一中/五中分」）共用一个校额名额池、按高中聚合；引擎以组内 canonical id（`groupKeyOf`）替代单 `(juniorSchoolId)` 进行分组与名额汇总，故「校内排名」实为「共享组内排名」。分组口径以 `quotaGroups` 配置为准，而非字面 `(juniorSchoolId, highSchoolId)`。

分组与录取（共享名额池口径，对齐 Q9 硬编码分组）：
- 按 `quotaGroups` 共享组聚合：同一组内多所初中校共用一个校额名额池，按 `(共享组 id, highSchoolId)` 汇总 `QuotaSeat.quota`，**仅限配额表 `QuotaSeat` 中存在的组合**。
- 组内 `MUST` 使用 `TieBreakComparator` 降序排序（即「共享组内分数排名」）；`rank` 从 1 起递增，写入 `AdmissionResult.schoolRank`。
- 每组取前 `QuotaSeat.quota` 名录取（`ADMITTED`），其余同组考生进入统招池。
- `note` `MUST` 记录「共享组内分数排名第 N」（如 `校额到校第{priority}志愿录取（共享组内分数排名第{rank}）`）。

✅ **已定（拷问结论）**：校额到校**不按志愿序号择优**，仅判定「是否填报了该校」。同一考生对同一高中在 QUOTA 有多条志愿，仅按「是否填报」判定一次，不重复计入分组。

## 3.3 统一招生批次规则（MUST，平行志愿）
- 候选：`exempt`（已被校额到校录取）之外的全部考生。
- 排序：全局按 `TieBreakComparator` 降序（**分数优先**），即分数最高者最先投档。
- 投档（**平行志愿：逐个考生处理完其全部志愿，再看下一个**）：
  - 对当前考生，按 `priority` 升序遍历其 `TONGZHAO` 志愿（**遵循志愿**）：若目标高中 `tongzhaoQuota` 剩余 > 0，则录取并扣减计划，**立即 break**——本次投档只看这一个考生，**处理完该考生的全部志愿（取到首个有余额学校即止）才轮到下一个分数更低的考生**。
  - 若所有志愿均无余额，记录 `NOT_ADMITTED`（滑档）。
- `MUST` 保证每位考生至多被一所高中统招录取。
- ⚠️ **非顺序志愿（拷问结论 ✅）**：引擎**不是**「先录完所有考生的一志愿、再录二志愿」。而是「按分数从高到低，每位考生一次性遍历自己的全部志愿、取首个有余额学校」，然后才处理下一位考生。**填在 2/3 志愿不会被罚到队尾**（区别于顺序志愿）；滑档即 `NOT_ADMITTED`，无补录批次。
- 对应实现：`AdmissionEngine.runTongzhao`（候选按比较器排序后 `for` 考生、`for` 该考生志愿、`break` 即止）。

## 3.4 同分比较器（MUST，可配置）
`TieBreakComparator.STEP` 定义比较链，方向均为「数值大者优先」（`reversed()`）。当前顺序（量纲见 02 §2.3）：
1. totalScore（总分，满分 510）
2. chinese+math+english（语数外三科总分，满分 300）
3. chinese（语文，100）
4. math（数学，100）
5. english（外语，100）
6. physics+politics（物理+道法两科总分，满分 160）
7. physics（物理，80）
8. politics（道德与法治，80）
9. pe（体育，50）
10. ticketNo（准考证号，自然序兜底，**保证比较器为全序/确定性**）

- `MUST`：调整成绩规则只改 `STEP` 列表，不重写比较逻辑；第 10 级 `ticketNo` 兜底为确定性必需，**不得删除**（见 3.7-3）。
- 比较链与真实北京规则一致（语数外→语→数→外→物道→物→道→体）。**末级是否纳入"综合素质评价"本期不纳入**。
- 因体育/道法/物理附加/英语听力基本满分，**实际区分主要发生在语文、数学、物理笔试、英语笔试、道法笔试**，比较器天然体现此优先级。

## 3.5 算法不变量（Invariants，MUST）
1. 每名考生录取结果全局唯一（校额到校 XOR 统招，不重复）。
2. 任一高中统招录取人数 `MUST` ≤ `tongzhaoQuota`。
3. 任一 `(junior, high)` 校额录取人数 `MUST` ≤ `QuotaSeat.quota`。
4. 总分 < 控制线者绝不得校额录取。
5. 每次运行 `MUST` 生成新的 `runId`（`findLatestRunId()+1`），结果按 `runId` 隔离追加，**不允许跨运行相互覆盖**（历史快照，见 3.8）；默认 `/results`、`/stats` 仅返回最新 `runId`，`/runs` 可对比多次模拟。

## 3.6 运行模式（SHOULD）
- `POST /run/full`：一键顺序跑完（生成新 runId，追加结果）。
- `POST /run/quota`：仅校额到校（生成新 runId，追加 QUOTA 结果）。
- `POST /run/tongzhao`：仅统招（以「最新一次运行」的 QUOTA 结果为豁免，生成新 runId 追加）。
- `GET /stats`、`GET /results`：只读最新运行，不重跑。
- `GET /runs`、`GET /runs/{runId}`：历史模拟对比（见 3.8）。

## 3.7 开放问题（已全部拷定 ✅）
1. ✅ **已定**：统招滑档考生**不进入补录/下一批次**，直接 `NOT_ADMITTED`（本期仅两批次）。
2. ✅ **已定**：校额到校**不按多序号志愿择优**，仅看「是否填报该校」（见 3.2）。
3. ✅ **已定**：同分末级**显式按 `ticketNo` 兜底**（第 10 级），保证比较器为全序、任意两考生必有确定先后（见 3.4）。

## 3.8 历史模拟快照（MUST，本期补齐）
- 每次 `run/full`、`run/quota`、`run/tongzhao` 均生成新 `runId`，录取结果写入 `admission_result(run_id, run_at)`。
- `run/tongzhao` 豁免集取「最新一次运行」的校额录取，避免跨运行污染。
- 查询默认返回最新 `runId`；`GET /runs` 列出全部历史运行及统计，`GET /runs/{runId}` 查看指定运行结果。
- 种子重建（删除 SQLite 文件）会清空全部历史。
