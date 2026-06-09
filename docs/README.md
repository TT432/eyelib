# Eyelib 文档体系

> 设计基线入口。所有架构知识按 **Diátaxis 四象限**（概念/指南/参考/决策）组织。

## 快速导航

| 你在找什么？ | 去哪里？ |
|-------------|---------|
| 项目规则（注释、编辑、构建） | [AGENTS.md](../AGENTS.md) |
| 操作流程（构建、调试、截帧） | [Hermes Skill: eyelib](../.hermes/profiles/qyleyelib/skills/eyelib/eyelib/SKILL.md) |
| 架构决策（为什么这么设计） | [decisions/](decisions/) — ADR 编号索引 |
| 系统架构总览（模块关系、分层） | [concepts/architecture.md](concepts/architecture.md) |
| 模块层级与依赖 | [concepts/module-map.md](concepts/module-map.md) |
| Bedrock 外部参考 | [reference/](reference/) |
| 行为规格（spec） | [specs/](specs/) |
| 已知陷阱 | [pitfalls/](pitfalls/) |
| Bedrock 差距分析 | [gap-analysis/](gap-analysis/) |
| 测试计划 | [tests/](tests/) |

## 文档哲学

### 四象限分类

遵循 **Diátaxis** 框架，按内容类型分四层：

```
docs/
├── concepts/       ← 解释「是什么、为什么」
│   ├── architecture.md      系统架构总览（含 C4 分层图）
│   └── module-map.md        模块清单（面向阅读）
├── guides/         ← 「怎么做」
│   ├── debugging.md         调试方法论
│   └── verification.md      验证流程
├── reference/      ← 精确技术规格
│   ├── bedrock-addon/       Bedrock addon 格式参考
│   └── module-catalog.md    API/模块目录
├── decisions/      ← 架构决策记录 (ADR)
│   ├── 0001-*.md
│   └── ...
├── specs/          ← 行为规格（测试 oracle）
├── pitfalls/       ← 故障排查记录
├── gap-analysis/   ← Bedrock 差距分析
└── tests/          ← 测试计划
```

### 核心原则

1. **不混类型**：同一篇文档只做一件事（解释原理 OR 教怎么做，不混在一起）。
2. **代码即权威**：包结构、类名、方法签名是对「存在什么」的 source of truth。外部参考（Bedrock 标准、Blockbench 格式）放在 `reference/`。
3. **ADR 记录决策**：每个架构决策单独一个 ADR → `decisions/NNNN-title.md`。格式：Context → Decision → Consequences。
4. **不在活跃文档里留历史**：已完成的任务、已解决的问题、历史中间状态属于 git history，不属于当前文档。
5. **路径必须有效**：每处文件引用都必须指向存在的文件。如果引用的文件被删除或移动，立即更新或删除引用。

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
| 0012 | System 层测试策略 — 三层 Fake-Contract 模型 | Proposed (2026-06-09) |

## 维护规则

1. 新增或删除模块 → 更新 `concepts/module-map.md`。
2. 新增架构决策 → 在 `decisions/` 中创建新 ADR，更新本文件 ADR 索引。
3. 发现新陷阱 → 判断是否与已有记录重合。重合则合并，不重合则新建 `pitfalls/` 文件。
4. 文档自引用必须验证路径有效性。
