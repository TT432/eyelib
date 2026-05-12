# Feature Landscape

**Domain:** Multi-module project structural cleanup
**Researched:** 2026-05-12
**Overall confidence:** HIGH

## Table Stakes

这些是 v1.5 必须完成的清理任务，缺失则里程碑不完整。

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **ANIM-01: 无效接口清理** | v1.4 删除了 KeyFrame.java，但 `client/animation/` 仍有 22 个文件包括 bedrock/ 子目录未逐文件审计 | Med | 需要 IDE "Find References" 逐文件验证；bedrock/ 中大部分是活跃运行时 |
| **CAP-01: capability 残留审计** | v1.4 移动了 5 个纯 data/codec 类型到 attachment，需要审计确认无遗漏 | Low-Med | EyelibAttachableData 注册中心必须留在 root；需确认 EntityBehaviorData 的归属 |
| **PREP-01: preprocessing 归属扫描** | 确保所有 preprocessing 职责的代码已移入 `:eyelib-preprocessing` 模块 | Low | 主要消费者已正确引用 preprocessing；需要扫描 root 中残留的 parse/bake 模式 |
| **DUP-01: 重复代码排查** | 防止 root capability/ 中仍有 attachment 已拥有的数据类型的影子版本 | Med | 需要区分 intentional adaptation layer vs 真正的 copy-paste |
| **DOCS-01: 结构文档审视** | README.md 可能引用 `eyelib-processor`（旧名）或描述已不存在的代码位置 | Low | 文档更新是纯文本操作，无编译影响 |

## Differentiators

这些不是必须的，但完成后能显著提升代码库健康度。

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **bin/ 残留清理** | 清理已删除源文件的 stale .class 文件 | Low | 运行 `clean build` 即可，但需确认无遗留在非标准位置 |
| **EntityBehaviorData codec 归位** | 如果确认 codec 部分是纯数据，可提取到 attachment | Med | 需要先判断 codec/行为边界 |
| **Type hierarchy audit** | 验证 Br*Definition → Br*Schema → Runtime 三层架构无断裂 | Low | 已有架构文档清晰描述，验证即可 |

## Anti-Features

明确不应在此里程碑中构建的内容。

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **新 Gradle 子项目** | v1.5 目标是清理现有结构 | 保持 7 个子模块 + root 的现有拓扑 |
| **将 Br*Definition 运行时类型合并到 Importer** | 这些是 runtime-compiled adaptation，不是 schema | 保留在 root `client/animation/bedrock/` |
| **将 EyelibAttachableData 移到 attachment** | 它是 Forge 注册中心，依赖 root 的 `@Mod` bootstrap | 保留在 root，这是正确的架构 |
| **将活跃的 bedrock executor 判定为"无效代码"** | BrClipExecutor、BrControllerExecutor 是活跃运行时 | 精确区分活跃 vs 零引用 |
| **删除 legacy compatibility pointer README** | `mixin/README.md` 是 intentional compatibility pointer | 保留或更新，不删除 |

## Feature Dependencies

```
DOCS-01 (无代码依赖)
    ↓
ANIM-01 (需要 IDE 引用分析 — 不依赖其他任务)
    ↓
PREP-01 (需要完整的 animation bedrock/ 状态才能准确扫描)
    ↓
DUP-01 (需要 CAP-01 和 PREP-01 的发现结果)
    ↓
CAP-01 (最复杂，需要前序阶段的完整图景)
```

## MVP Recommendation

Prioritize:
1. **DOCS-01** — 零风险，立即执行
2. **ANIM-01** — 逐文件引用分析，删除零引用接口
3. **PREP-01** — 扫描和报告，不自动移动代码

Defer: 无——所有五个任务都是 v1.5 milestone 的 active requirements。

## Sources

- `.planning/PROJECT.md` — v1.5 Active Requirements (CAP-01, ANIM-01, PREP-01, DUP-01, DOCS-01)
- `MODULES.md` — 模块清单和边界规则
- `docs/architecture/01-module-boundaries.md` — 当前→目标所有权映射
- 所有 confidence: HIGH（来自 PROJECT.md 和架构文档的直接对应）
