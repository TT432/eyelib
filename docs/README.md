# Eyelib 文档体系

> 架构知识按 **Diátaxis** 组织：概念（concepts/）/ 决策（decisions/）/ 规格（specs/）/ 差距分析（gap-analysis/）。
> 操作流程（How-to）以 opencode Skill 形式维护，由 AGENTS.md 加载，位于 [.opencode/skills/](../.opencode/skills/)。

## 快速导航

| 你在找什么？ | 去哪里？ |
|-------------|---------|
| 项目规则（注释、编辑、构建、文档同步） | [AGENTS.md](../AGENTS.md) |
| 操作流程（构建、调试、截帧、域提取、验收闸门） | [.opencode/skills/](../.opencode/skills/) — 见下方 Skill 索引 |
| 架构决策（为什么这么设计） | [decisions/](decisions/) — ADR 编号索引 |
| 系统架构总览（构建布局 + 模块分层） | [concepts/architecture.md](concepts/architecture.md) |
| 行为规格（测试 oracle） | [specs/](specs/) |
| Bedrock 差距分析 | [gap-analysis/](gap-analysis/) |
| 六边形架构 Port 清单与提取进度 | [architecture/domain-module-map.md](architecture/domain-module-map.md) |
| Molang 重构路线图 | [molang/ROADMAP.md](molang/ROADMAP.md) |
| Spark 性能基线与优化 | [perf/spark-baseline-and-optimizations.md](perf/spark-baseline-and-optimizations.md) |

## Skill 索引

操作流程全部以 opencode Skill 形式维护，由 AGENTS.md 加载。项目相关 Skill 位于 `.opencode/skills/`：

| Skill | 用途 |
|-------|------|
| `eyelib` | 项目总索引——概览、Skill 导航、跨域约束 |
| `eyelib-build` | 构建、测试、环境——Gradle(Stonecutter 多版本) + eyelib-debug MCP 全流程 |
| `eyelib-debug` | MCP 调试——启动客户端、/eval 执行代码、渲染诊断 Phase |
| `eyelib-clientsmoke` | Clientsmoke 客户端烟雾测试 |
| `eyelib-renderdoc` | GPU 调试——RenderDoc 截帧、headless 回放 |
| `eyelib-hexagonal-gates` | 六边形架构验收闸门——G1(ArchUnit)→G2(spec-test)→G3(RenderDoc) |
| `eyelib-domain-extraction` | Domain Port 提取操作手册——定位 MC 接触点、创建 Port、迁移 bridge |
| `codec-design` | Codec 数据转换设计与实现——序列化、loader/schema Codec 化 |
| `mixin-writing` | Mixin accessor/injector 编写——反射替换、vanilla 注入、双版本模式 |
| `molang-refactor-supervisor` | molang 包重构切片协调——子代理设计/实现/审查/验证 |
| `progressive-exploration` | 运行时状态交互式探索——probe 屏幕、检查游戏状态、UI 导航 |
| `smoke-test` | ClientSmoke 视觉行为测试——渲染输出、GL 状态、纹理正确性 |
| `testing` | 测试策略决策框架——progressive / unit / smoke 选择 |
| `unit-test` | JUnit 5 单元测试——结构不变量、边界、codec 往返、null 安全 |

## ADR 索引

| 编号 | 标题 | 状态 |
|------|------|------|
| 0001 | 模块架构控制规范 | Accepted |
| 0002 | 模块边界 | Accepted |
| 0003 | Side 边界 | Accepted |
| 0004 | 生成代码策略 | Accepted |
| 0005 | MC 功能债务台账 | Accepted |
| 0006 | 关键架构决策 | Accepted |
| 0007 | 已知陷阱与反模式 | Accepted |
| 0008 | Item Track 设计 | Accepted |
| 0009 | Domain 事件—粒子交互 | Accepted |
| 0010 | 六边形架构 — Domain/Bridge 分层 | Implemented |
| 0011 | 文档设计基线 | Accepted (2026-06-09) |
| 0012 | System 层测试策略 | Proposed (2026-06-09) |
| 0013 | 基岩版动画控制器 & 动画计算逻辑（附录 A：`query.*` 函数清单） | Accepted (2026-06-15) |
| 0014 | 模块扁平合并 — 取消 Gradle 子项目 | Accepted (2026-06-17) |
| 0015 | Stonecutter 多版本改造与 ArchUnit 恢复 | Accepted (2026-06-18) |
| 0016 | 库隔离标准 — DDD 分层与 ACL 职责边界 | Accepted (2026-06-25) |
| 0017 | 移除 migrate26Renames 文本替换，统一用 `//?` 条件注释 | Accepted (2026-06-28) |
| 0018 | 孤立静止片段架构（IQF）—— 片段形状判据 + ACL 开放契约（反射调度） | Accepted (2026-06-28) |

## 核心原则

1. **不混类型**：同一篇文档只做一件事。
2. **代码即权威**：包结构、类名、方法签名是"存在什么"的 source of truth。
3. **操作流程在 Skill**：How-to 类知识以 Skill 形式维护，由 AGENTS.md 加载。
4. **ADR 记录决策**：每个架构决策单独一篇 ADR → `decisions/NNNN-title.md`。
5. **不在活跃文档里留历史**：已完成的任务、已解决的问题属于 git history。

## 维护规则

1. 新增或删除模块 → 更新对应 `package-info.java`，跑 `:1.20.1:generateModulesMd` 重新生成 `MODULES.md`。
2. 新增架构决策 → 在 `decisions/` 中创建新 ADR，更新本文件 ADR 索引；若修订旧 ADR，在旧 ADR 头部加 `amended/superseded by` 标注。
3. 发现新陷阱 → 操作类陷阱（执行某流程时易犯）添加到对应 Skill 的 Common Pitfalls 节；架构/领域类陷阱添加到 `decisions/0007-known-pitfalls.md` 或对应 ADR。
4. 新增操作流程 → 创建或更新对应 Skill，更新本文件 Skill 索引。
5. 改 `build.gradle` / `settings.gradle` / Stonecutter node → 同步 AGENTS.md Tooling Restrictions 与本文件导航表（若结构变）。
6. 文档自引用必须验证路径有效性——提交前 grep 全仓库验证无旧路径残留。
7. Skill 的 `references/` 只放支撑该 skill 操作流程的结构化参考（命令清单/模板/API）；架构概念、陷阱根因、实现决策、历史记录归 `docs/`（ADR-0011）。
