# Phase 30: CAP — Capability 残留审计与最终迁移 — REPORT

**Date:** 2026-05-12
**Status:** Complete — 0 类型可安全迁移，全部保留/推迟有据

## 1. Root `capability/` 全量分类

共 **10 个 Java 类型**（含 component/ 子目录）：

### 1.1 必须保留 (7 类型)

| 类型 | 保留原因 | 依赖链 |
|------|----------|--------|
| **EyelibAttachableData** | PIT-03: Forge `DeferredRegister` registry hub，`@Mod.EventBusSubscriber` | 注册全部 6 个 attachment 类型，是 root 与 attachment 之间的桥接 |
| **RenderData\<T\>** | Deep MC耦合：`Entity`, `DataAttachmentHelper`；root component 依赖：`AnimationComponent`, `ModelComponent`, `RenderControllerComponent`, `ClientEntityComponent` | `RenderData.codec()` 直接序列化 attachment 的 `ModelComponentInfo.CODEC` 和 `AnimationComponentInfo.CODEC` |
| **ItemInHandRenderData** | 依赖 `RenderData<ItemStack>` (root)，`LivingEntity`, `ItemStack` (MC)，`MolangScope` (Molang) | `init(LivingEntity, RenderData<?>)` 直接操作 `entity.getMainHandItem()` |
| **AnimationComponent** (component/) | 运行时状态持有者：`animate` HashMap, `animationData`, tick状态 | 不可序列化到 attachment；是 DTO-vs-运行时分离的运行时侧 |
| **ModelComponent** (component/) | 运行时状态：`modelInfos`, `partVisibility`, render type 解析 | 同上 |
| **ClientEntityComponent** (component/) | 运行时客户端实体状态 | 同上 |
| **RenderControllerComponent** (component/) | 运行时渲染控制器槽位状态 | 同上 |

### 1.2 需推迟 (1 类型)

| 类型 | 推迟原因 | 阻塞分析 |
|------|----------|----------|
| **EntityBehaviorData** | PIT-04: "EntityBehaviorData codec extraction deferred to post-v1.5" | 导入 `BehaviorEntity`, `ComponentGroup`, `Component` (root `common.behavior`)。迁移需要同时移动整个 `common.behavior` 包族。内部维护 mutable `HashMap<Class, Component>` 状态。STATE.md CAP-F01 记录 |

### 1.3 安全可迁 (0 类型)

v1.4 已迁移的 5 个类型为当前阶段所有可安全提取的类型。

## 2. v1.4 已迁移类型位置确认

| 已迁移类型 | Attachment 位置 | 状态 |
|-----------|----------------|------|
| `AnimationComponentInfo` | `eyelib-attachment/.../capability/AnimationComponentInfo.java` | ✅ 正确 |
| `ModelComponentInfo` | `eyelib-attachment/.../capability/ModelComponentInfo.java` | ✅ 正确 |
| `ExtraEntityData` | `eyelib-attachment/.../capability/ExtraEntityData.java` | ✅ 正确 |
| `ExtraEntityUpdateData` | `eyelib-attachment/.../capability/ExtraEntityUpdateData.java` | ✅ 正确 |
| `EntityStatistics` | `eyelib-attachment/.../capability/EntityStatistics.java` | ✅ 正确 |

全部 5 个类型在 attachment 模块位置正确。`EyelibAttachableData` 引用它们通过完全限定名（如 `io.github.tt432.eyelibattachment.capability.ExtraEntityData`），无回归。

## 3. EyelibAttachableData 确认

- 文件: `src/main/java/io/github/tt432/eyelib/capability/EyelibAttachableData.java`
- 角色: Forge `DeferredRegister` hub + `RegistryBuilder`
- 注册全部 6 个 `DataAttachmentType<?>`: RENDER_DATA, ENTITY_STATISTICS, EXTRA_ENTITY_UPDATE, EXTRA_ENTITY_DATA, ITEM_IN_HAND_RENDER_DATA, ENTITY_BEHAVIOR_DATA
- **确认**: 正确保留在 root，未被错误迁移

## 4. 编译验证

- `jetbrain_build_project` → BUILD SUCCESSFUL, 0 problems ✅
- 无代码修改，只读审计
