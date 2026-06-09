# 经典开源项目文档组织模式研究

> 研究时间: 2026-06-09
> 目标: 为 eyelib 多模块 Minecraft Forge 渲染库项目改进文档结构提供参考

---

## 1. PostgreSQL 源代码文档组织

### 结构摘要

PostgreSQL 的官方文档采用 **DocBook SGML/XML** 格式，集中存储在 `doc/src/sgml/` 目录下。整个文档被组织为 **6 个独立 Book/Part**，面向不同读者群体：

```
doc/src/sgml/
├── postgres.sgml          # 根文档，通过 SGML ENTITY 引用所有其他文件
├── tutorial.sgml          # Part I:  教程 - 面向新手
├── sql.sgml               # Part II: SQL 语言参考
├── admin.sgml             # Part III: 服务器管理
├── libpq.sgml             # (客户端接口，后来拆分出去)
├── reference.sgml         # Part IV: 客户端接口
├── ... 约 200+ .sgml 碎片文件
├── config.sgml           # 配置参数文档
├── catalogs.sgml          # 系统目录表文档
├── func.sgml             # 函数和运算符
├── indices.sgml          # 索引相关
├── Makefile               # 构建规则（make html / make pdf）
└── stylesheet/           # DSSSL/XSLT 样式表
```

### 组织原则

| 层级 | 内容 | 目标读者 |
|------|------|----------|
| **Part I - Tutorial** | 入门教程（创建表、查询、高级功能） | 新用户 |
| **Part II - SQL Language** | SQL 语法、数据类型、函数、索引等 | 所有用户 |
| **Part III - Server Administration** | 安装、配置、角色管理、备份恢复 | DBA/运维 |
| **Part IV - Client Interfaces** | libpq, psql, ecpg 等客户端工具 | 开发者 |
| **Part V - Server Programming** | PL/pgSQL, 触发器, 规则, 扩展开发 | 开发者 |
| **Part VI - Reference** | SQL 命令、系统目录、pg_config 等 | 所有用户 |

### 关键技术特征

1. **单一源 + 多输出**：一份 DocBook 源码同时生成 HTML (分页/单页)、PDF、man page
2. **碎片化 + ENTITY 链接**：大型文件（如 `catalogs.sgml` 10452行）通过 SGML ENTITY 机制组合
3. **源码内文档（内嵌注释）**：函数和类型的结构化注释被提取到文档中（`kernel-doc` 风格）
4. **独立构建系统**：文档有独立的 `Makefile`，与源代码构建解耦

### 可借鉴点

- **按读者角色划分 Part（用户/DBA/开发者）** → eyelib 可按"使用者/贡献者/模块开发"分层
- **单一源多格式输出** → 减少维护负担
- **ENTITY 碎片化** → 避免单个文档过长（但 eyelib 可简化）

---

## 2. Linux kernel Documentation/

### 结构摘要

Linux 内核文档位于源码树顶层的 `Documentation/` 目录，采用 **Sphinx + reStructuredText** 格式。主索引文件 `Documentation/index.rst` 通过 `toctree` 组织所有子章节：

```
Documentation/
├── index.rst               # 顶层 toctree，指向所有主要章节
├── process/                # 内核开发流程（提交补丁、编码风格、许可证）
│   ├── index.rst
│   ├── submitting-patches.rst
│   ├── coding-style.rst
│   └── ...
├── admin-guide/            # 系统管理员指南
│   ├── index.rst
│   ├── kernel-parameters.rst
│   └── ...
├── core-api/               # 核心 API 手册（给内核开发者）
│   ├── index.rst
│   ├── kernel-api.rst      # 批量包含 kernel-doc 注释
│   └── ...
├── driver-api/             # 驱动开发 API
│   ├── index.rst
│   └── ...
├── subsystem-apis/         # 子系统 API 索引
├── locking/                # 锁机制
├── security/               # 安全子系统
├── filesystems/            # 文件系统
├── networking/             # 网络
├── doc-guide/              # 如何写内核文档（元文档）
└── ... (约 50+ 个子目录)
```

### 组织原则

| 目录 | 内容 | 目标读者 |
|------|------|----------|
| `process/` | 开发流程、编码风格、提交指南 | **贡献者** |
| `admin-guide/` | 内核参数、硬件配置、应急处理 | **运维/管理员** |
| `core-api/` | 内核核心 API、数据结构 | **内核开发者** |
| `driver-api/` | 驱动开发 | **驱动开发者** |
| `subsystem-apis/` | 各子系统 API | **子系统开发者** |
| `doc-guide/` | 文档编写指南 | **文档贡献者** |

### 关键技术特征

1. **Sphinx toctree 分层**：每个子目录有自己的 `index.rst`，通过 `toctree` 形成树状结构
2. **kernel-doc 注释**：C 源码中的 `/** ... */` 注释被自动提取到文档中（类似 JavaDoc）
3. **RST 轻量标记**：比 DocBook 简单得多，降低文档编写门槛
4. **按读者角色分目录**：明确区分 admin、core、driver 等不同目标群体
5. **元文档**：`doc-guide/` 本身就是文档编写指南，自解释性强

### 可借鉴点

- **Sphinx toctree 树状组织** → eyelib 可以用类似方式组织子模块文档
- **按目标读者分目录** → 区分"用户文档"和"开发者文档"
- **kernel-doc / rustdoc 模式** → 从 Java 源码中提取注释生成 API 文档
- **元文档 (文档的文档)** → 解释文档结构和编写规范

---

## 3. Kubernetes Docs

### 结构摘要

Kubernetes 官方文档采用 **Hugo 静态网站** 生成，源文件位于 `kubernetes/website/content/` 目录。其最大特色是按 **内容类型** 而非模块/组件组织：

```
content/en/docs/
├── _index.md                # 文档首页
├── concepts/                # 概念 — 理解系统
│   ├── _index.md
│   ├── architecture/        # 架构（节点、控制平面）
│   ├── containers/          # 容器概念
│   ├── workloads/           # Pods, Deployments, Jobs
│   ├── services-networking/ # 服务发现、网络
│   ├── storage/             # 存储
│   └── ...
├── tasks/                   # 任务 — 如何完成特定操作
│   ├── _index.md
│   ├── configure-pod-container/
│   ├── manage-kubernetes-objects/
│   ├── inject-data-application/
│   ├── run-applications/
│   └── ...
├── tutorials/               # 教程 — 端到端引导
│   ├── _index.md
│   ├── hello-minikube/
│   ├── stateful-application/
│   └── ...
├── reference/               # 参考 — CLI、API、配置表
│   ├── _index.md
│   ├── kubectl/
│   ├── kubernetes-api/
│   └── ...
├── setup/                   # 安装/部署
│   ├── _index.md
│   ├── production-environment/
│   └── ...
└── contribute/              # 如何贡献文档
    └── ...
```

### 组织原则 — DIKW 金字塔模型

| 层级 | 类型 | 目的 | 例 |
|------|------|------|----|
| **Concepts** | 概念 | 理解"是什么"和"为什么" | 什么是 Pod |
| **Tasks** | 任务 | 完成"怎么做" | 如何创建一个 Deployment |
| **Tutorials** | 教程 | 端到端学习路径 | 完整的留言板应用 |
| **Reference** | 参考 | 精确的技术规格 | `kubectl run` 命令语法 |

这种分类来自 **Diátaxis 框架**（也称为"内容类型驱动设计"），已被 Kubernetes、Django、Arch Linux 等多个项目采用。

### 关键技术特征

1. **内容类型优先于组件结构**：不论讲哪个组件，先确定是"概念"还是"任务"
2. **每个页面有明确的类型标签**：用户一眼就知道该读什么
3. **SEO/导航友好**：左侧导航按类型和主题嵌套
4. **I18n 支持**：`content/en/`、`content/zh/` 等多语言
5. **可测试的文档**：文档中的命令示例通过 CI 测试

### 可借鉴点

- **Diátaxis 内容类型分类** → eyelib 文档可以按"概念/任务/参考/教程"分层
- **概念与任务分离** → AGENTS.md 和 SKILL.md 中的"概念知识"和"操作步骤"应分开
- **Reference 独立** → `MODULES.md` 可作为 Reference 类型的导航索引
- **避免按模块目录组织** → 用户是按"想做什么"而非"属于哪个模块"来查阅

---

## 4. Rust 语言项目文档惯例

### 结构摘要

Rust 生态的文档是一个 **分层文档套件**，每个组件都有明确的职责和受众：

```
doc.rust-lang.org/
├── book/                    # "The Book" — 入门教程式教材
│   ├── ch01-00-getting-started.md
│   ├── ch02-00-guessing-game.md
│   └── ... (20 chapters, 循序渐进的阅读式学习)
├── reference/               # "The Rust Reference" — 正式语言规范
│   ├── introduction.md
│   ├── notation.md
│   ├── lexical-structure.md
│   ├── types.md
│   ├── expressions.md
│   └── ... (精确但枯燥的技术参考)
├── rustdoc/                 # rustdoc Book — 文档工具的使用指南
├── cargo/                   # Cargo Book — 包管理器手册
├── edition-guide/           # Edition Guide — 版本迁移指南
├── nomicon/                 # "The Rustonomicon" — 不安全代码高级主题
├── unstable-book/           # Unstable Book — 不稳定功能参考
├── rustc/                   # rustc Book — 编译器手册
└── embedded-book/           # Embedded Rust Book — 嵌入式开发
```

### 组织原则 — 按知识深度和用途分层

| 文档 | 深度 | 用途 | 读者 |
|------|------|------|------|
| **The Book** | 入门 | 系统化学习、循序渐进 | 新手 |
| **Reference** | 精确 | 语言特性的正式定义 | 所有开发者 |
| **Rustdoc Book** | 工具 | 教你如何使用文档工具 | 库作者 |
| **Cargo Book** | 工具 | 构建和依赖管理 | 所有用户 |
| **Edition Guide** | 迁移 | 版本间的变化 | 升级者 |
| **Rustonomicon** | 高级 | 不安全代码、未定义行为 | 系统程序员 |
| **Unstable Book** | 预览 | 尚未稳定的功能 | 冒险者 |

### 关键技术特征

1. **与生俱来的文档文化**：`rustdoc` 是 Rust 工具链的一等公民
2. **"Doc as Code"**：文档注释（`///` 和 `//!`）是第一级文档
3. **文档测试**：示例代码在 `cargo test` 中自动运行
4. **分层明确**：入门、参考、高级、工具手册各自独立
5. **按上下文细分**：不同场景有不同文档（嵌入式、CLI、WASM 等）

### 可借鉴点

- **用 `///` 文档注释生成 API 文档** → Java 的 Javadoc 类似，应充分利用
- **分层文档套件** → 不要把所有内容塞进一个文件
- **"The Book" 概念** → 可以写一份循序渐进的 eyelib 入门教程
- **工具书独立** → 构建脚本、CI、调试指南单独成册
- **示例代码可测试** → 文档中的代码片段应该可运行

---

## 5. Spring Framework 文档结构

### 结构摘要

Spring Framework 文档采用 **Antora + Asciidoc** 格式，源文件位于 `spring-framework/framework-docs/modules/`。按功能模块组织，而非按读者角色：

```
framework-docs/modules/
├── ROOT/                    # 根模块 — 总览 + 导航
│   └── pages/
│       ├── index.adoc       # 文档首页
│       ├── overview.adoc    # 概述
│       └── ...
├── core/                    # Core — IoC 容器、AOP、资源
│   └── pages/
│       ├── index.adoc
│       ├── beans/
│       ├── resources/
│       └── ...
├── testing/                 # 测试
│   └── pages/
│       ├── index.adoc
│       └── ...
├── data-access/             # 数据访问 — JDBC, ORM, 事务
│   └── pages/
│       ├── index.adoc
│       └── ...
├── web/                     # Web — Servlet, WebFlux
│   └── pages/
│       ├── index.adoc
│       ├── webmvc.adoc
│       ├── webflux.adoc
│       └── ...
└── integration/             # 集成 — JMS, JMX, Email, 调度
    └── pages/
        ├── index.adoc
        └── ...
```

### 组织原则

| 模块 | 内容 | 对应框架模块 |
|------|------|-------------|
| **ROOT** | 概述、起步、文档指南 | 全局 |
| **Core** | IoC 容器、AOP、SpEL、资源 | spring-core |
| **Testing** | 单元测试、集成测试、Mock | spring-test |
| **Data Access** | JDBC、事务、ORM、JPA | spring-jdbc, spring-orm |
| **Web Servlet** | Spring MVC、WebSocket | spring-webmvc |
| **Web Reactive** | WebFlux、WebClient | spring-webflux |
| **Integration** | JMS、JMX、Email、调度任务 | spring-jms, spring-context |

### 关键技术特征

1. **Antora 组件化文档**：每个功能模块是一个 Antora 组件，有独立的导航
2. **Asciidoc 格式**：比 Markdown 更强大（表格、脚注、包含、条件渲染）
3. **模块与代码模块对应**：文档模块结构 ≈ 代码模块结构
4. **ROOT 模块做聚合导航**：总览页引用所有子模块
5. **引用文档 (Reference) 为主**：主要是概念+参考，少教程（教程在 spring.io/guides）

### 可借鉴点

- **文档模块 ≈ 代码模块** → eyelib 每个模块应有对应的文档章节
- **ROOT 总览 + 独立模块文档** → `docs/` 目录下按模块分目录
- **Asciidoc 的 include 机制** → 避免重复内容，从源文件自动引用
- **参考文档 + 独立教程** → 区分"框架能力说明"和"使用步骤"

---

## 跨项目模式总结

### 共同的优秀实践

| 模式 | PostgreSQL | Linux Kernel | Kubernetes | Rust | Spring |
|------|:-----------:|:------------:|:-----------:|:----:|:------:|
| **按读者角色分层** | ✅ | ✅ | ✅ | ✅ | ⚡部分 |
| **按内容类型分类** | | | ✅ 核心 | ✅ | |
| **源码内嵌文档** | | ✅ kernel-doc | | ✅ rustdoc | ✅ Javadoc |
| **碎片化 + 索引** | ✅ SGML ENTITY | ✅ toctree | ✅ Hugo | ✅ mdbook | ✅ Antora |
| **独立构建系统** | ✅ Makefile | ✅ Sphinx | ✅ Hugo | ✅ mdbook | ✅ Antora |
| **元文档** | ✅ docguide | ✅ doc-guide/ | ✅ contribute/ | ✅ rustdoc book | |
| **多输出格式** | ✅ HTML/PDF/man | ✅ HTML/PDF | ✅ HTML | ✅ HTML/PDF | ✅ HTML/PDF |
| **文档可测试** | | | ✅ CI 验证 | ✅ doctest | |

### 给 eyelib 的具体建议

**1. 文档重组 — 按内容类型分 4 层（借鉴 Kubernetes + Rust）**

```
docs/
├── concepts/          # 概念 — 解释是什么、为什么
│   ├── architecture.md    (来自 architecture/)
│   ├── module-map.md      (来自 MODULES.md 精简)
│   └── render-pipeline.md
├── guides/            # 指南 — 怎么做 (替代 "tutorials" 或 "tasks")
│   ├── getting-started.md
│   ├── adding-a-module.md
│   └── debugging.md
├── reference/         # 参考 — 精确技术规格
│   ├── module-catalog.md  (来自 MODULES.md 格式化)
│   ├── api/               (Javadoc 生成的 API 文档)
│   └── config-options.md
└── decisions/         # 架构决策记录 (ADR)
    ├── 0001-*.md
    └── ...
```

**2. 大幅精简 AGENTS.md 和 SKILL.md**

- AGENTS.md → 仅保留**规则**和**流程**（< 100 行）
- SKILL.md → 仅保留**技能定义**和**使用说明**（< 100 行）
- Pitfalls → 移至 `docs/pitfalls/`
- 架构决策 → 移至 `docs/decisions/`
- 调试指南 → 移至 `docs/guides/debugging.md`

**3. 利用 Java 注释（借鉴 Rust kernel-doc）**

- 在 Java 源码中使用标准 Javadoc `/** ... */`
- Gradle 插件 (`javadoc` task) 生成 API 文档
- 在 `docs/reference/api/` 中引用

**4. 文档模块与代码模块对应（借鉴 Spring）**

```
docs/modules/
  core/
  mesh/
  particle/
  material/
  ...
```

每个模块目录下放该模块专属的概念、指南和参考。

**5. 善用 GitBook / mdBook / Docsify 等工具**

- 选择一种静态网站生成器
- 用 `SUMMARY.md` 或侧边栏导航组织全局结构
- 替代当前零散的 59 个 md 文件

