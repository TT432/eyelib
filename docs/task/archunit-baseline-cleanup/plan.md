# ArchUnit Baseline 全量清零 + 预存问题修复

## 任务范围（用户决策）

用户明确选择：
1. **I-5 新违规**：沉积全消掉（不只 2 条，全部 177+ 条）
2. **baseline 沉积**：全量修复（分批但本任务内完成）
3. **预存问题**：两个都纳入（ModelPartModelInterfaceTest + 26.1.2 node）

## 测试结构（src/test/java/io/github/tt432/eyelib/architecture/）

- **ArchitectureTest.java**：9 条规则用 FreezingArchRule freeze，baseline 按 `${project.name}` 隔离存于 `build/archunit_store/<version>/`，UUID 文件存违规描述。`build.gradle:504` 配 `archunit.freeze.store.default.path`；自定义 `IgnoreLineNumberViolationMatcher` 忽略行号。
- **IqfSourceScanRulesTest.java**：源码扫描，baseline 硬编码 `build/archunit_store/iqf-*-baseline.txt`。
- **StonecutterCommentPlacementTest.java**：扫描 `//?` 出现在非 bridge/mixin/smoke/debug 包的违规。

### stored.rules UUID → 规则映射（1.20.1）

| UUID 前缀 | 规则 | baseline 大小 | 违规数 |
|---|---|---|---|
| `f94b8188` | 规则 8 aclPublicApiMustBeInterfaceOrAnnotation | 10008B | 82 |
| `7996bcad` | 规则 4 aclMustNotDependOnApplication | 2277B | 8 |
| `4f6a37ea` | I-2 domainMustNotUseSingletonInstance | 150B | 6 |
| `be980ffb` | I-5 applicationMustNotDependOnBridgeConcreteClasses | 60074B | 214 |
| `90464bbb` | 规则 2 domainMustNotDependOnOrchestration | 0B | 0 |
| `04dacba2` | 规则 3 versionSpecificMcOnlyInBridgeOrInfrastructure | 0B | 0 |
| `107fcaac` | 规则 1 domainMustNotDependOnMinecraft | 0B | 0 |
| `ce46cfba` | 规则 6 applicationMustNotDependOnDomainInternals | 0B | 0 |

**规则 1/2/3/6 baseline 为空（0 违规），无需处理。** 总计需清零：规则 4(8) + 规则 8(82) + I-2(6) + I-5(214) + Stonecutter(48文件) + iqf-static(1) + iqf-lazy(1) = 360 条违规。

## 规则 4 违规清单（8 条，aclMustNotDependOnApplication）

bridge → application 的反向依赖：

1. `bridge.Eyelib.<init>` → `network.EyelibNetworkManager.register()`
2. `bridge.client.render.bake.ModelBakeInfo.modelCache` 泛型含 `client.render.bake.BakedModel`
3. `bridge.CommonEntityEventHandler.onServerAboutToStart` → `common.behavior.BehaviorPackAutoLoader.load`
4. `bridge.capability.CapabilityComponentRuntimeHooks.onTextureChanged` → `capability.component.RenderControllerComponent.onTextureStateChanged`
5. `bridge.client.loader.BedrockAddonAutoLoader.bridgeAndPublish` → `client.loader.BedrockAddonRuntimeBridge.replaceFromAddon`
6. `bridge.client.loader.ClientLoaderLifecycleHooks.registerAnnotatedListeners` references `client.loader.ResourceLoader`
7. `bridge.client.render.bake.ModelBakeInfo.bake` 返回 `client.render.bake.BakedModel`
8. `bridge.client.render.bake.ModelBakeInfo.getBakedModel` 返回 `client.render.bake.BakedModel`

**模式**：BakedModel 应移到 domain 或 bridge（被 bridge 和 client 共用）；*Hooks 应调 Port 不直接调 application。

## 规则 8 违规清单（82 条，aclPublicApiMustBeInterfaceOrAnnotation）

bridge 直接子级（非 adapter/）的 public 顶层类不是接口。完整 82 类清单见 `build/archunit_store/1.20.1/f94b8188-*`。

**分类**：
- Mod 入口/环境：Eyelib, ForgeEnvironment
- 事件 Handler：CommonEntityEventHandler, *RuntimeHooks, *LifecycleHooks, *Handler（~15）
- Packet 类：*SyncPacket, *UpdatePacket, *DataPacket, Spawn/RemoveParticlePacket（~10）
- 事件类：*Event, *EventPublisher（~6）
- 工具/适配器：ItemKeyResolver, EntityPortAdapter, ShaderManager, CodecOps, NativeImageIO, TextureLayerMerger, RenderTypeResolver, ResourceLocationBridge, ComponentStore, BrRenderTypeFactory, RenderPassAdapter（~12）
- 容器/桥接：EntityRenderPorts, DataAttachmentContainer*, McDataAttachmentContainer（~5）
- UI 适配：MCButton, MCGraphics, MCPoseStack, MCScreenAdapter, MCTextField, MCWidgetAdapter, ScreenPort（~7）
- Loader/Runtime：BedrockAddonAutoLoader, ClientLoaderLifecycleHooks, *RuntimeBridge（~8）
- molang 映射/query：ForgeMolangMappingDiscovery, *MolangQuery*, MolangEntityContext, ComponentStore, MolangBuiltInQuery（~8）

**修复方向**：规则定义 `BRIDGE_PUBLIC_TOP_LEVEL_CLASSES` 排除 `bridge..adapter..`。两个选项：
- A）把具体实现类移到 `bridge/<module>/adapter/` 子包
- B）为每个具体类抽 Port 接口留在 bridge 直接子级，实现类进 adapter

## I-2 违规清单（6 条，domainMustNotUseSingletonInstance）

domain 层的 `public static final INSTANCE` 字段：
1. `EmptyComponent.INSTANCE` — null object 模式
2. `InsideBlockNotifier.INSTANCE`
3. `MolangNull.INSTANCE` — null object 模式
4. `SuspectTracking.INSTANCE`
5. `VibrationDamper.INSTANCE`
6. `VibrationListener.INSTANCE`

**修复方向**：null object（EmptyComponent/MolangNull）改用工厂方法或 Optional；服务定位器型改依赖注入。

## I-5 违规清单（214 条，applicationMustNotDependOnBridgeConcreteClasses）

按被调用的 bridge 具体类分组（完整 214 条见 `build/archunit_store/1.20.1/be980ffb-*`）：

| 分组 | bridge 类 | 违规数 | application 调用方 | 修复方向 |
|---|---|---|---|---|
| network | EyelibNetworkTransport + 10 *Packet + DataAttachmentSyncRuntime | ~60 | EyelibNetworkManager, NetClientHandlers, ClientRenderSyncService | 抽 Transport Port + Packet 移 domain/抽接口 |
| dataattach | DataAttachmentHelper, DataAttachmentTypeRegistry, EyelibAttachableData | ~30 | RenderData, ClientBootstrap, MolangQuery, EntityRenderOrchestrator, AttachableDataTypes | 抽 DataAttachment Port |
| render | RenderPorts.get()/renderSystemPort/install + 各 Port 字段 | ~20 | EntityRenderOrchestrator, RenderHelper, AttachableItemRenderSetup | 参数注入 RenderSystemPort |
| texture | NativeImageIO, TextureLayerMerger | ~15 | RenderControllerEntry, RenderParams, ManagerResourceImportPlanner | 抽 TextureIO Port |
| material | ResourceLocationBridge, RenderTypeResolver, RenderPassAdapter | ~15 | ModelComponent, RenderParams, RenderControllerEntry, ModelPreviewScreen | 抽 Material Port |
| codec | CodecOps | ~10 | ManagerResourceImportPlanner | 抽 Codec Port 或移 domain/util |
| tick/particle | ClientTickHandler, ParticleRuntimeBridge.SPAWN_ADAPTER | ~8 | AttachableItemRenderSetup, EntityRenderOrchestrator, MinecraftParticleRuntimeEnvironment, DragTargetWidget, EntitiesListPanel, NetClientHandlers | 抽 Tick/Particle Port |
| molang | ComponentStore, MolangEntityContext, EntityPortAdapter | ~6 | EntityRenderOrchestrator, AttachableItemRenderSetup | 抽 MolangContext Port |
| event | ManagerEntryChangedEvent/Publisher, TextureChangedEventPublisher | ~8 | RenderHelper, ModelBakeInvalidationHooks, ManagerResourceImportPlanner | 抽 EventPublisher Port |
| bake | ModelBakeInfo（继承） | 3 | EmissiveModelBakeInfo, TwoSideModelBakeInfo | ModelBakeInfo 抽接口 |
| gui | ModalWorksurfaceScreen（继承） | 6 | ModelPreviewScreen | ModalWorksurfaceScreen 抽接口或移 application |
| entity | EntityRegistryBridge | 4 | EntitiesListPanel | 抽 EntityRegistry Port |
| screen | ScreenPort, ModelPreviewScreenHook, AnimationViewHook | 4 | ClientBootstrap | Hook 类用 Port |
| misc | RenderCallRecorder, ARCompat, ForgeEnvironment, RenderEntityParams | ~4 | ManagerFolderSession, ActiveModelRenderVisitors, AIDebugServer, EntityRenderOrchestrator | 各自抽 Port |

## Stonecutter 违规清单（48 文件，//? 在非 ACL 包）

完整清单见 `build/archunit_store/stonecutter-comment-baseline.txt`。

**分布**：client/(28文件) > common/(2) > animation/behavior/capability/importer/model/particle/track/util/(各1-3)

**修复方向**：把含 `//?` 的代码段迁到 bridge/ 或 mixin/，或抽 Port 让版本差异收敛到 ACL。

## iqf 源码扫描违规（2 条）

- `iqf-static-initializer`：`client/render/visitor/ActiveModelRenderVisitors.java:13` — `static {` 块
- `iqf-lazy-init-scatter`：`capability/RenderData.java:90` — `if (getOwner() != owner)`

## 预存问题

### 1.21.1 ModelPartModelInterfaceTest ClassCastException

- 根因：`ModelPartModel.childrenOf` 1.21+ 走 `((ModelPartAccessor)(Object)modelPart).eyelib$getChildren()`，但单元测试环境 Mixin 未装配
- 引入：3163e5d7（Bone 接口化 + ModelPartModel implements Model，已 commit）
- 与本次 4 文件改动无关

### 26.1.2 node 平台配置失败

- 880 个 MC/NeoForge 平台类找不到
- stash 验证为预存
- `versions/26.1.2/` 有 hs_err_pid92052.log（JVM crash）
- build.gradle:186-247 的 NeoForge 分支用 `project.neoforge_version`，但 gradle.properties 无此属性

## 修复策略与子任务分割

### 子任务 0：预存问题修复
- 0a：26.1.2 node 平台配置（查 neoforge_version 来源 + hs_err）
- 0b：1.21.1 ModelPartModelInterfaceTest（Mixin 测试装配）

### 子任务 1：I-2（6 条，最小）
- null object（EmptyComponent/MolangNull）vs 服务定位器分类处理

### 子任务 2：规则 4（8 条）
- BakedModel 归属（移 domain 或 bridge）
- *Hooks 反向依赖消除

### 子任务 3：Stonecutter（48 文件）
- //? 代码段迁移到 bridge/mixin

### 子任务 4：规则 8（82 条）
- bridge 具体类移 adapter/ 子包或抽 Port 接口

### 子任务 5：I-5（214 条，最大）
- 按 bridge 类分组逐批抽 Port + 参数注入
- 先处理高频分组（network ~60 / dataattach ~30 / render ~20）

### 子任务 6：iqf 源码扫描（2 条）

### 子任务 7：三版本全闸门复跑

## baseline 持久化问题

baseline 在 `build/`（gitignored），clean build 会丢失。FreezingArchRule 首次跑会重新 freeze 全部违规。**清零后需确保 baseline 为空文件（0 违规），这样即使重建也不会引入旧违规。** 源码扫描 baseline（iqf-*/stonecutter-）同理——清零后文件应为空。
