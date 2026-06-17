# 文档设计基线 — 方法论参考

> 2026-06-09 文档重组的研究基础。综合 6 种设计方法论的研究成果。

## 方法论总览

| 方法论 | 核心价值 | eyelib 采用 |
|--------|----------|:-----------:|
| **Diátaxis 四象限** | 按内容类型分 Tutorial/How-to/Reference/Explanation | ✅ 核心结构 |
| **C4 Model** | 4 层架构图（Context/Container/Component/Code） | ✅ 可视化 |
| **ADR** | 轻量决策记录（Context→Decision→Consequences） | ✅ 已采用 |
| **arc42** | 12 章节架构模板（§5/6/8 最适合模块设计） | 部分（§5/8） |
| **Living Documentation** | 文档与代码同源、自动化生成 | 部分（Javadoc） |
| **Team API** | 模块间契约文档 | 待采用 |

## Diátaxis 四象限

| 象限 | 问题 | eyelib 对应 |
|------|------|------------|
| **Tutorial** | 怎么上手？ | 待编写 |
| **How-to Guide** | 怎么做 X？ | SKILL.md（操作流程） |
| **Reference** | X 是什么？ | docs/reference/ + Javadoc |
| **Explanation** | 为什么？ | docs/concepts/ + ADR |

## C4 Model — 4 层架构图

1. **System Context**: eyelib ↔ Minecraft、GPU 驱动、其他 mod 的边界
2. **Container**: 模块分层（Root → Bridge → Domain → Adapter）
3. **Component**: 每个 domain 模块的内部接口（Port/manager/visitor 等）
4. **Code**: UML 类图级别实现细节（用 Javadoc 替代）

## ADR 规范

每个 ADR 文件 = 一个决策。格式：

```
# ADR-NNNN: 标题
**Status:** Proposed/Accepted/Deprecated/Superseded
**Date:** YYYY-MM-DD

## Context (为什么需要决策)
## Decision (做了什么选择)
## Consequences (后果：什么变容易了，什么变难了)
```

## 经典开源项目参考

| 项目 | 组织原则 | 可借鉴点 |
|------|----------|----------|
| PostgreSQL | 按读者角色分 6 个 Part | 单一源多格式输出 |
| Linux Kernel | Sphinx toctree 树状，按读者分 50+ 目录 | kernel-doc 源码注释提取 |
| Kubernetes | Diátaxis 内容类型，不按组件 | 概念与操作分离 |
| Rust | 分层文档套件（Book/Reference/Nomicon） | 示例代码可测试(doctest) |
| Spring | Antora 组件化，模块对应代码模块 | Asciidoc include 防重复 |

## eyelib 文档重组规则

1. **按内容类型分层** — 概念（是什么）≠ 指南（怎么做）≠ 参考（精确规格）≠ 决策（为什么）
2. **AGENTS.md 只放规则** — 注释规范、文档规则、编辑规则、构建验证
3. **SKILL.md 只放操作** — 构建命令、MCP 工具使用、RenderDoc 流程
4. **知识权威在 docs/** — pitfall、ADR、架构规格以 docs/ 为 canonical source
5. **不混类型** — 同一篇文档只做一件事
6. **不在活跃文档留历史** — 已完成任务属于 git history
