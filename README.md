



# 青云 e —— AI 赋能校园事务智理与职业成长智辅平台

## 📖 项目概述
本项目旨在构建面向高校班级的智能信息管理平台，借助 NapCat 框架实现多班级群消息的自动接收与处理，并结合 AI 技术完成消息归纳、个人画像生成与简历诊断。系统致力于解决高校班级管理中信息过载、效率低下、数据分散及求职困难等痛点。

## 🏗️ 目录结构与模块说明
本项目采用单体仓库（Monorepo）方式进行统一管理，包含四个核心子系统[cite: 1]：

```text
qingyun-e-system/
├── backend-api/       # Java 后端服务：提供 RESTful API，处理业务与 AI 对接
├── frontend-web/      # Web 管理端：Vue 3 + Element Plus 面向班委与管理员
├── frontend-mini/     # 微信小程序端：面向在校大学生的使用主入口
└── napcat-service/    # 消息服务：基于 NapCat 框架的群聊消息采集服务
```

## 🚀 新人指南：首次连接与本地配置

新加入团队的开发者，请按以下步骤完成仓库接入和环境初始化：

**1. 克隆代码到本地**

Bash

```
git clone [https://github.com/mfyang8-gif/qingyun-e-system.git](https://github.com/mfyang8-gif/qingyun-e-system.git)
cd qingyun-e-system
```

**2. 配置本地 Git 身份标识**

请务必配置你自己的用户名和邮箱，以便于在提交历史中追踪代码归属：

Bash

```
git config user.name "你的名字或拼音"
git config user.email "你的邮箱地址"
```

**3. 安装基础依赖**

- **后端**：确保本地已安装 JDK 17+ 和 Maven。
- **前端/小程序/NapCat**：确保本地已安装 Node.js (v16+) 和 npm/pnpm，并在各自目录下执行 `npm install`。

## 🌿 Git 协作与分支规范（重点）

在多人开发 Monorepo 时，**严禁直接在 `main` 或 `dev` 分支上直接写代码和提交**。请严格按照以下流程工作。

### 1. 分支命名规范

- `main`：生产环境主分支，仅用于线上发布。
- `dev`：开发环境集成分支，汇总所有人的功能代码。
- `feat/*`：新功能分支，命名格式为 `feat/端名称-功能名-你的拼音`。
  - *后端功能示例*：`feat/backend-auth-zhangsan`（张三开发的后端登录功能）
  - *前端功能示例*：`feat/web-dashboard-lisi`（李四开发的 Web 端数据看板）
- `fix/*`：Bug 修复分支，示例：`fix/mini-login-bug`。

### 2. 独立开发工作流（如何只提交自己的分支）

每天开始写代码前，请按照此流程操作，确保你的代码不干扰别人，也不被别人干扰：

Bash

```
# 1. 确保在 dev 分支，并拉取团队最新代码
git checkout dev
git pull origin dev

# 2. 从最新的 dev 分支切出你自己的独立功能分支
git checkout -b feat/backend-ai-summary-yourname

# --- (在你的编辑器里愉快地写代码) ---

# 3. 提交你自己的修改（确保不要改动与你无关的模块）
git add .
# 请参考下方的【提交信息规范】填写
git commit -m "feat(backend): 完成 AI 消息归纳接口开发"

# 4. 把你自己的分支推送到远端仓库
git push -u origin feat/backend-ai-summary-yourname
```

**推送完成后**：登录 GitHub 页面，点击 `Compare & pull request`，请求将你的 `feat` 分支合并到 `dev` 分支，由技术负责人 Code Review 后合并。



### 3. Commit 提交信息规范

提交信息必须带有前缀，以说明此次提交的目的：

- `feat:` 新增功能（feature）
- `fix:` 修复 bug
- `docs:` 仅修改了文档，比如 README、修改 API 文档等
- `style:` 代码格式修改（不影响代码运行的变动，如空格、格式化）
- `refactor:` 代码重构（即不是新增功能，也不是修改 bug 的代码变动）
- `chore:` 构建过程或辅助工具的变动（如更新依赖、修改构建脚本）

## 💻 项目开发规范

### 1. 后端规范 (Java / Spring Boot)

- **架构分层**：严格遵循 `Controller` -> `Service` -> `Mapper` 三层架构。业务逻辑必须在 Service 层处理。
- **接口规范**：统一使用 RESTful 风格。所有 API 返回结果必须统一包装为 `Result<T>` 结构（包含 code、message、data）。
- **API 文档**：使用 Swagger 或 Knife4j 注解（`@Tag`, `@Operation`），新写接口必须附带明确的参数和返回值说明。
- **安全规范**：严禁在代码中硬编码数据库密码、AI API 密钥等敏感信息，必须通过 `application-dev.yml` 注入，且该文件已加入 `.gitignore`。

### 2. Web 管理端规范 (Vue 3)

- **代码风格**：使用 Alibaba Java Coding Guidelines（jbr插件） 进行统一格式化。提交前请确保没有 Alibaba Java Coding Guidelines 报错。
- **组件化**：复用率高的 UI 模块（如表格、弹窗）必须抽离为独立组件放于 `src/components`。
- **网络请求**：所有的 Axios 请求必须在 `src/api` 目录下统一封装，禁止在 Vue 页面组件内直接写死接口 URL。

### 3. 微信小程序端规范

- **包体积控制**：严格控制主包大小。非首页核心功能（如简历生成、画像查看）必须使用 **分包加载（Subpackages）** 机制。
- **UI 标准**：优先使用成熟组件库（如 uView 或原生 WeUI），保持全端设计风格统一。
- **状态管理**：合理使用状态管理（Vuex / Pinia），跨页面共享数据避免使用 url 传参导致长度超限。

### 4. 消息服务规范 (NapCat)

- **配置隔离**：主备 QQ 账号的密码及 Session 凭证绝对不允许提交到 Git，环境配置必须留在本地[cite: 1]。
- **异常处理**：需严格实现掉线重连机制，避免因单点故障导致消息丢失[cite: 1]。

## 📚 附录与文档指引

- 产品需求与业务逻辑：[需求分析.md](./docs/青云 e——AI 赋能校园事务智理与职业成长智辅小程序 需求分析.md)
- 数据库表结构设计：参阅 `docs/schema.sql` (待定)

