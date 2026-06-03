# Eyelib 模块依赖关系图

> 生成日期: 2026-06-02
> 项目: qylEyelib — Minecraft Forge 多模块渲染库
> 子模块数: 11 (+ 1 composite build: clientsmoke)

---

## 1. 模块概览

| 子模块 | 包前缀 | 角色描述 |
|--------|--------|----------|
| `eyelib-util` | `io.github.tt432.eyelibutil` | 基础工具库（数学、集合、资源路径、颜色编码等） |
| `eyelib-molang` | `io.github.tt432.eyelibmolang` | Molang 表达式引擎、编译器、类型系统 |
| `eyelib-model` | `io.github.tt432.eyelibmodel` | 规范模型数据类型、定位树、骨骼 ID 映射 |
| `eyelib-network` | `io.github.tt432.eyelibnetwork` | 网络传输层（纯管道，独立） |
| `eyelib-attachment` | `io.github.tt432.eyelibattachment` | 数据附加系统、流编解码工具 |
| `eyelib-importer` | `io.github.tt432.eyelibimporter` | 导入器拥有的 schema、codec、归一化、模型/实体/动画导入 |
| `eyelib-material` | `io.github.tt432.eyelibmaterial` | Bedrock 材质定义与 GL 状态管理 |
| `eyelib-animation` | `io.github.tt432.eyelibanimation` | 动画运行时 |
| `eyelib-behavior` | `io.github.tt432.eyelibbehavior` | Bedrock 实体行为组件模型和运行时 |
| `eyelib-particle` | `io.github.tt432.eyelibparticle` | 粒子模块 API、核心契约、集成接缝 |
| `eyelib-track` | `io.github.tt432.eyelibtrack` | ItemStack 追踪与每实例动画状态 |

|---

> 编译期依赖关系以各子模块 build.gradle 的 project(:) 边定义为权威来源。此处不重复。



## 3. 运行时 import 耦合矩阵

### 3.1 矩阵表

| 源 ↓ \ 目标 → | anim | behavior | model | network | attach | importer | material | molang | particle | track | util |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **root** | 45 | 16 | 32 | 5 | 25 | 42 | 7 | 17 | 18 | 2 | 11 |
| **animation** | — | 0 | 4+ | 0 | 2+ | ~15 | 0 | ~15 | 0 | 0 | ~12 |
| **behavior** | 0 | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | ~4 |
| **model** | 0 | 0 | — | 0 | 1+ | 0 | 0 | 0 | 0 | 0 | 2+ |
| **network** | 0 | 0 | 0 | — | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| **attachment** | 0 | 0 | 0 | 1 | — | 0 | 0 | 1 | 0 | 0 | ~12 |
| **importer** | 0 | 0 | ~8 | 0 | 0 | — | 1 | ~12 | 0 | 0 | 0 |
| **material** | 0 | 0 | 0 | 0 | 0 | 0 | — | 0 | 0 | 0 | 2 |
| **molang** | 0 | 0 | 0 | 0 | 0 | 0 | 0 | — | 0 | 0 | 1 |
| **particle** | 0 | 0 | 0 | 0 | 0 | ~5 | 1 | ~30 | — | 0 | 1 |
| **track** | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | — | 0 |
| **util** | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | — |

> 数值为 import 语句的近似计数。`+` 表示额外引用（如枚举常量、序列化引用）。
> ✓ 表示实际 import 完全符合 build.gradle 声明。
> ⚠ 表示不一致。

### 3.2 Root → 子模块 import 分布详情

Root 模块 137 个 Java 文件中，**77 个文件** 至少 import 了一个子模块。

| 子模块 | Import 行数 | 涉及文件数 | 引用最多的文件 |
|--------|:---------:|:---------:|----------------|
| eyelib-animation | 45 | ~30 | EntityRenderSystem, AttachableItemRenderSetup, ItemInHandRendererMixin |
| eyelib-importer | 42 | ~20 | ManagerResourceImportPlanner, BedrockAddonRuntimeBridge, ModelPreviewScreen |
| eyelib-model | 32 | ~20 | DFSModel, ModelPartModel, RenderControllerEntry, 各 bake info |
| eyelib-attachment | 25 | 17 | ExtraEntityUpdateDataRuntimeHooks, RenderData, RenderSyncApplyOps |
| eyelib-particle | 18 | 6 | ParticleSpawnService, BrParticleLoader, EyelibNetworkManager |
| eyelib-molang | 17 | 8 | MolangQuery, RenderControllerEntry, ItemTrackRenderer, ParticleSpawnService |
| eyelib-behavior | 16 | 4 | EntityRenderSystem, BehaviorEntityAssetRegistry, behavior/EntityBehaviorData |
| eyelib-util | 11 | 7 | RenderParams, Manager, ModelPartModel, MolangQuery |
| eyelib-material | 7 | 4 | ModelComponent, ManagerResourceImportPlanner, BrMaterialLoader |
| eyelib-network | 5 | 3 | EyelibNetworkManager (intra-root), ExtraEntityUpdateDataRuntimeHooks, EyelibParticleCommand |
| eyelib-track | 2 | 1 | ItemTrackRenderCache |

### 3.3 eyelib-attachment 在 Root 中被引用的完整清单

17 个文件，25 处 import：

| Root 文件 | Import 目标 |
|-----------|------------|
| `capability/ExtraEntityUpdateDataRuntimeHooks.java` | `ExtraEntityUpdateData`, `DataAttachmentHelper`, `DataAttachmentTypeRegistry`, `ExtraEntityUpdateDataPacket` |
| `capability/EyelibAttachableData.java` | `DataAttachmentType`, `DataAttachmentTypeRegistry` |
| `capability/RenderData.java` | `AnimationComponentInfo`, `ModelComponentInfo`, `DataAttachmentHelper` |
| `capability/component/ModelComponent.java` | `ModelComponentInfo` |
| `client/EntityRenderSystem.java` | `ModelComponentInfo`, `DataAttachmentHelper` |
| `client/particle/ParticleSpawnService.java` | `DataAttachmentHelper` |
| `client/render/controller/RenderControllerEntry.java` | `ModelComponentInfo` |
| `client/render/sync/ClientRenderSyncService.java` | `ModelComponentInfo`, `RenderModelSyncPayload` |
| `client/render/sync/RenderSyncApplyOps.java` | `AnimationComponentInfo`, `ModelComponentInfo`, `RenderModelSyncPayload` |
| `mixin/MultiPlayerGameModeMixin.java` | `UpdateDestroyInfoPacket` |
| `molang/mapping/MolangQuery.java` | `DataAttachmentHelper`, `DataAttachmentTypeRegistry` |
| `network/EyelibNetworkManager.java` | `eyelibattachment.network.*` (wildcard) |
| `network/NetClientHandlers.java` | `eyelibattachment.network.*` (wildcard) |

### 3.4 幽灵依赖 / 废弃依赖

| 模块 | 类型 | 说明 |
|------|------|------|
| ~~**eyelib-track**~~ | ~~**废弃依赖**~~ | ~~`build.gradle` 声明依赖 `attachment`, `util`, `molang`，但实际 Java 代码未 import 这 3 个模块中的任何类（所有 import 均为自身包内引用） — ✅ 已于 2026-06-02 清理~~ |

**幽灵依赖（import 了但 build.gradle 未声明）: 未发现。**

所有子模块的实际 import 都符合其 build.gradle 声明。eyelib-track 的多余声明已于 2026-06-02 清理完成，当前无任何废弃依赖。

### 3.5 逆向依赖检查（子模块 → Root）

**结果：无违规。**

所有子模块的 `import io.github.tt432.eyelib*` 语句都解析到已知的 11 个子模块包中，没有子模块 import `io.github.tt432.eyelib` 根包下的任何类。

---

## 4. 债务项映射 (ADR-0005)

### FM-004: ParticleSpawnService ✅ 已消除

- **原文件**: `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` — 已删除
- **处理方式**: 逻辑下沉到 `ParticleSpawnRuntimeAdapter`（eyelib-particle 模块）
  - `currentEnvironment()` / `currentParentScope()` → 通过 `ParticleSpawnRuntimeAdapter.configure()` 在 `ManagerEventLifecycleHooks.onClientSetup()` 中注入
  - `MinecraftParticleRuntimeEnvironment` → 提取为 root 模块的独立 record
  - `NetClientHandlers` → 直接调用 `ParticleSpawnRuntimeAdapter.INSTANCE.spawn() / .remove()`
  - `RootAnimationParticleSpawner` → 接受 `ParticleSpawnApi` 构造函数参数，由调用者传入 `ParticleSpawnRuntimeAdapter.INSTANCE`
- **结果**: 消除了跨 `particle` + `attachment` 的编排器，`ParticleSpawnRuntimeAdapter` 现在直接持有环境/作用域的 supplier

### FM-008: Root 附属包耦合 🔄 解耦中 (2026-06-02)

- **问题**: Root 文件中有大量 `eyelib-attachment` 的 import
- **已完成的解耦**:
  - ✅ **阶段 0 (网络包自注册)**: `EyelibNetworkManager`/`NetClientHandlers` 中的 6 个 attachment packet 注册→已迁移到 `EyelibAttachmentMod.regiserNetworkPackets()`
  - ✅ **阶段 1 (事件+Mixin搬迁)**: `ExtraEntityUpdateDataRuntimeHooks` + `MultiPlayerGameModeMixin` → 搬迁到 `eyelib-attachment` 模块
  - ✅ **阶段 2 (同步载荷转换)**: `RenderModelSyncPayload.toInfo()` 方法封装 payload ↔ ModelComponentInfo 转换
- **保留的耦合（合法 API/DTO 使用）**:
  1. `ModelComponentInfo` / `AnimationComponentInfo` — 纯 DTO，接口化无收益
  2. `DataAttachmentHelper` — 公开 API，合法使用
  3. `DataAttachmentTypeRegistry` — Root 域类型注册本身是 Root 责任
  4. `MolangQuery` — AGENTS.md 明确标记为 root 保留
- **当前影响**: ~9 个 root 文件保留合法 attachment 引用（共 17→9，约 47% 减少）

### FM-014: 共享网络通道

- **文件**: `src/main/java/io/github/tt432/eyelib/network/EyelibNetworkManager.java`, `NetClientHandlers.java`
- **现状**: Root 模块的网络包 (`io.github.tt432.eyelib.network`) 作为中央注册中心，为 4 个子模块注册和分发网络包
| **注册的包类型**:
  - `eyelib-animation`: `AnimationComponentSyncPacket`
  - `eyelib-model`: `ModelComponentSyncPacket`
  - `eyelib-particle`: `SpawnParticlePacket`, `RemoveParticlePacket`
- **自注册的包**: `eyelib-attachment` 的 6 个 packet 已移出 root 注册中心，改由 `EyelibAttachmentMod` 在构造阶段自注册
- **网络传输层**: 通过 `eyelib-network` 子模块 (`io.github.tt432.eyelibnetwork.EyelibNetworkTransport`) 提供底层通道
- **结构**: Root 拥有注册/分发，子模块拥有具体协议包实体——"稳定"状态

### FM-015: Accessor

- **文件**: `src/main/java/io/github/tt432/eyelib/mixin/LivingEntityRendererAccessor`（根据 ADR 提及）
- **现状**: Root 的 `mixin/` 包下存活的 accessor mixin。由于 Mixin 需要特定的 refmap 和配置注册，留在 root 中具技术合理性
- **状态**: 稳定，当前不被视为重构优先级

---

## 5. 违反规则清单

| 规则 | 状态 | 详情 |
|------|------|------|
| 子模块不能依赖 Root | ✅ 通过 | 无子模块 import `io.github.tt432.eyelib` 根包 |
| import 与 build.gradle 一致 | ✅ 已解决 | `eyelib-track` 的废弃依赖已在 2026-06-02 清理 |
| eyelib-preprocessing 已废弃 | ✅ 已确认 | `settings.gradle` 中无此模块 |
| 名称空间一致性 | ✅ 统一 | 所有子模块使用连接式包名（`eyelibmolang` 非 `eyelib.molang`） |

---

## 6. 关键发现总结

1. **eyelib-util** 是整个依赖树的基石——10 个非 util 模块中有 7 个直接依赖它
2. **eyelib-network** 是完全独立的子模块，没有任何内部 eyelib 依赖
3. **eyelib-track** 的 3 个未使用的 Gradle 依赖声明（`attachment`, `util`, `molang`）已于 2026-06-02 清理
4. **Root 模块** 广泛依赖所有子模块（77/137 文件），其中 `animation`, `importer`, `model` 为前三
5. **FM-004** (ParticleSpawnService) ✅ 已消除 — 逻辑下沉到 `ParticleSpawnRuntimeAdapter` 的静态 `configure()` 注入，原文件已删除
6. **FM-008** 已解耦（Phase 0-2 完成）：17 个 root 文件 → 9 个文件保留合法 API/DTO 引用（-47%）
7. **逆向依赖为零**——DDD 重构 Phase 3 确保了这一基本边界
