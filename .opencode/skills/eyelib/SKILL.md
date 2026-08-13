---
name: eyelib
description: Eyelib Forge 模组开发总索引——项目概览、Skill 导航、跨域约束。Load first for any eyelib task.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "3.0.0"
  tags: eyelib, forge, minecraft, java, index
  related-skills: eyelib-build, eyelib-debug, eyelib-renderdoc, eyelib-clientsmoke, eyelib-hexagonal-gates, eyelib-domain-extraction
---

# eyelib

Eyelib Forge 模组开发总索引——项目概览、Skill 导航、跨域约束

## When to use
- any eyelib task / eyelib Forge 模组开发（总索引，Load first）

## Rules
- When 测试 Oracle 优先级:
  - [when 写 spec-based 测试时] PREFER 写 spec-based 测试时，正确答案来源按优先级排序（由高到低）：1) Mojang Creator 文档 (E:\_____基岩版文档\minecraft-creator\creator\Documents\)；2) .mcpack 真实数据 (run/resourcepacks/*.mcpack)；3) Bedrock Wiki (E:\_____基岩版文档\bedrock-wiki\docs\)；4) 项目内部 pitfall/ADR（二次加工，可能有滞后或偏差）
- When 架构与领域知识:
  - SKILL 只放操作流程（ADR-0011）；架构概念、陷阱、规格、ADR 以 `docs/` 为权威源
- When 跨域约束:
  - NEVER 禁止猜测：不知道就说不知道，不要编造因果链
  - NEVER 禁止 `git add -A`（会污染 `3rdparty/rd_src` 子模块）
  - NEVER 子代理报告不可信：delegate_task 返回的 self-report 不能作为验证证据
  - 发现测试失败先查是否预存：`git stash` 切回原始代码复现
  - PREFER 系统性思维压倒逐实体验证：多实体共享 bug 优先找系统级根因
  - 渲染调试基准以 .mcpack Bedrock 数据 + Mojang 官方文档为准，不是 vanilla JE
  - 形式化证明必须从规范建模开始：从 Bedrock 官方文档提取形式操作语义，再对每个实现层做结构归纳
  - NEVER 禁止跳过数学建模直接读代码
  - 证明前必须读完全部源码：molang 模块 ~50 文件/205KB，必须全部载入上下文后才能声称"已验证"

<!-- locked residual (verbatim, do not edit) -->
项目根 `E:\_ideaProjects\qylEyelib`。MC 1.20.1 / Forge 47.1.3 / Java 17，Stonecutter active = `1.20.1`(legacyforge) + node `1.21.1`/`26.1.2`(neoforge)。26.1.2 渲染架构重写（声明式 PSO + submit 延迟提交），经 RenderSink 兼容层与 1.20.1 行为对齐（详见 `docs/concepts/cross-version-render-architecture.md`）。
| 需求 | 加载 |
|---|---|
| 编译、测试、Gradle 约束 | `eyelib-build` |
| 启动调试、/eval、渲染诊断 | `eyelib-debug` |
| mcmcp 拓展、.mcmcp 格式、端口机制 | `mcmcp` |
| GPU 截帧、RenderDoc 回放 | `eyelib-renderdoc` |
| Clientsmoke 烟雾测试 | `eyelib-clientsmoke` |
| 六边形架构验收 G1→G2→G3 | `eyelib-hexagonal-gates` |
| Domain Port 提取操作 | `eyelib-domain-extraction` |
| Mixin accessor/injector 编写 | `mixin-writing` |
| Codec 数据转换设计 | `codec-design` |
| molang 包重构协调 | `molang-refactor-supervisor` |
| 运行时状态探索 | `progressive-exploration` |
| 测试策略选择 | `testing` |
文档导航：`docs/README.md`
系统架构总览：`docs/concepts/architecture.md`
ECS ComponentStore 模式：`docs/concepts/ecs-architecture.md`
材质/渲染实现笔记：`docs/concepts/`（material-rendering-chain、bone-level-material-rendering 等）
Bedrock 规格与差距：`docs/specs/`、`docs/gap-analysis/`
ADR 索引（含已知陷阱 0007）：`docs/decisions/`
Molang 重构路线图：`docs/molang/ROADMAP.md`
