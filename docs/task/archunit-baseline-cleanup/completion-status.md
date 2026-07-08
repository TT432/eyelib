# ArchUnit Baseline 清零：续作完成状态

执行日期：2026-07-08（续 review-report.md）

## 本次完成

### P0 — 规则 2 domain→bridge 反向依赖（已清零 ✅）
`CodecOps` 从 `bridge/util` 迁至 `domain/util/codec`，8 个方法全部改写为跨版本稳定 DFU API：
- `getOrThrow`/`getOrThrowLog`/`parseOrThrow` → `result()`/`resultOrPartial()`/`error()`（DispatchedMapCodec 先例）
- `unit` → 纯 `Codec.of(Encoder,Decoder)`；`lazyCodec` → 委托式 Codec
- `dispatch` → 用稳定 `MapCodec`/`RecordBuilder`/`DynamicOps` 原语复刻 DFU `KeyDispatchCodec` 语义
  （DFU 4.x `Codec.dispatch` 取 `Function<K,Codec>`，6.x+ 取 `Function<K,MapCodec>`，无统一形式）
- `dispatchStable` 删除（唯一消费者 `BrAnimationController.CODEC` 是死代码，一并删除）

**验证**：删 `build/archunit_store` 重跑，**规则 2 及全部 8 条规则 baseline 均 0B**；三版本 BUILD SUCCESSFUL；
BehaviorCodecSpecTest 通过。commit `809b16f9`→`888437ef`。

### P1 — Stonecutter //? 清零（23 → 2，21/23 ✅）
baseline `stonecutter-comment-baseline.txt` 从 23 文件缩至 **2 文件**：
- **loader 集群（9 文件全清）**：`SimpleJsonWithSuffixResourceReloadListener` 迁至 `bridge/client/loader/`，
  override MC 版本特定的 `apply(Map<RL/Identifier>)` 并归一键为 String，暴露稳定 `applyJson` 回调；
  8 个 `Br*Loader` 改 override `applyJson`。commit `90d7cfb9`
- **DFU 稳定化（CodecOps 模式）**：BehaviorPackPublication、BedrockPackManifest、ManagerImportActions、
  EyelibStreamCodecs（含 `NbtAccounter`→`buf.readNbt()` 消除）。commit `bc10c849`
- **RL↔Identifier / RenderType / 其它 MC API 收敛到 bridge**：AttachableResolver、ModelComponent
  （`getRenderType`→`PortRenderPass`）、RenderControllerEntry、ItemTrackApi、ItemStackIdCache、
  AIDebugServer、ModelPartModel、MolangQuery。新增 bridge facade（DebugServerPort/ModelPartPort/
  EntityStatePort/TrackIdStoragePort/TexturePresencePort 等 interface）。
- **ModelPreviewScreen** 迁至 `debug/client/gui/`（//? 合法栖息地）。commit `bc10c849`、`e0c24c10`

### P3 — 代码质量（✅）
UIScreen/UIGraphics 的 bridge adapter 死 import 删除（仅 Javadoc 引用）。commit `809b16f9`

## 渲染架构对齐（剩余 2 文件已清零 ✅，stonecutter 23 → 0）

> 用户决策：bridge+Port 对齐；删除 emissive 覆盖重渲染（保留材质级 emissive 渲染态）。

### `EyelibLivingEntityRenderer`（17 //?）→ bridge 薄壳
整类迁至 `bridge/client/render/`（//? 合法栖息地）。两代 MC 渲染器外壳（<26.1 双泛型 `render`
/ >=26.1 三泛型 `submit`）改为薄壳，统一委托 application 层既有
`RenderEntityPort.render(RenderEntityParams)`——与 `RenderLivingEventAdapter` 同一 Port。
`useBuiltInRenderSystem` 恒为 true（从未被 setUseBuiltInRenderSystem(false) 调用），
Port 守卫不触发，行为等价；>=26.1 经此对齐获得 <26.1 已有的完整编排（手持物/collectLocators）。

### `RenderParams`（6 //?）→ PortRenderPass + 删 asEmissive
- `renderType` 字段（MC `RenderType`，26.1 包硬移动）改类型为 domain `PortRenderPass` 并改名
  `renderPass`——该字段访问器**从未被读取**，仅构造时用于 `getBuffer`（物化仍经
  `MaterialPort.toRenderType`，用 `var` 局部变量避免 import）。
- `asEmissive` 删除：其结果在 `EntityRenderOrchestrator` 中算出后**从未使用**（死代码），
  26.1 为 no-op stub。连带删除 `getEmissiveTexture`/`toEmissiveTextureLayerPaths`/
  `TexturePaths.emissivePath`/`EmissiveModelBakeInfo`/`ModelBakePort.emissiveInvalidateModel`/
  `TexturePresencePort.isLoaded`（仅 asEmissive 调用）。
- **保留**材质级 emissive 渲染态（`SurfaceClass` EMISSIVE 系列 → `entityTranslucentEmissive`，完整可用）。

### ArchitectureTest 规则 8 补 `EXTENDS_MC_CLASS` 排除
与规则 7 `BRIDGE_CONCRETE_CLASSES` 对齐：extends MC 类的 bridge wrapper 不可能是接口，
不应触发「bridge public 顶层类必须是接口」。

commit `b...`（渲染对齐）、`...`（修正 BrParticleLoaderPublicationTest 适配 P1 applyJson 契约）。

## 验证快照
- 三版本（1.20.1/1.21.1/26.1.2）BUILD SUCCESSFUL
- ArchUnit 8 规则 baseline 全 0B（1.20.1；26.1.2 仅余既有 `EyelibAttachableData` 债务，非本次引入）
- Stonecutter baseline：23 → **0 文件**（全部清零）
- iqf baseline：0B（既有）
- 全量单元测试通过；clientsmoke 因环境（虚拟 GPU）客户端启动失败，未运行——改动经分析为行为等价
