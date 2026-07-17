# GQM2026 · 北京中考东城区志愿填报与录取模拟系统

[![CI](https://github.com/lovttt/gqm2026/actions/workflows/ci.yml/badge.svg)](https://github.com/lovttt/gqm2026/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-17-orange)
![Vue](https://img.shields.io/badge/Vue-3-42b883)

面向**北京中考东城区**的志愿填报辅助与录取模拟系统。后端为 Spring Boot 微服务，前端为 Vue 3 单页应用，数据使用内嵌 SQLite，开箱即用、无需外部中间件。

核心特性：

- **志愿生成器（Generator）**：输入通勤上限、高考出口梯队、跨区投放、综合素质评价、梯度权重、偏好权重等参数，按校额到校 / 统招 / 贯通三批次生成志愿方案，并对通勤超限、梯队不符、贯通门槛、梯度权重合计等做实时校验与联动对比。
- **录取模拟**：校额到校（控制线 + 校内排名）、统招平行志愿兜底、贯通批次，支持一键全量模拟并输出统计。
- **多角色视图**：考生端、管理端、学校端。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3.2.5 · Spring Cloud 2023.0.1 · SQLite（sqlite-jdbc） |
| 前端 | Vue 3.4 · Vite 5 · Element Plus 2.7 · Pinia · Vue Router 4 · Axios |
| 构建 | Maven（多模块）· npm |

## 目录结构

```
gqm2026/
├── backend/                 # Spring Boot 微服务（Maven 多模块）
│   ├── gateway/             # 网关（端口 8080，统一 /api 入口）
│   ├── auth-service/        # 认证与鉴权
│   ├── school-service/      # 学校 / 招生计划数据
│   ├── student-service/     # 考生 / 志愿 / 志愿生成器
│   ├── admission-service/   # 录取与模拟
│   └── start-all.ps1        # 一键构建 + 启动 + 模拟脚本
├── frontend/                # Vue 3 + Vite 前端
│   └── src/views/Generator.vue  # 志愿生成器页面
└── docs/                    # 规范与契约（Doc-First）
    ├── CONSTITUTION.md      # 工程宪法（文档先于代码）
    └── spec/                # 各模块 spec
```

## 环境要求

- JDK 17+
- Node.js 18+（建议 20）
- Maven 3.8+（后端构建会自动下载依赖）

## 快速开始

### 1. 启动后端

```powershell
cd backend
# 方式 A：一键构建并启动全部 5 个服务（含 5729 考生种子），约 2 分钟
powershell -ExecutionPolicy Bypass -File start-all.ps1

# 方式 B：手动构建各模块 jar 后分别启动
mvn package -DskipTests
java -jar gateway/target/gateway-1.0.0.jar
java -jar auth-service/target/auth-service-1.0.0.jar
java -jar school-service/target/school-service-1.0.0.jar
java -jar student-service/target/student-service-1.0.0.jar
java -jar admission-service/target/admission-service-1.0.0.jar
```

网关监听 `http://localhost:8080`，所有接口以 `/api` 为前缀。

默认管理员账号：

- 用户名：`admin`
- 密码：`admin123`

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。开发服务器通过 Vite 代理将 `/api` 转发到 `http://localhost:8080`，无需额外配置跨域。

### 3. 使用志愿生成器

登录后在考生端点击「**志愿生成器**」进入 `/generator`，填写参数后点击「生成」，即可查看三批次方案、校验问题与权重联动对比。管理员无关联考生时可在页面顶部选择考生。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录，返回 JWT（`{username, password}`） |
| POST | `/api/student/generator/generate` | 单考生志愿生成（参数 + 校验 + 三批次方案） |
| POST | `/api/student/applications/simulate` | 一键模拟志愿填报 |
| POST | `/api/admission/run/full` | 全量录取模拟 |
| GET  | `/api/admission/stats` | 录取统计 |

## 开发与测试

```bash
# 后端：运行单元测试（含志愿生成器逻辑）
cd backend && mvn test

# 前端：类型/构建校验
cd frontend && npm run build
```

CI（`.github/workflows/ci.yml`）在每次 push / PR 到 `master` 时自动执行：后端 `mvn package`（编译打包 + 运行单元测试，作为质量门禁）、前端 `npm install && npm run build`。测试不通过即 CI 失败。

## 工程约定

本项目遵循 `docs/CONSTITUTION.md` 的 **Doc-First（文档先于代码）** 原则：任何对系统行为、数据模型、API、算法的改动，需先更新 `docs/spec/` 下的契约文档，再改代码，并保持二者一致。
