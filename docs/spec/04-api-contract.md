# 04 · API 契约（API CONTRACT）

> 约束对象：所有 HTTP 端点。前端与后端 MUST 以此为准。
> 统一前缀：`http://<host>:8080/api/{service}/**`，网关 `StripPrefix=1` 后转发到服务根。

## 4.1 通用约定（MUST）
- 鉴权：`Authorization: Bearer <jwt>`（除 `/api/auth/login`、`/api/auth/register`）。
- 内容类型：`application/json`。
- 时间：ISO-8601。
- 错误：提交锁冲突返回 `400`（BAD_REQUEST）、资源不存在返回 `404`（均经 `ResponseStatusException`，见 04 §4.4 / 06）；业务异常经 `@RestControllerAdvice` 统一返回 `400/404/500` + 结构化错误体 **`{message}`**（见 09 §4）。统一错误体 **已定为 `{message}`**（不含 `code`），与前端契约一致（见 4.7-2 / 08-Q8）。

## 4.2 Auth 服务（`/api/auth`）
| 方法 | 路径 | 鉴权 | 说明 | 请求/响应 |
|------|------|------|------|-----------|
| POST | `/login` | 否 | 登录换 token | req `{username,password}` → `{token,username,role,studentId}` |
| POST | `/register` | 否 | 注册用户 | req `{username,password,role,studentId?}` → User |
| GET | `/users` | 是 | 用户列表（分页，见 4.6） | → `Page<User>` |

- `role` 取值：`ADMIN`、`STUDENT`（注册默认 STUDENT）。

## 4.3 School 服务（`/api/school`）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST/PUT/DELETE | `/high-schools` | 高中 CRUD（GET 分页，见 4.6） |
| GET/POST/PUT/DELETE | `/junior-schools` | 初中校 CRUD（GET 分页，见 4.6） |
| GET/POST/PUT/DELETE | `/quota-seats` | 校额名额 CRUD（GET 分页，见 4.6） |
| GET/POST | `/control-line` | 控制线读取/upsert（注意：单数路径，非 `/control-lines`；见 `06 §6.1 ControlLineControllerTest`）|
| GET | `/export` | 整体导出 `SchoolDataset`（供引擎拉取）；含 `highSchools/juniorSchools/quotaSeats/controlLine/scoreLines` + **`scoreSegments`（2025/2026 一分一段表，用于志愿模拟换算区排名）** + `quotaGroups`（校额名额共享分组，见 `school-service/.../config/QuotaGroupConfig.java`）|
| POST | `/import` | 整体导入 `SchoolDataset` |

## 4.4 Student 服务（`/api/student`）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST/PUT/DELETE | `/students` | 考生 CRUD（GET 分页，见 4.6） |
| GET/POST/PUT/DELETE | `/applications` | 志愿 CRUD（GET 分页，见 4.6） |
| GET | `/students/junior/{juniorSchoolId}` | 某初中校考生列表（分页，见 4.6） |
| POST | `/applications/student/{studentId}` | 整页保存某考生志愿（先删后写，提交锁生效；注意路径非 `/applications/batch`）|
| POST | `/generator/generate` | 单考生志愿生成器：参数见 `GenerateRequest`（通勤上限/高考出口梯队/跨区/综合素质/梯度权重/偏好权重），返回三批次方案 + 校验 issues + 过滤信息 + 权重联动对比（见 07 §7.8）|
| POST | `/generate?perClass=40` | 按初中校班数生成考生（先清空考生与志愿再生成，见 02 §2.4）|
| POST | `/students/{id}/submit` | 提交志愿（锁定，之后不可增删改） |
| POST | `/students/{id}/reopen` | 撤回提交（重新开放编辑） |
| POST | `/applications/simulate` | 按「理性考生」策略为所有未锁定考生重新生成志愿 → `{"generated": <总数>}`（见 07） |
| POST | `/quota-eligibility/recompute` | 结合各初中校名额总数重算校额资格（按初中校取前 N 名，见 02 §2.6）→ `{"eligibleTotal":<具资格人数>,"studentTotal":<考生总数>,"byJunior":[{"juniorSchoolId":..,"quotaTotal":N,"eligible":..}]}` |
| GET | `/export` | 整体导出 `StudentDataset`（供引擎拉取） |
| POST | `/import` | 整体导入 `StudentDataset` |

- `Student.submitted == true` 时，对 `/applications` 的 POST/PUT/DELETE、`/applications/student/{id}` 整页保存均返回 `400`（提交锁，见 02 §2.5-3）。

## 4.5 Admission 服务（`/api/admission`）
| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/run/full` | 是 | 一键顺序模拟 → stats（新 runId） |
| POST | `/run/quota` | 是 | 仅校额到校 → stats（新 runId） |
| POST | `/run/tongzhao` | 是 | 仅统招 → stats（新 runId） |
| GET | `/results` | 是 | 最新运行录取结果（分页，见 4.6） |
| GET | `/results/student/{studentId}` | 是 | 某考生最新运行结果（单考生有界，不分页） |
| GET | `/stats` | 是 | 只读统计最新运行 `{runId,total,admitted,notAdmitted,quotaAdmitted,tongzhaoAdmitted}` |
| GET | `/runs` | 是 | 全部历史模拟运行列表 + 每轮统计（派生汇总，不分页） |
| GET | `/runs/{runId}` | 是 | 指定运行的录取结果（派生汇总，不分页） |
| GET | `/results/summary-by-school` | 是 | 最近一次运行的各校录取汇总：计划/录取/分数线/满额率（见 07 §7.4，聚合，不分页） |

- `AdmissionResult` 字段含 `schoolRank`（共享组内排名）：仅 `batch=QUOTA` 且 `ADMITTED` 时有效，表示「同一 `quotaGroups` 共享组内（如 一中/五中分）竞争同一高中校额」的名次（非单所初中校内排名）；统招与未录取为 `null`。`GET /results`、`/results/student/{id}` 均返回该字段。
- 可视化端点 `GET /results/summary-by-school` 返回每校 `{tongzhaoPlan, quotaPlan, tongzhaoAdmitted, quotaAdmitted, admitted, minScore, maxScore, fillRate}`，供前端「各校录取可视化」使用（见 05 §5.5）。
- ✅ `AdmissionResult` 反规范化新增「来源初中」与「各科得分」：`juniorSchoolId` / `juniorSchoolName`（来自 `SchoolSnapshot.juniorSchools`）、`chinese` / `math` / `english` / `physics` / `politics` / `pe`（来自 `StudentSnapshot.StudentInfo`）。`runQuota` 与 `runTongzhao` 在保存结果时一并写入，使 `GET /results`、`/results/student/{id}` 直接返回考生初中校与各科成绩，前端结果页无需回查 student-service。
- ✅ `GET /results` 支持可选过滤条件（均 `required=false`，仅返回最新一次运行）：`juniorSchoolId`（毕业学校）、`highSchoolId`（录取学校）、`minScore`/`maxScore`（总分范围，闭区间）、`status`（`ADMITTED`/`NOT_ADMITTED`）、`studentName`（考生姓名模糊匹配 `LIKE %x%`）。后端用 JPA `Specification` 组合可选条件，前端 Admin「模拟录取」Tab 提供对应筛选栏（含查询/重置）。

## 4.6 列表分页（MUST）
开放问题原 4.6-1「列表端点是否需要分页/排序」已定为 **需要分页**：所有集合型列表端点 MUST 支持分页与排序，禁止无界返回全量（防止数据增长后 OOM / 前端卡顿）。错误响应结构化（原 4.6-3）仍为开放问题，见 4.7。

### 请求（查询参数，由 Spring `Pageable` 自动解析）
- `page`：页码，**0-based**，默认 `0`。
- `size`：每页条数，默认 `20`。
- `sort`：排序字段与方向，形如 `sort=totalScore,desc`（可重复多次）；字段须为实体持久化属性。

### 响应（统一信封，Spring `Page<T>` 序列化；前端 MUST 读取 `content` 数组）
```json
{
  "content": [ ... ],
  "totalElements": 123,
  "totalPages": 7,
  "number": 0,
  "size": 20,
  "numberOfElements": 20,
  "first": true,
  "last": false,
  "empty": false
}
```

### 适用端点（MUST 分页）
| 服务 | 端点 |
|------|------|
| auth | `GET /auth/users` |
| school | `GET /school/high-schools`、`GET /school/junior-schools`、`GET /school/quota-seats` |
| student | `GET /student/students`、`GET /student/students/junior/{juniorSchoolId}`、`GET /student/applications` |
| admission | `GET /admission/results` |

### 不适用（派生/聚合汇总，数据量有界，保持全量数组返回）
`GET /admission/runs`、`/admission/runs/{runId}`、`/admission/results/student/{id}`、`/admission/results/summary-by-school`。

## 4.7 开放问题
1. ✅ **已定**：历史模拟对比端点 `/runs`、`/runs/{runId}` 已提供（见 3.8）；「重置模拟」仍通过删除 SQLite 文件实现。
2. ✅ **已定（见 08-Q8 / 09 §4）**：错误响应统一为结构化体 **`{message}`**（不含 `code`），由四业务服务的 `@RestControllerAdvice` + `DomainException` 统一返回；成功响应直通不包装。
