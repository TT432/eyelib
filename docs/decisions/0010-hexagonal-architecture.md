# ADR-0010: 六边形架构 — Domain/Bridge 分层与 Working Core 提取

**Status:** Implemented (partially — all domain modules have MC import exclusion rules enforced by ArchUnit; remaining are @Mod bootstrap, platform bindings, and FriendlyByteBuf hard dependencies)
**Date:** 2026-06-08
**Author:** @TT432

## Context

### 问题

当前 eyelib 的模块边界按功能划分（material、animation、molang 等），但没有按"是否依赖 MC 运行时"分层。后果：

1. **行为无法独立验证**：大量逻辑只能在 MC 进程中通过视觉观察来验证。测试"全绿"不代表行为正确——AI 生成的测试会 pin 当前实现而非验证 Bedrock 规范。
2. **多版本支持困难**：MC API 接触点散布在 10 个模块中，切换 MC 版本时需要扫描全项目。
3. **无 working core**：违反 **Gall's Law**——"A complex system that works is invariably found to have evolved from a simple system that worked." eyelib 从第一天就是多模块设计，没有一个简单可验证的 working core 逐步演化。

### 目标

> 在不启动 Minecraft 的情况下，证明 eyelib 的每一层行为都正确实现了 Bedrock 规范。

## Decision

### 分层架构

```
┌──────────────────────────────────────────────────────┐
│  eyelib-bridge (adapter) — 新子项目                    │
│  依赖: 所有 domain 模块 + eyelib-util                 │
│  · 实现 domain 层定义的 Port 接口                       │
│  · 所有 net.minecraft.* import 的唯一栖息地            │
│  · Stonecutter //? if 注释的唯一栖息地                  │
│  · MC 纹理上传、RenderType 映射、BufferBuilder 桥接    │
├──────────────────────────────────────────────────────┤
│  Domain 模块 (零 MC import)                            │
│  eyelib-molang  eyelib-material  eyelib-model         │
│  eyelib-animation  eyelib-behavior  eyelib-particle   │
│  eyelib-importer                                      │
│  · Port 接口定义在各自模块中（domain 说"我需要 X"）      │
│  · 纯 Bedrock 逻辑：CODEC 解析、继承链、状态机、Molang 求值│
│  · 可在 JUnit 中独立验证，oracle 来自 Bedrock 规范       │
├──────────────────────────────────────────────────────┤
│  eyelib-util (共享工具，零 MC)                          │
├──────────────────────────────────────────────────────┤
│  Adapter 模块 (原本就是 MC 胶水)                        │
│  eyelib-network  eyelib-attachment  eyelib-track      │
│  eyelib-preprocessing                                 │
├──────────────────────────────────────────────────────┤
│  Root 模块 (Forge 生命周期)                             │
│  · 依赖 bridge + 所有模块                               │
│  · Forge bootstrap、EntityRenderSystem、loader 注册      │
│  · 已有 Manager/Registry/reload 基础设施保持不变         │
└──────────────────────────────────────────────────────┘
```

### 核心约束

1. **Domain 模块不 import `net.minecraft.*` 或 `net.minecraftforge.*`。** 编译时由 ArchUnit 规则强制。
2. **Domain 模块的测试 oracle 来自 Bedrock 规范**（Mojang Creator 文档 / .mcpack 数据），不来自当前实现输出。
3. **Port 接口由 domain 模块定义，`eyelib-bridge` 实现。** 依赖方向: bridge → domain，无循环。
4. **Bridge 模块是所有 Stonecutter `//? if` 的唯一栖息地。** 多版本切换只改一个模块。

### 按 Gall's Law 的提取顺序

从最内层（已最接近纯 domain）开始：

| 批次 | 模块 | 提取前 MC 文件数 | 当前 MC 文件数 | 状态 |
|------|------|-----------------|---------------|------|
| 1 | eyelib-material | 33+ | 3 (@Mod + BrShaderMapping + BrMaterialEntry) | 🔶 ArchUnit 通过，R2 消除剩余 |
| 1 | eyelib-molang | 28+ | 4 (@Mod + 3 platform/) | ✅ ArchUnit 通过，platform/ 按 ROADMAP 保留 |
| 2 | eyelib-model | 1 | 2 (@Mod + ModelComponentSyncPacket) | ✅ ArchUnit 通过，FriendlyByteBuf 硬依赖 |
| 3 | eyelib-animation | 4 | 4 (@Mod + network + 2 Entity 引用) | 🔶 ArchUnit 通过，R4 Entity→Port 待完成 |
| 3 | eyelib-behavior | 4 | 1 (@Mod) | ✅ ArchUnit 通过，已达最小 MC 接触 |
| 4 | eyelib-particle | 15 | 8 (@Mod + client/ + network/) | ✅ ArchUnit 通过，client 渲染桥待 R4 |

每批次验证闸门：ArchUnit 过 → spec-based 测试全绿 → 再进入下一批。

### 与现有 ADR 的关系

- **ADR-0002** (模块边界) 补充：新增 `eyelib-bridge` 子项目，bridge → domain 单向依赖。Domain 模块间依赖方向通过 Port 接口保持单向。
- **ADR-0005** (功能债务台账) 不冲突：domain 提取是对已有模块的内部分层，不改变模块间的功能所有权。
- **stonecutter-multiversion-patterns.md** 的 Pattern 5 (模块分层隔离) 是本 ADR 的依据：contact surface area law → MC 接触点收拢到 bridge。

## Consequences

### Positive

- **行为可离线验证**：Domain 层测试不需要 MC 运行时。对照 Bedrock 文档 + .mcpack JSON 跑 JUnit 即可验证 CODEC 往返、材料继承链、Molang 求值、RC 状态转换。
- **Oracle 正确**：测试的期望值来自 Bedrock 文档，不是来自当前代码输出。
- **多版本可控**：MC 版本切换只影响 `eyelib-bridge` 一个模块。
- **编译隔离**：ArchUnit 规则自动化检查 domain 模块的 MC 依赖，违反即编译失败。当前所有 6 个 domain 模块 ArchUnit 通过（@Mod bootstrap 类排除）。

### Negative / Risk

- **Bridge 变成新 dumping ground**：缓解——Port 接口由各 domain 模块定义，bridge 不能自行发明抽象。bridge 内部按 domain 模块分包子目录（`io.github.tt432.eyelibbridge.material/`, `...animation/`）。
- **重构期间功能暂时退化**：提取过程中某些运行时桥接可能不完整。缓解——每批次编译 + 已有渲染截帧验证，不做大爆炸式合并。
- **Port 接口设计不当**：过度抽象或粒度过细。缓解——Port 只在被至少两个 MC 代码路径调用时才创建；单一 use case 直接暴露具体 MC 类型给 bridge 内部，不走 Port。

### 不做的

- 不重命名现有类或包结构（除非 move 到 bridge 的文件需要新包名）。Manager/Loader/Visitor 模式保持。
- 不动 `eyelib-network`、`eyelib-attachment`、`eyelib-track`、`eyelib-preprocessing`——它们已经是 adapter 层。
- 不动 root 模块的 Manager/Registry/reload 基础设施——它们是 Forge 生命周期胶水，职责明确。

## Verification

- [x] `eyelib-material` ArchUnit 规则通过（排除 @Mod + BrShaderMapping + BrMaterialEntry）
- [x] `eyelib-molang` ArchUnit 规则通过（排除 @Mod + platform/）
- [x] `eyelib-model` ArchUnit 规则通过（排除 @Mod + network.packet/ + Model）
- [x] `eyelib-animation` ArchUnit 规则通过（排除 @Mod + network/ + Entity 引用类）
- [x] `eyelib-behavior` ArchUnit 规则通过（仅排除 @Mod）
- [x] `eyelib-particle` ArchUnit 规则通过（排除 @Mod + client/ + network/）
- [x] Spec-based 测试对照 Bedrock 文档：material(28) + molang(21) + animation(7) + behavior(9) + particle(6) = 71 个
- [x] RenderDoc 截帧 / 运行时验证：`shouldRender=true`, `comps=2`, `useBuiltInRenderSystem=true`
- [ ] `eyelib-material` BrShaderMapping + BrMaterialEntry 迁移到 bridge（R2 未完成部分）
- [ ] `eyelib-animation` Entity 引用 → PortEntity（R4 未完成部分）
