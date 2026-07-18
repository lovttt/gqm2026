# 05 · 前端契约（FRONTEND CONTRACT）

> 约束对象：Vue 前端。技术栈与页面 MUST 对齐本节。

## 5.1 技术栈（MUST）
- Vue 3 + Vite + Pinia（状态）+ Vue Router（路由）+ Element Plus（UI）+ Axios（请求）。
- 入口代理：`vite.config.js` 将 `/api` 代理到 `http://localhost:8080`（网关）。
- 全局请求封装：`utils/request.js` 自动附带 JWT，401 跳登录。

## 5.2 路由与页面（MUST）
| 路径 | 页面 | 角色 | 功能 |
|------|------|------|------|
| `/login` | Login.vue | 匿名 | 登录（admin/student） |
| `/admin` | Admin.vue | ADMIN | 学校/计划/名额/控制线管理、考生管理、触发模拟、结果统计 |
| `/student` | Student.vue | STUDENT | 成绩展示、志愿填报（QUOTA+TONGZHAO）、录取结果查询 |
| `/applications` | Application.vue | ADMIN | 志愿管理：按考生查看/编辑志愿，提交锁校验（见 04 §4.4）|
| `/generator` | Generator.vue | ADMIN/STUDENT | 单考生志愿生成器：参数交互 → 三批次方案 + 校验 + 权重联动对比（见 07 §7.8）|

- 导航守卫：`MUST` 未登录跳 `/login`；角色不符跳对应首页。
- `MUST`：登录后按 `role` 重定向（ADMIN→/admin，STUDENT→/student）。
- ✅ **已裁决（见 `08 §5 Q5`）**：`/generator`（Generator.vue）对 **ADMIN/STUDENT 双开放**，后端不额外加角色限制（维持各服务自校验 JWT），与「前端零改动」原则一致；与「角色不符跳对应首页」守卫不冲突（双角色均允许进入）。

## 5.3 角色视图细节（SHOULD）
### 管理员后台（Admin.vue）
- 高中/初中校/校额名额/控制线 增删改查。
- 考生列表与成绩查看；考生管理页 `SHOULD` 提供「按名额重算校额资格」按钮（调 `/student/quota-eligibility/recompute`，见 02 §2.6、04 §4.4），重算后刷新「校额资格」列。
- 模拟控制：「一键模拟」「仅校额」「仅统招」按钮 → 调 `/run/*`。
- 结果区：`/stats` 统计卡 + `/results` 表格。
- `MAY`：导入导出按钮（调 `/school/import`、`/student/import`）。

### 考生端（Student.vue）
- 展示本人成绩与校额资格。
- 志愿填报：QUOTA 志愿 + TONGZHAO 志愿（1~8）整页保存。
- 录取结果：调 `/results/student/{id}` 展示批次/高中/说明。

## 5.4 与后端契约映射（MUST）
- 所有请求路径 `MUST` 以 `/api/{service}` 开头（经网关）。
- 角色判断 `MUST` 基于登录返回的 `role` 字段与 Pinia `auth` store。

## 5.5 开放问题
1. ✅ **已定并实现**：录取结果可视化已在 `Admin.vue` 新增「各校录取可视化」Tab——基于 `GET /admission/results/summary-by-school`，用纯 CSS 柱状图展示「各校录取人数（校额+统招堆叠）」与「两批次占比（全部高中合计）」，并列各校明细表（计划/录取/满额率）。无第三方图表依赖。
2. 考生端是否需要「志愿保存后冻结/提交」二次确认？
3. 是否需要多考生切换的演示视图（管理员代查某考生）？
