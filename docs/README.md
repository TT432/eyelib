# Eyelib 文档体系

> 架构知识按 **Diátaxis** 组织：概念（concepts/）/ 决策（decisions/）/ 规格（specs/）/ 差距分析（gap-analysis/）。
> 操作流程（How-to）已迁移到 Hermes Skill，通过 AGENTS.md 的 Skill 导航加载。

## 快速导航

| 你在找什么？ | 去哪里？ |
|-------------|---------|
| 项目规则（注释、编辑、构建） | [AGENTS.md](../AGENTS.md) |
| 操作流程（构建、调试、截帧、域提取、验收闸门） | [Hermes Skills](../../.hermes/profiles/qyleyelib/skills/eyelib/) — 见下方 Skill 索引 |
| 架构决策（为什么这么设计） | [decisions/](decisions/) — ADR 编号索引 |
| 系统架构总览（C4 分层图） | [concepts/architecture.md](concepts/architecture.md) |
| 行为规格（测试 oracle） | [specs/](specs/) |
| Bedrock 差距分析 | [gap-analysis/](gap-analysis/) |
| 测试计划 | [tests/](tests/) |
| 六边形架构 Port 清单与提取进度 | [architecture/domain-module-map.md](architecture/domain-module-map.md) |

## Skill 索引

操作流程全部以 Hermes Skill 形式维护，由 AGENTS.md 加载。项目相关 Skill 位于 `eyelib/` 分类：

| Skill | 用途 |
|-------|------|
| `eyelib` | 项目总索引——概览、Skill 导航、跨域约束 |
| `eyelib-build` | 构建、测试、环境——Gradle/WSL/Windows 交叉编译全流程 |
| `eyelib-debug` | MCP 调试——启动客户端、/eval 执行代码、渲染诊断 Phase |
| `eyelib-clientsmoke` | Clientsmoke 客户端烟雾测试 |
| `eyelib-renderdoc` | GPU 调试——RenderDoc 截帧、headless 回放 |
| `eyelib-hexagonal-gates` | 六边形架构验收闸门——G1(ArchUnit)→G2(spec-test)→G3(RenderDoc) |
| `eyelib-domain-extraction` | Domain Port 提取操作手册——定位 MC 接触点、创建 Port、迁移 bridge |

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

## 核心原则

1. **不混类型**：同一篇文档只做一件事。
2. **代码即权威**：包结构、类名、方法签名是"存在什么"的 source of truth。
3. **操作流程在 Skill**：How-to 类知识以 Skill 形式维护，由 AGENTS.md 加载。
4. **ADR 记录决策**：每个架构决策单独一篇 ADR → `decisions/NNNN-title.md`。
5. **不在活跃文档里留历史**：已完成的任务、已解决的问题属于 git history。

## 维护规则

1. 新增或删除模块 → 更新 `MODULES.md`。
2. 新增架构决策 → 在 `decisions/` 中创建新 ADR，更新本文件 ADR 索引。
3. 发现新陷阱 → 判断属于哪个 Skill 域，添加到对应 Skill 的 Common Pitfalls 节。
4. 新增操作流程 → 创建或更新对应 Skill，更新本文件 Skill 索引。
5. 文档自引用必须验证路径有效性。
