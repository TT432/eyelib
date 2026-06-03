# FM-008 解耦方案：Root↔Attachment 耦合消除

> 分析日期：2026-06-02
> 数据来源：实际文件内容审查（全量 grep + 逐文件阅读）
> 背景：MODULES_RELATIONSHIP.md §3.3 / ADR-0005

---

## 0. 耦合全景图

### 0.1 最终验证的耦合统计

通过全量代码审查，Root → `eyelib-attachment` 的实际影响 **13 个文件**，细分如下：

| 耦合类别 | Root 文件 | Import 目标 | 行数 |
|----------|-----------|-------------|:----:|
| **A. 网络包注册** | `network/EyelibNetworkManager.java` | `eyelibattachment.network.*` (6 个 packet) | 22 |
| | `network/NetClientHandlers.java` | `eyelibattachment.network.*` (6 个 handler) | 16 |
| **B. 能力信息 record 引用** | `capability/RenderData.java` | `AnimationComponentInfo`, `ModelComponentInfo` | 2 |
| | `capability/component/ModelComponent.java` | `ModelComponentInfo` | 1 |
| | `client/EntityRenderSystem.java` | `ModelComponentInfo` | 1 |
| | `client/render/controller/RenderControllerEntry.java` | `ModelComponentInfo` | 1 |
| | `client/render/sync/ClientRenderSyncService.java` | `ModelComponentInfo` | 1 |
| | `client/render/sync/RenderSyncApplyOps.java` | `AnimationComponentInfo`, `ModelComponentInfo` | 2 |
| **C. DataAttachmentHelper 工具** | `capability/ExtraEntityUpdateDataRuntimeHooks.java` | `DataAttachmentHelper` | 1 |
| | `capability/RenderData.java` | `DataAttachmentHelper` | 1 |
| | `client/EntityRenderSystem.java` | `DataAttachmentHelper` | 1 |
| | `molang/mapping/MolangQuery.java` | `DataAttachmentHelper` | 1 |
| | `client/manager/ManagerEventLifecycleHooks.java` | `DataAttachmentHelper` | 1 |
| **D. DataAttachmentTypeRegistry 键** | `capability/EyelibAttachableData.java` | `DataAttachmentTypeRegistry` | 1 |
| | `capability/ExtraEntityUpdateDataRuntimeHooks.java` | `DataAttachmentTypeRegistry` | 1 |
| | `molang/mapping/MolangQuery.java` | `DataAttachmentTypeRegistry` | 1 |
| **E. 事件钩子 + Packet 构造** | `capability/ExtraEntityUpdateDataRuntimeHooks.java` | `ExtraEntityUpdateData`, `ExtraEntityUpdateDataPacket` | 2 |
| **F. Mixin → Packet 构造** | `mixin/MultiPlayerGameModeMixin.java` | `UpdateDestroyInfoPacket` | 1 |
| **G. 同步载荷转换** | `client/render/sync/ClientRenderSyncService.java` | `RenderModelSyncPayload` | 1 |
| | `client/render/sync/RenderSyncApplyOps.java` | `RenderModelSyncPayload` | 1 |
| **H. Type 注册** | `capability/EyelibAttachableData.java` | `DataAttachmentType` | 1 |

### 0.2 依赖方向合法性验算

| 方向 | 是否允许 | 依据 |
|------|:--------:|------|
| Root → Attachment | ✅ 允许 | `build.gradle: api project(':eyelib-attachment')` + ADR-0002 |
| Attachment → Root | ❌ 禁止 | ADR-0002 基本规则 |
| Animation → Attachment | ✅ 允许 | `AnimationComponent` 使用 `AnimationComponentInfo`，`build.gradle` 已声明 |
| Model → Attachment | ✅ 允许 | `ModelComponentInfo` 依赖（但 model 仅 1+ import，可忽略） |

**结论**：所有 Root → Attachment 的依赖方向在架构上都是**合法**的。耦合的"债务"本质是三种具体问题：
1. **集中注册瓶颈** — Root 充当中央路由，但 attachment 的 packet 明细却漏在 root（类别 A）
2. **事件/同步逻辑错位** — Forge 事件钩子、mixin、同步转换逻辑在 root，但数据载体在 attachment（类别 E, F, G）
3. **纯类型引用噪声** — DTO/工具类被多处引用，但不形成实质耦合风险（类别 B, C, D, H）

---

## 1. 各类耦合的解耦策略

### 1.1 类别 A: 网络包自注册（最高优先级）

**现状**：
- `EyelibNetworkManager.register()` 硬编码 6 个 attachment packet 的注册调用（`ExtraEntityUpdateDataPacket`, `UniDataUpdatePacket`, `ExtraEntityDataPacket`, `DataAttachmentUpdatePacket`, `DataAttachmentSyncPacket`, `UpdateDestroyInfoPacket`）
- `NetClientHandlers` 中 6 个方法纯委托到 `DataAttachmentSyncRuntime.*`（eyelib-attachment 已拥有的类型）

**策略：SPI 模式 / 消费者回调**

```
EyelibNetworkTransport (eyelib-network) 提供:
  void registerClientPacket(Class<?> type, Encoder, Decoder, Consumer)
  void registerServerPacket(Class<?> type, Encoder, Decoder, BiConsumer)

EyelibAttachmentMod (eyelib-attachment) 在构造时调用:
  EyelibNetworkTransport.registerClientPacket(...) // 6 packets
  EyelibNetworkTransport.registerServerPacket(...)  // 1 packet (UpdateDestroyInfo)

Root 的 EyelibNetworkManager.register() 只保留跨模块的包:
  - ModelComponentSyncPacket (eyelib-model)
  - AnimationComponentSyncPacket (eyelib-animation)
  - SpawnParticlePacket / RemoveParticlePacket (eyelib-particle)
```

**结果**：
- `EyelibNetworkManager.java` 减掉 38 行（6 个 attachment 注册块）
- `NetClientHandlers.java` 减掉 16 行（6 个 delegate 方法 + import）
- Root 不再知道 attachment packet 的细节类型
- 对 `EyelibNetworkTransport` 的要求：支持模块启动时注册（Forge `FMLConstructModEvent` 阶段）

**风险等级：低** — 纯添加逻辑，不改变现有数据流

**⚠️ 注意**：`UpdateDestroyInfoPacket` 的 server handler `DataAttachmentSyncRuntime::handleDestroyInfoUpdate` 已在 eyelib-attachment 中，无需额外迁移。

---

### 1.2 类别 B: 能力信息 record（可推迟）

**现状**：
- `ModelComponentInfo` — record(String model, ResourceLocation texture, ResourceLocation renderType)，**纯数据 DTO，无行为方法**
- `AnimationComponentInfo` — record(Map<String, String> animations, Map<String, MolangValue> animate)，**纯数据 DTO，无行为方法**
- 使用模式：构造 `new ModelComponentInfo(...)`、读取 `.model()`/`.texture()`/`.renderType()`

**策略：保持原状，不做接口抽象**

理由：
1. 这些是纯数据类型，接口化不会带来多态收益（没有需要覆盖的实现）
2. Root → Attachment 的依赖方向是合法的
3. `AnimationComponent`（eyelib-animation）对 `AnimationComponentInfo` 的依赖也是合法的（animation → attachment）

**可选的未来优化**（非 FM-008 范围）：
- 如果 future phase 需要消除 animation → attachment 依赖，可以：
  - 将 `AnimationComponentInfo` 移动到 eyelib-util（最底层）
  - 或定义独立 SPI 模块

**风险等级：低** — 无技术债务紧急度，仅为计数噪声

---

### 1.3 类别 C: DataAttachmentHelper 工具方法（低优先级）

**现状**：
- 5 个 root 文件使用 `DataAttachmentHelper.getOrCreate()` / `.setLocal()`
- 这是 eyelib-attachment 提供的**公开 API**，Root 是其合法客户端

**策略：保持原状**

理由：
1. `DataAttachmentHelper` 是 attachment 模块的**公开工具 API**，不是内部细节
2. Root → Attachment 合法
3. 提取为接口层不会减少 import 数量

**风险等级：低** — 这是设计意图内的使用

---

### 1.4 类别 D: DataAttachmentTypeRegistry 键（意图内）

**现状**：
- `EyelibAttachableData` — Root 通过 registry 注册其数据类型的 `DataAttachmentType`，这是**正确的归属模式**
- `ExtraEntityUpdateDataRuntimeHooks`、`MolangQuery` — 通过 registry 获取类型键来读取/写入数据

**策略：保持原状**

理由：
1. `EyelibAttachableData` 注册 Root 域类型（`RenderData`, `ItemInHandRenderData`, `EntityBehaviorData`），这是 Root 的责任
2. 读取 registry 键是客户端代码的正常行为

**风险等级：低**

---

### 1.5 类别 E: ExtraEntityUpdateDataRuntimeHooks（中优先级，可移动）

**现状**：
- 一个 Forge `@Mod.EventBusSubscriber`，监听 `LivingDamageEvent` + `LivingTickEvent`
- 构造 `ExtraEntityUpdateData`、发送 `ExtraEntityUpdateDataPacket`
- 引用 `DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE` 键、`EyelibNetworkTransport`、`DataAttachmentHelper`

**策略：下沉到 eyelib-attachment**

可行性分析：
| 依赖 | 在 eyelib-attachment 是否可用 |
|------|:---:|
| `ExtraEntityUpdateData` | ✅ 已有（attachment.capability） |
| `ExtraEntityUpdateDataPacket` | ✅ 已有（attachment.network） |
| `DataAttachmentHelper` | ✅ 已有（attachment.dataattach.mc） |
| `DataAttachmentTypeRegistry` | ✅ 已有（attachment.dataattach.mc） |
| `EyelibNetworkTransport` | ✅ 已有（eyelib-networt → compile dependency） |
| `LivingDamageEvent` (Forge) | ✅ Forge mod 事件正常 |

**操作步骤**：
1. 将 `ExtraEntityUpdateDataRuntimeHooks` 复制到 `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/runtime/`
2. 改为 `eyelibattachment` 的 `@Mod.EventBusSubscriber(modid = "eyelibattachment")`
3. 删除 Root 同名文件
4. 更新 `MODULES_RELATIONSHIP.md`

**风险等级：低** — 纯文件搬迁，逻辑不变

---

### 1.6 类别 F: MultiPlayerGameModeMixin（高复杂度，考虑移动到 attachment 或简化）

**现状**：
- Root mixin 在 `MultiPlayerGameMode.startDestroyBlock`/`stopDestroyBlock`/`continueDestroyBlock` 中注入 `UpdateDestroyInfoPacket`
- 使用 `EyelibNetworkManager.sendToServer()` 发送

**策略选项**：

**选项 F-1：下沉 Mixin → eyelib-attachment（推荐）**
- 将 mixin 类移动到 `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/mixin/`
- 在 `eyelib-attachment` 的 `build.gradle` 中添加 mixin 配置
- 注意：eyelib-attachment 需要 access `EyelibNetworkManager.sendToServer()`（这个是 static 的）

**问题**: `EyelibNetworkManager.sendToServer()` 在 root，attachment 不能依赖 root。

**解决**：将 `sendToServer()` 下沉到 `EyelibNetworkTransport.INSTANCE.sendToServer()` 并让 eyelib-attachment 直接使用（eyelib-network 已经是 attachment 的依赖）

或者：直接使用 `EyelibNetworkTransport.sendToServer(new UpdateDestroyInfoPacket(...))`，绕过 `EyelibNetworkManager`。

**选项 F-2：保留在 Root（次选）**
- 接受这是一个混合的 Mixin 技术限制
- Mixin 需要 refmap 配置，搬迁到子模块需要额外的 Gradle 设置

**推荐：F-1**，因为：
- `UpdateDestroyInfoPacket` 是 attachment 的包类型，mixin 应与其所在模块一致
- 搬迁后，`EyelibNetworkManager` 对 attachment 的不均匀注册调用归零

**风险等级：中** — Mixin 搬迁需处理 `refmap`、`build.gradle` mixin 配置、`sendToServer` 路由更改

---

### 1.7 类别 G: 同步载荷转换（中优先级）

**现状**：
- `ClientRenderSyncService.decodeModelPayload()` 将 `RenderModelSyncPayload` → `ModelComponentInfo`
- `RenderSyncApplyOps.collectSerializableModelInfo()` 将 `ModelComponentInfo` → `RenderModelSyncPayload`
- `RenderModelSyncPayload.from(ModelComponentInfo)` 在 attachment 中已存在

**策略选项**：

**选项 G-1: 将转换方法移到 eyelib-attachment**
- 在 `RenderModelSyncPayload` 中添加 `ModelComponentInfo toInfo()` 方法
- 在 `ModelComponentInfo` 中添加 `RenderModelSyncPayload toPayload()` 方法（或反向）
- Root 仅调用 `payload.toInfo()` 即可

**选项 G-2: 保持原状**
- 转换逻辑简单（3 字段互转），额外抽象开销 > 收益

**推荐：G-1**，因为：
- 消除 2 个 root 文件对 `ModelComponentInfo` / `RenderModelSyncPayload` 的直接构造
- `toInfo()` / `toPayload()` 方法天然属于协议 payload 的定义所在模块
- 更改量极小（2 个方法 + 3 行调用方替换）

**风险等级：低**

---

### 1.8 类别 H: DataAttachmentType 注册（意图内，不处理）

**现状**：
- `EyelibAttachableData` 注册 Root 域的类型到 `DataAttachmentTypeRegistry`

**策略：保持原状**

理由：Root 拥有自己的域类型，负责通知 attachment 系统"这些类型应该被管理"。这是正确的 Inversion of Control。

**风险等级：零**

---

### 1.9 特例: MolangQuery（不处理）

**现状**：
- `MolangQuery` 在 Root 的 `molang/mapping/` 包下，引用 `DataAttachmentHelper` + `DataAttachmentTypeRegistry`
- AGENTS.md §109 明确标注：*"Root-coupled query functions that cannot move to `eyelib-molang`."*
- 原因：MolangQuery 依赖 Root 域类型（`Creeper`, `WitherBoss`, `Entity`, `LivingEntity`, `AttachableResolver`），这些在 eyelib-molang 中不可用

**策略：保持原状**

**可选的减负**：如果只想减少 eyelib-attachment 的 import，可以将 attachment 相关的 6 个 query 函数抽取到一个单独的 `MolangAttachmentQuery` 类中（仍留在 root 包下），让 `MolangQuery` 不再 import `DataAttachmentHelper`。但这只是 import 噪声移动，不改架构实质。

**风险等级：零**

---

## 2. 分阶段实施顺序

### 阶段 0: 网络包自注册（当前 Sprint）

| 任务 | 影响文件 | 预估量 |
|------|:--------:|:------:|
| `EyelibNetworkTransport` 添加 SPI 注册能力 | `eyelib-network` | ~20 行 |
| `EyelibAttachmentMod` 中注册 self-registrations | `eyelib/attachment/EyelibAttachmentMod.java` (new) | ~40 行 |
| 从 `EyelibNetworkManager` 删除 6 个 attachment 注册块 | `root/network/EyelibNetworkManager.java` | -38 行 |
| 从 `NetClientHandlers` 删除 6 个 delegate | `root/network/NetClientHandlers.java` | -16 行 |

**依赖约束**：无前置依赖

**风险等级：低**

### 阶段 1: 事件钩子 + Mixin 搬迁（同一 Sprint 或下一 Sprint）

| 任务 | 影响文件 | 预估量 |
|------|:--------:|:------:|
| 搬迁 `ExtraEntityUpdateDataRuntimeHooks` → eyelib-attachment | `root/capability/ExtraEntityUpdateDataRuntimeHooks.java` (delete) + `eyelib-attachment/runtime/ExtraEntityUpdateDataRuntimeHooks.java` (new) | ~94 行搬迁 |
| 搬迁 `MultiPlayerGameModeMixin` → eyelib-attachment | `root/mixin/MultiPlayerGameModeMixin.java` (delete) + `eyelib-attachment/mixin/MultiPlayerGameModeMixin.java` (new) | ~76 行搬迁 |
| 更新 `eyelib-attachment` 的 `build.gradle` mixin 配置 | `eyelib-attachment/build.gradle` | ~3 行 |
| 更新 `sendToServer` 调用路径（绕过 `EyelibNetworkManager`） | eyelib-attachment mixin 内部 | ~5 处替换 |

**依赖约束**：建议阶段 0 完成后（但非必须）

**风险等级：中**（Mixin 搬迁需要测试验证）

### 阶段 2: 同步载荷转换减负（可选，低优先级）

| 任务 | 影响文件 | 预估量 |
|------|:--------:|:------:|
| 在 `RenderModelSyncPayload` 中添加 `toInfo()` | `eyelib-attachment/sync/RenderModelSyncPayload.java` | ~3 行 |
| `ClientRenderSyncService` 用 `payload.toInfo()` 替代直接构造 | `root/client/render/sync/ClientRenderSyncService.java` | -1 行 +1 行 |
| `RenderSyncApplyOps.collectSerializableModelInfo` 用 `info.toPayload()` | `root/client/render/sync/RenderSyncApplyOps.java` | -1 行 +1 行 |

**依赖约束**：无

**风险等级：低**

### 不处理的耦合（有意保留）

| 耦合项 | 理由 |
|--------|------|
| `ModelComponentInfo` / `AnimationComponentInfo` 引用 | 纯 DTO，合法依赖方向，接口化无意义 |
| `DataAttachmentHelper` 调用 | 公开 API，合法使用 |
| `DataAttachmentTypeRegistry` 键读取 | 实现细节不应当被隐藏 |
| `EyelibAttachableData` 类型注册 | Root 域类型的注册是 Root 责任 |
| `MolangQuery` 中的 attachment 引用 | AGENTS.md 明确标记为 root 保留 |

---

## 3. 阶段依赖图

```
阶段 0 (网络自注册)
  └── 无前置依赖
  ├── 产出: Root 不再引用任何 eyelibattachment.network.* 包
  └── 可单独交付

阶段 1 (事件+Mixin搬迁)
  ├── 前置: 强烈建议阶段 0 先行（Mixin 中 sendToServer 路径更改）
  ├── 产出: Root 不再持有 attachment-specific 的事件钩子和 Mixin
  └── 需测试: Mixin + Forge 事件在子模块中正常工作

阶段 2 (同步载荷减负)
  ├── 前置: 无
  ├── 产出: 转换逻辑与数据定义同模块
  └── 可随时交付
```

所有阶段可独立交付，不构成阻塞链。

---

## 4. 解耦后的 Root→Attachment 依赖指纹

### 解耦前 Root→Attachment import 分布（13 文件）

```
capability/ExtraEntityUpdateDataRuntimeHooks.java    → 4 import  (ATTACHMENT)
capability/EyelibAttachableData.java                 → 2 import  (ATTACHMENT)
capability/RenderData.java                           → 3 import
capability/component/ModelComponent.java             → 1 import
client/EntityRenderSystem.java                       → 2 import
client/manager/ManagerEventLifecycleHooks.java       → 1 import
client/render/controller/RenderControllerEntry.java  → 1 import
client/render/sync/ClientRenderSyncService.java      → 2 import
client/render/sync/RenderSyncApplyOps.java           → 3 import
mixin/MultiPlayerGameModeMixin.java                  → 1 import  (ATTACHMENT)
molang/mapping/MolangQuery.java                      → 2 import
network/EyelibNetworkManager.java                    → 6+ import (wildcard, ATTACHMENT)
network/NetClientHandlers.java                       → 6+ import (wildcard, ATTACHMENT)
```

### 解耦后（阶段 0+1+2 后）

```
capability/EyelibAttachableData.java                 → 2 import  (保留: 注册 Root 域类型)
capability/RenderData.java                           → 3 import  (保留: 合法 API 使用)
capability/component/ModelComponent.java             → 1 import  (保留: 合法 DTO 引用)
client/EntityRenderSystem.java                       → 2 import  (保留: 合法 API 使用)
client/manager/ManagerEventLifecycleHooks.java       → 1 import  (保留: 合法 API 使用)
client/render/controller/RenderControllerEntry.java  → 1 import  (保留: 合法 DTO 引用)
client/render/sync/ClientRenderSyncService.java      → 1 import  (从 payload 转换减负)
client/render/sync/RenderSyncApplyOps.java           → 2 import  (从 payload 转换减负)
molang/mapping/MolangQuery.java                      → 2 import  (保留: AGENTS.md 限制)
```

**解耦效果**：
- **删除文件**: 4 个文件完全消除 attachment import（ExtraEntityUpdateDataRuntimeHooks, MultiPlayerGameModeMixin, EyelibNetworkManager, NetClientHandlers）
- **保留文件**: 9 个文件的 import 保持（均为合法 API/DTO 使用）
- **import 减少**: ~25 处 → ~15 处（约 40% 减少）
- **类型耦合消除**: 6 个 attachment packet 类型、`UpdateDestroyInfoPacket`、`ExtraEntityUpdateData`、`ExtraEntityUpdateDataPacket`、`RenderModelSyncPayload` 的类型耦合全部消除

---

## 5. 实施后验证标准

| 检查项 | 验证方法 |
|--------|----------|
| Root 不再 import `eyelibattachment.network.*` | `grep "eyelibattachment.network" src/main/java/` → 零结果 |
| Root 不再 import `ExtraEntityUpdateData*` | `grep "ExtraEntityUpdateData" src/main/java/` → 零结果 |
| Root 不再 import `UpdateDestroyInfoPacket` | 同上 |
| `NetClientHandlers` 中无 attachment 委托方法 | 检查文件 |
| `EyelibNetworkManager.register()` 中无 attachment 注册 | 检查文件 |
| 编译通过 | `jetbrain_build_project` |
| 客户端包注册正常工作 | 运行内建测试或手动验证 |

---

## 6. 风险总结

| 阶段 | 风险等级 | 主要风险点 | 缓解措施 |
|:----:|:--------:|-----------|----------|
| 0 | 🟢 低 | 模块启动时机（FMLConstructModEvent 中注册是否可用） | 使用 Forge `ModEventBus` 的构造阶段 |
| 1 | 🟡 中 | Mixin refmap 配置需要调整；`EyelibNetworkManager.sendToServer` 路径需替换 | 先在 Dev 环境完整测试，再提交 |
| 2 | 🟢 低 | 更改量极小 | 代码审查即可 |

---

## 7. 文件更改清单总表

| 阶段 | 操作 | 文件路径 |
|:----:|:----:|----------|
| 0 | 修改 | `eyelib-network/src/main/java/.../EyelibNetworkTransport.java` |
| 0 | 新增 | `eyelib-attachment/src/main/java/.../network/AttachmentPacketRegistration.java` |
| 0 | 修改 | `eyelib-attachment/src/main/java/.../EyelibAttachmentMod.java` |
| 0 | 删除 | `src/main/java/.../network/EyelibNetworkManager.java` (attachment 块) |
| 0 | 删除 | `src/main/java/.../network/NetClientHandlers.java` (attachment 块) |
| 1 | 新增 | `eyelib-attachment/src/main/java/.../runtime/ExtraEntityUpdateDataRuntimeHooks.java` |
| 1 | 删除 | `src/main/java/.../capability/ExtraEntityUpdateDataRuntimeHooks.java` |
| 1 | 新增 | `eyelib-attachment/src/main/java/.../mixin/MultiPlayerGameModeMixin.java` |
| 1 | 删除 | `src/main/java/.../mixin/MultiPlayerGameModeMixin.java` |
| 1 | 修改 | `eyelib-attachment/build.gradle` (mixins 配置) |
| 2 | 修改 | `eyelib-attachment/src/main/java/.../sync/RenderModelSyncPayload.java` |
| 2 | 修改 | `src/main/java/.../client/render/sync/ClientRenderSyncService.java` |
| 2 | 修改 | `src/main/java/.../client/render/sync/RenderSyncApplyOps.java` |
| 全部 | 更新 | `MODULES_RELATIONSHIP.md` |

总计：约 **13 个文件**涉及更改（5 个新增/删除，8 个修改），约 **250-300 行**净删改量。
