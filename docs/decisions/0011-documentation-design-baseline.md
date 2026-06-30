# ADR-0011: 文档设计基线 — Diátaxis 四象限重组

**Status:** Accepted
**Date:** 2026-06-09
**Author:** @TT432

## Context

### 问题

eyelib 的文档随项目演进自然增长，产生了以下问题：

1. **内容重复**：SKILL.md（796行）和 AGENTS.md 大量内容重叠。Pitfall 记录同时存在于 SKILL.md 内联、`docs/pitfalls/`、`docs/decisions/0007` 三处。
2. **类型混淆**：AGENTS.md 同时包含规则、架构决策、pitfall 记录、操作流程——违反了 Diátaxis 的"不混类型"原则。
3. **无导航入口**：59 个 docs/ 文件分布在 10 个子目录中，无统一入口。
4. **历史残留**：`docs/pipeline/`、`docs/review/` 中的计划文档已完成但未清理。
5. **ADR 分散**：ADR-0010 在 `docs/architecture/` 而非 `docs/decisions/`。

### 目标

建立清晰的文档设计基线，让每个文档有单一职责，每个问题有唯一查找路径。

## Decision

### 文档分层

采用 **Diátaxis 四象限**（概念/指南/参考/决策）+ **C4 Model**（架构可视化）+ **ADR**（决策记录）：

```
docs/
├── README.md                ← 导航入口
├── concepts/                ← 解释「是什么、为什么」
│   ├── architecture.md      ← 系统架构总览（含 C4 分层图）
│   └── module-map.md        ← 模块清单（面向阅读）
├── decisions/               ← 所有 ADR 统一目录
│   └── 0001-0011.md
├── architecture/            ← 架构操作手册（提取指南、验收闸门）
├── reference/               ← 精确技术规格（Bedrock 参考）
├── specs/                   ← 行为规格
├── pitfalls/                ← 故障排查
├── gap-analysis/            ← Bedrock 差距分析
├── guides/                  ← 操作指南（预留）
├── tests/                   ← 测试计划
└── research-*.md            ← 研究笔记
```

> **注**：此目录树为 ADR-0011 发布时（2026-06-09）的设计快照。后续 ADR-0014（flat-merge）、ADR-0015（stonecutter 多版本）及文档演化已改变实际结构（`reference/` 迁移至 `.opencode/skills/`、`pitfalls/`/`guides/` 已移除、`tests/` 已删、`concepts/module-map.md` 重命名为 `architecture/domain-module-map.md`）。当前文档结构以 `docs/README.md` 和 `MODULES.md` 为权威源。

### 文件变更

| 变更 | 旧位置 | 新位置 |
|------|--------|--------|
| 新建 | — | `docs/README.md`（导航入口） |
| 新建 | — | `docs/concepts/architecture.md`（架构总览） |
| 新建 | — | `docs/concepts/module-map.md`（模块清单） |
| 新建 | — | `docs/decisions/0011-documentation-design-baseline.md`（本 ADR） |
| 精简 | `AGENTS.md`（150行） | `AGENTS.md`（157行，微调） |
| 大幅精简 | `SKILL.md`（796行→450行） | `SKILL.md`（去重 380 行） |
| 标记残留 | `docs/pipeline/`、`docs/review/` | 历史计划文件，建议归档或删除 |

### 核心原则

1. **按内容类型分层**：概念（是什么）≠ 指南（怎么做）≠ 参考（精确规格）≠ 决策（为什么）。
2. **AGENTS.md 只放规则**：注释规范、文档规则、编辑规则、构建验证——不含陷阱、架构知识、操作流程。
3. **SKILL.md 只放操作流程**：构建命令、MCP 工具使用、RenderDoc 流程、诊断 Phase——不含陷阱详情和架构文档。
4. **知识权威在 docs/**：所有 pitfall、ADR、架构规格以 docs/ 中的文件为 canonical source。

## Consequences

### Positive

- **查找路径唯一**：每个问题只有一个 canonical 位置。
- **内容无重复**：AGENTS.md、SKILL.md、docs/ 各司其职。
- **新成员友好**：docs/README.md → concepts/architecture.md → decisions/ 的阅读路径清晰。
- **ADR 统一**：所有决策记录在 decisions/ 下，编号连续。

### Negative / Risk

- **docs/pipeline/ 和 docs/review/ 文件未删除**：历史计划文件保留在原地，等待确认后清理。它们违反了"不在活跃文档中留历史"原则。
- **guides/ 目录为空**：尚未创建调试指南和验证指南，当前这些内容仍在 SKILL.md 中。

### 不做的

- 不动 `docs/reference/bedrock-addon/`（外部参考，结构合理）。
- 不动 `docs/pitfalls/`（已有独立文件）。
- 不动 `docs/architecture/` 中的操作手册（提取指南、验收闸门——它们是操作文档，不是决策记录）。
- 不在此 ADR 中创建 guides/ 内容（后续任务）。
