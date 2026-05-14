# Phase 29: DUP — 重复代码排查报告

**Date:** 2026-05-12
**Status:** Read-only analysis — no code modified

## 执行摘要

**结论: 干净代码库，零真正的复制粘贴重复。** 所有相似性均为有意设计的适配层，架构合理。

## 分析领域

### 1. Capability/数据类型 (Root vs Attachment)
11 个类别对已分析，0 个真正重复。全部为 **运行时状态 vs. 可序列化 DTO** 分离:
- Root `AnimationComponent` / `ModelComponent` 持有可变运行时状态
- Attachment `AnimationComponentInfo` / `ModelComponentInfo` 是不可变可序列化 DTO 记录
- `RenderData.codec()` 直接导入 attachment 模块 `CODEC` — 这是跨模块序列化缝合面

### 2. Codec 模式
**0 重复。** 28+ root 导入 + 10+ attachment 导入，全部委托到单一 `:eyelib-util`:
- `eyelib-util/codec/` — `EyelibCodec`, `CodecHelper`, `TupleCodec`, `DispatchedMapCodec` 等
- `eyelib-util/streamcodec/` — `EyelibStreamCodecs`, `StreamCodec` 接口
- root `util/` 和 `core/util/` 已完全排空（零 Java 文件）

### 3. `fromSchema()` 模式一致性
**0 重复。** 12 个 `fromSchema()` 方法横跨 4 个模块，形成良好的 **3 层适配器链**:
- **Layer 1 (Importer):** 原始 JSON schema 类型
- **Layer 2 (Preprocessing):** 中间烘焙类型（平台无关）
- **Layer 3 (Root runtime):** 运行时包装类型（委托到 importer + preprocessing）

每层的 `fromSchema()` 做适合其层的不同工作。Root 运行时的委托向下，不是重新实现。

### 4. Loaders — 0 重复
14 个 loader 类别分析:
- Root loaders 共享基类 `BrResourcesLoader`（故意的模式共享）
- 子项目 loaders 服务不同职责：格式解析（importer）、addon 发现（importer）、通用库加载（util）

### 5. Manager/Registry — 0 重复
15 个类别分析:
- `Manager<T>` 层次结构是有意的领域特化（每个类型参数确实不同）
- Root registries 是发布缝合面，不是存储
- `ParticleDefinitionRegistry` 遵循完全不同的架构（模块独有、字符串键、发布者模式）

### 6. 工具类别 — 0 重复
v1.3 工具分离已完全完成：root `util/` 和 `core/util/` 已排空。`eyelib-util` 为 11 个包中 30 个工具类的唯一拥有者。

### 7. Capability 注册路径 — 无重复职责
6 个注册路径分析，清晰的层分离:
- Root `EyelibAttachableData`: 入口 + Forge 注册（仅一个注册主干）
- Attachment: 数据契约类型
- `mc/impl/*`: 平台接线（Forge 事件、NBT、capabilities）

## 汇总

| 领域 | 分析对 | 真正重复 | 有意适配 |
|------|-------|---------|----------|
| Capability/数据类型 | 11 | 0 | 7 对是运行时-DTO 分离 |
| Codec 模式 | 38+ 引用 | 0 | 全部委托到 eyelib-util |
| `fromSchema()` | 12 方法 | 0 | 3 层适配器链 |
| Loaders | 14 | 0 | 领域特定 |
| Manager/Registry | 15 | 0 | 模板模式/领域特化 |
| 工具类 | 30 | 0 | root util 完全排空 |
| Capability 注册 | 6 | 0 | 引导/契约/Forge/事件分离 |
