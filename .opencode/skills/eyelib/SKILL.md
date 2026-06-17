---
name: eyelib
description: Eyelib Forge 模组开发总索引——项目概览、Skill 导航、跨域约束。Load first for any eyelib task.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "2.0.0"
  tags: eyelib, forge, minecraft, java, index
  related-skills: eyelib-build, eyelib-debug, eyelib-renderdoc, eyelib-clientsmoke, eyelib-hexagonal-gates, eyelib-domain-extraction
---

# Eyelib 开发

项目路径 `/mnt/e/_ideaProjects/qylEyelib`，MC 1.20.1 / Forge 47.1.3 / Java 17。

## Skill 导航

| 需求 | 加载 |
|---|---|
| 编译、测试、Gradle 约束 | `eyelib-build` |
| 启动调试、/eval、渲染诊断 | `eyelib-debug` |
| GPU 截帧、RenderDoc 回放 | `eyelib-renderdoc` |
| Clientsmoke 烟雾测试 | `eyelib-clientsmoke` |
| 六边形架构验收 G1→G2→G3 | `eyelib-hexagonal-gates` |
| Domain Port 提取操作 | `eyelib-domain-extraction` |

## 测试 Oracle 优先级

写 spec-based 测试时，正确答案的来源排序（由高到低）：

1. **Mojang Creator 文档** — `/mnt/e/_____基岩版文档/minecraft-creator/creator/Documents/`
2. **.mcpack 真实数据** — `run/resourcepacks/*.mcpack`
3. **Bedrock Wiki** — `/mnt/e/_____基岩版文档/bedrock-wiki/docs/`
4. **项目内部 pitfall/ADR** — 二次加工，可能有滞后或偏差

## 跨域约束

- **禁止猜测** — 不知道就说不知道，不要编造因果链
- **禁止 `git add -A`** — 会污染 `3rdparty/rd_src` 子模块
- **子代理报告不可信** — delegate_task 返回的 self-report 不能作为验证证据
- **发现测试失败先查是否预存** — `git stash` 切回原始代码复现
- **系统性思维压倒逐实体验证** — 多实体共享 bug 优先找系统级根因
- **渲染调试基准** — .mcpack Bedrock 数据 + Mojang 官方文档，不是 vanilla JE
- **形式化证明必须从规范建模开始** — 从 Bedrock 官方文档提取形式操作语义，再对每个实现层做结构归纳。禁止跳过数学建模直接读代码。
- **证明前必须读完全部源码** — molang 模块 ~50 文件/205KB，必须全部载入上下文后才能声称"已验证"。

## Repomix 模块打包

将模块源码打包为 AI 上下文：

```bash
# 单模块
repomix --style markdown --output /tmp/eyelib-module-xxx.md \
  --include "src/main/**" --ignore "build/**,.gradle/**" \
  /mnt/e/_ideaProjects/qylEyelib/eyelib-xxx

# 全项目 (main-only, ~465K tokens)
python scripts/repomix_main_only.py [/path/to/output.md]
```

> ⚠️ repomix 的 `--include` 是 **AND** 逻辑，多个模式取交集。必须用 brace expansion 合并：`{src/main,eyelib-*/src/main}/**`，不要拆成多个 `--include`。

模块→token 对照表详见 `references/repomix-module-packing.md`。

## ECS 架构：ComponentStore 模式

基岩版 ECS → eyelib 复刻时，System 之间只通过 Component 通信：

```
ComponentStore  ← 所有 System 的唯一数据交汇点
    │
    ├── S₁: 事件执行器 → 写入 C（如 minecraft:variant = 2）
    ├── S₂: Molang 查询  → 读取 C（q.variant → store.get("minecraft:variant")）
    └── S₃: ...
```

| ECS 层 | 测试策略 | eyelib 位置 |
|---|---|---|
| E (Entity) | 不测 | BrClientEntity / BrBehaviorEntityFile |
| C (Component) | 不测（纯数据） | Component 子类（Variant, Health...） |
| S (System) | **必须测** | 事件执行器、Molang 查询、渲染管线 |

新建 System 时只需声明读/写哪些 Component，不依赖其他 System。测试：往 ComponentStore 塞数据 → 调 System → 断言输出。详见 `references/ecs-architecture.md`。

## Bedrock 实现差距分析

> 详见 `docs/gap-analysis/` 目录。

流程：确定子类别 → 读 E 盘文档 → 平行 subagent 调查 → 输出 GAP-ANALYSIS → 集成测试封闭循环。

## 架构重构：六边形架构与 Domain 提取

> 详见：
> - 架构决策：`docs/decisions/0010-hexagonal-architecture.md`
> - Port 清单与进度：`docs/architecture/domain-module-map.md`
> - 操作流程：加载 `eyelib-hexagonal-gates`（验收闸门）或 `eyelib-domain-extraction`（Port 提取）

## 参考文件索引

`references/` 目录包含深度参考资料。按主题组织：

- **构建/环境**：`references/archunit-pitfalls.md`
- **调试/渲染**：`references/entity-debug-data-first.md`、`references/buffer-sharing-zfighting.md`、`references/brarchive-subpack-key-mismatch.md`、`references/mcp-state-recovery.md`、`references/entity-verification-workflow.md`、`references/entity-rendering-arena.md`
- **材质/纹理**：`references/material-codec-routing.md`、`references/material-rendering-chain.md`、`references/bone-level-material-rendering.md`、`references/material-inheritance-diagnostics.md`、`references/material-routing-verification.md`、`references/texture-material-per-pass-evaluation.md`、`references/multi-pack-material-loading.md`、`references/bedrock-rc-materials-spec.md`、`references/bedrock-materials-pass-semantics.md`、`references/rc-materials-discrepancies.md`
- **Alpha/颜色**：`references/bedrock-alpha-cutout-threshold.md`、`references/alpha-clamp-two-path.md`、`references/vertex-color-hardcoding.md`、`references/bedrock-box-uv-floor.md`
- **RenderDoc**：`references/known-good-capture-workflow.md`、`references/renderdoc-mcp.md`、`references/renderdoc-windows-replay.md`
- **Behavior**：`references/behavior-component-pitfalls.md`、`references/behavior-runtime-testing.md`、`references/behavior-component-spec-location.md`
- **Molang**：`references/molang-spec-driven-testing.md`、`references/molang-antlr-removal.md`、`references/molang-scope-eval-debugging.md`、`references/molang-junit-testing.md`、`references/molang-bytecode-semantics.md`、`references/molang-formal-proof.md`
- **网络/ClassLoader**：`references/network-cce-classloader-root-cause.md`、`references/cross-classloader-network-diagnostics.md`、`references/forge-transformer-network-channel.md`
- **架构/Domain**：`references/domain-extraction-pitfalls.md`、`references/hexagonal-port-extraction.md`、`references/port-extraction-lessons.md`、`references/stonecutter-multiversion-patterns.md`
- **文档**：`references/doc-audit-checklist.md`、`references/documentation-design-baseline.md`
- **工具链**：`references/repomix-module-packing.md`
- **其他**：`references/clientsmoke-testing.md`、`references/nativeimageio-download-gotcha.md`、`references/resource-reload-profiling.md`、`references/mcpack-decomposition-workflow.md`、`references/spec-based-testing.md`、`references/ecs-architecture.md`、`references/delegation-patterns.md`
