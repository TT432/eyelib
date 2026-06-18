# Stonecutter 多版本改造 — 交接报告

**Date:** 2026-06-18
**Branch:** `1.20.1`
**Status:** Phase 1 + Phase 2 完成（1.20.1 零回归 + 1.21.1 编译通过）

## 一句话总结

Stonecutter 0.7.11 多版本脚手架落地，**1.20.1 node 编译+测试零回归，1.21.1 node 编译通过**（从 100 个错误降到 0）。Forge→NeoForge 全量 API 迁移完成（网络功能 stub 除外）。

## 已完成的里程碑

### Phase 1：Stonecutter 脚手架 + 1.20.1 node ✅
- Stonecutter 0.7.11 集成（Gradle 8.12.1 + Groovy DSL）
- `settings.gradle` / `stonecutter.gradle` / `build.gradle` centralScript 模式
- `versions/1.20.1/gradle.properties` + `versions/1.21.1/gradle.properties`
- `:1.20.1:compileJava` BUILD SUCCESSFUL
- `:1.20.1:test` 976 测试 / 3 预存失败 / 9 skip — **零回归**（stash 验证 3 个失败迁移前就存在）

### Phase 2：1.21.1 node 编译通过 ✅
- Forge→NeoForge API 全量迁移（从 100 错误 → 118 错误 → 35 → 24 → **0**）
- Capability 门面：Forge Capability → NeoForge Data Attachment（`AttachmentType` + `NeoForgeRegistries.ATTACHMENT_TYPES`）
- 事件系统：`net.minecraftforge.*` → `net.neoforged.*` 包名 + TickEvent/LivingTickEvent → EntityTickEvent
- 注册表：`ForgeRegistries` → `BuiltInRegistries`、`RegistryObject` → `DeferredHolder`、`IForgeRegistry` → `Registry`
- 渲染 API：VertexFormatElement 常量、BufferBuilder.vertex→addVertex、Tesselator.begin、时间 API
- Codec API：DFU 6→8 `getOrThrow` 签名变化、`ExtraCodecs.lazyInitializedCodec` 移除、dispatch 泛型
- ResourceLocation 构造器 private → `fromNamespaceAndPath` / `parse`
- ItemStack NBT → DataComponents
- `:1.21.1:compileJava` BUILD SUCCESSFUL

## 关键设计决策

### 逐模块适配器隔离（非全局文本替换）
按 `stonecutter-multiversion-patterns.md` 范式 2「Version Barrier 适配器」：`//?` 注释集中在适配器层（`dataattach/mc/`、`EyelibNetworkTransport`、`Eyelib.java`），业务代码尽量干净。放弃了 `migrateForgeToNeoForge` 全局事后文本替换方案（违背隔离原则）。

### ReflectAccess 工具类
对 final class / protected 字段（PoseStack.Pose 构造器、AvoidEntityGoal.toAvoid），Mixin 无法 target，改用反射（`util/ReflectAccess.java`）。

### Mixin 策略
保留：`PoseStackAccessor`、`LivingEntityAccessor`、`RangedAttackGoalAccessor`（1.20.1 public 字段，1.21.1 protected/private，mixin 在 1.20.1 生效）。
放弃：`PoseAccessor`（PoseStack.Pose 是 final）、`CreeperAccessor`（改用 `getSwelling(1F)` public API）、`AvoidEntityGoalAccessor`（改用反射）、`ModelPartAccessor`（改用反射）。

## 未完成的工作

### 1.21.1 网络功能（完整实现）
`EyelibNetworkTransport.java` 1.21.1 分支完整实现 `CustomPacketPayload` 体系：
- 10 个 packet 类实现 `CustomPacketPayload`（`TYPE` 字段 + `type()` override）
- `EyelibNetworkTransport` 1.21.1 分支：`RegisterPayloadHandlersEvent` + `PayloadRegistrar` 注册 + `StreamCodec.of` 适配参数顺序 + `PacketDistributor` 发送
- `Eyelib.java` 1.21.1 分支：`bus.addListener(EyelibNetworkTransport::onRegisterPayloads)`
- `UniDataUpdatePacket` record 字段 `type` → `attachmentType`（避免与 `CustomPacketPayload.type()` 冲突）

### 1.21.1 运行时验证未做
编译通过 ≠ 运行通过。`runClient` 冒烟未在 1.21.1 node 验证。可能的运行时问题：反射调用、mixin 行为、数据附属序列化。

### 1.21.1 测试
`:1.21.1:test` 编译通过 + 运行：976 测试 / 16 失败 / 9 skip。
- 16 个失败是运行时问题（非编译）：BedrockAddon fixture（9 个，部分与 1.20.1 共享 + 部分新增）、MaterialEndToEndTest RuntimeException（7 个，Material.java Codec/API 运行时差异）
- 需逐个排查运行时差异

### Phase 0 收尾
- ArchUnit 骨架（freeze 模式 + baseline）未引入
- 旧 modid 字符串清理（部分已修：`DataAttachmentContainerCapability` 的 `eyelibattachment` → `eyelib`；其余 `eyelibtrack` 等未处理）
- `BrAnimationEntryDefinition.java:117-186` 反射残留未清理

### Phase 3-6
- Phase 3（ArchUnit 收紧）未开始
- Phase 4（26.1.2 node）未开始
- Phase 5（clientsmoke 多版本化）未开始

## Stonecutter `//?` 注释关键语法（踩坑记录）

- **块条件**：`//? if <1.20.6 { ... //?} else { ... //?}`（闭合标记**必须** `//?}`，不是 `//}`）
- **行条件不支持 else**：`//? if <1.20.6\n<代码>\n//?} else` 是错误语法，必须用块条件
- **`//?` 块内不要放纯注释**：Stonecutter 在 else 分支激活时会剥离注释包裹，纯注释变成裸文本导致编译错误，改用 `throw new UnsupportedOperationException("...")`
- **sourceSet 替换**：Stonecutter 0.7.x centralScript 不自动替换 sourceSet，需在 build.gradle 手动 `sourceSets.main.java.srcDirs = [stonecutterGenerated]`

## 文件改动清单

```
modified:   build.gradle          (centralScript 模式 + 条件化 + sourceSet 替换 + test workingDir)
modified:   gradle.properties     (移除版本特定属性)
modified:   settings.gradle       (Stonecutter 插件 + tree)
new:        stonecutter.gradle    (root 构建脚本)
new:        versions/1.20.1/gradle.properties
new:        versions/1.21.1/gradle.properties
new:        docs/decisions/0015-stonecutter-multi-version.md
new:        docs/superpowers/specs/2026-06-17-stonecutter-migration-design.md
new:        docs/stonecutter-migration-handoff.md  (本文件)
modified:   src/main/resources/eyelib.mixins.json  (加 3 个 accessor mixin)
new:        src/main/java/.../mixin/PoseStackAccessor.java
new:        src/main/java/.../mixin/LivingEntityAccessor.java
new:        src/main/java/.../mixin/RangedAttackGoalAccessor.java
new:        src/main/java/.../util/ReflectAccess.java
modified:   ~40 个源码文件  (//? 条件化 Forge→NeoForge + MC API 差异)
```

**未提交**（按 AGENTS.md 约定，需用户显式要求）。

## 验证命令（JetBrains MCP）

```powershell
# 1.20.1 node 编译 + 测试
jetbrain_run_gradle_tasks taskNames=[":1.20.1:compileJava"]
jetbrain_run_gradle_tasks taskNames=[":1.20.1:test"]  # 3 预存失败

# 1.21.1 node 编译
jetbrain_run_gradle_tasks taskNames=[":1.21.1:compileJava"]

# 切换 active version（编辑 stonecutter.gradle 的 stonecutter.active 行）
# runClient 冒烟
jetbrain_run_gradle_tasks taskNames=["runClient"]
```

## 下次接手优先级

1. **1.21.1 runClient 冒烟**（验证运行时行为，编译通过 ≠ 运行通过）
2. **`:1.21.1:test`**（验证测试通过）
3. **Phase 0 收尾**（ArchUnit 骨架 + 旧 modid 清理 + BrAnimationEntryDefinition 反射残留）
4. **Phase 4**（26.1.2 node — 需 Java 25 + mixin 移除 refmap + L3 渲染路径并行实现）
5. **Phase 3**（ArchUnit 收紧）可与 Phase 4 并行
