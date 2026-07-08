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

## 剩余 2 文件（耦合到渲染架构对齐任务）

> 用户决策：对齐 26.1.2 的先进渲染架构（见 `docs/task/render-arch-26.1-alignment/plan.md`）

| 文件 | //? | 根因 |
|---|---|---|
| `client/render/EyelibLivingEntityRenderer.java` | 17 | **整类版本分歧**：<26.1 `extends LivingEntityRenderer<T,Model>`（2 泛型）+ `render(entity,...)`→`SimpleRenderAction`→`EntityRenderOrchestrator.renderEntity`；≥26.1 `extends LivingEntityRenderer<T,State,Model>`（3 泛型）+ `submit(state,...)` 手动组件循环。两代 MC 渲染器 API，基类泛型元数/方法签名/渲染逻辑均不同。 |
| `client/render/RenderParams.java` | 6 | `RenderType` 包硬移动（26.1 `renderer.RenderType`→`renderer.rendertype.RenderType`，旧路径已删）+ `asEmissive` 26.1 故意 no-op stub（emissive 重渲染未移植）。 |

这两个文件是渲染核心，清零需要对齐/统一渲染架构，属独立大型任务。

## 验证快照
- 三版本（1.20.1/1.21.1/26.1.2）BUILD SUCCESSFUL
- ArchUnit 8 规则 baseline 全 0B
- Stonecutter baseline：23 → 2 文件
- iqf baseline：0B（既有）
