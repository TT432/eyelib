# ADR-0045：渲染 sink 批次作用域与阶段订阅优先级契约

> **状态**：已实施（2026-11-17）
> **关联**：ADR-0042（实体 pass 状态单一权威）、ADR-0038（姿态管道早于 shadow pass）、ADR-0032（薄映射层 / GPU 蒙皮）、ADR-0039（Iris 下 26.1 禁蒙皮）
> **触发**：`docs/gap-analysis/downstream-consumer-gaps.md` G4 / G9

---

## 1. 问题

**G4**：`EntityRenderOrchestrator` 在每个组件提交后立即 `sink.flush()`，`passOrder` 是包私有静态映射；`RenderSink` 没有批次概念。下游要跨实例合批只能自建 `RenderType` 分桶、复制 pass 档位、用 `skinning=null` 关 GPU 蒙皮（黑魔法），并且该实现到 26.1 必须重写。

**G9**：`@OnRenderStage` 没有优先级，订阅者顺序由发现顺序决定；下游想「在 eyelib TickStage 采样 `query.head_yaw` 之前改写 `yBodyRot`」只能退回 NeoForge 事件 + `EventPriority.HIGHEST`（跨事件源抢序，脆弱）。

## 2. 决策

### 2.1 批次是 sink 实例状态（G4）

```java
interface RenderSink {
    BatchScope openBatch();                       // = openBatch(BatchPolicy.DEFER)
    BatchScope openBatch(BatchPolicy policy);     // default: flush(); return BatchScope.NOOP
    FlushMode flushMode();                        // default: PER_SUBMIT
    SkinningSupport skinningSupport(pass, texture);
    int submitCount();
}
record BatchPolicy(boolean mergeDraws, boolean allowGpuSkinning) { DEFER / MERGE / MERGE_KEEP_SKINNING }
interface BatchScope extends AutoCloseable { close(); stats(); isOpen(); }
enum SkinningSupport { AVAILABLE, OFF_BY_FLAG, NO_ENGINE_PATH, SHADER_PACK_ACTIVE,
                       OUTSIDE_STAGE_WINDOW, PASS_NOT_SKINNABLE, DISABLED_BY_BATCH }
```

- 新增方法全部是 `default`：外部既有 `RenderSink` 实现（含下游旧 `BatchedRenderSink`）不改也能编译；默认 `NOOP` 作用域 = 立即刷（保守正确）。
- 作用域内 `flush()` 不绘制（`PER_BATCH`），作用域外行为与今天完全一致——编排层那句 `sink.flush()` 一个字都不用改。
- **跨类型合批必须在 sink 侧做**：1.21.1 的 `MultiBufferSource.BufferSource` 只有单个 `lastSharedType`，换类型即内部 `endBatch`，编排侧「延迟 flush」无法让同类型跨实例累积。`mergeDraws=true` 时 sink 自建 `RenderType → Bucket`，`close()` 按 `PassTier` 分组、组内按首次提交序各画一次。
- 关闭顺序：`OPAQUE` 桶 → OPAQUE 蒙皮会话 → `BLEND` 桶 → BLEND 蒙皮会话 → **最后** `bufferSource.endBatch()`（vanilla 通道装的是挂载物/手持物/栓绳直写几何；栓绳契约要求在本体几何之后）。启用 `MERGE` 会改变这两条通道的相对次序，属调用方显式选择的行为变化，需要实机确认。
- `allowGpuSkinning=false` 把 `skinning=null` 传给 writer——下游的「合批必须关蒙皮」黑魔法变成策略参数。
- **26.1 的能力边界（硬结论）**：无法改变 flush 时机。`submitCustomGeometry` 只登记节点，绘制归 `renderAllFeatures`/`LevelRenderer`；批次在 26.1 只是「语义占位 + 策略生效 + 统计」（`stats().draws == 0`）。不得在文档或注释里声称 26.1 会因此改变绘制时机。
- **1.20.1 的边界**：`<1.20.6` 无 `ByteBufferBuilder/MeshData`，分桶关闭路径必须镜像该版本的 `BufferSource#endBatch`（`BufferBuilder.endOrDiscardIfEmpty()` + 手动 `setupRenderState/drawWithShader/clearRenderState`）。无法取得可核对签名时只交付 `DEFER`，并显式声明限制——不把猜测写成实现。

### 2.2 pass 档位公开为 `PassTier`（G4）

`material/port/PassTier { OPAQUE(0), BLEND(1) }`；`PortRenderPass.tier()`；`ModelComponent.passTier()`（`texture==null` 或 `getRenderType==null` → OPAQUE，与旧 `passOrder` 逐项等价）。`EntityRenderOrchestrator.passOrder` 删除。

### 2.3 阶段优先级（G9）

```java
@OnRenderStage(priority = RenderStagePriority.BEFORE_TICK)   // int 常量：BEFORE_SETUP/-300, BEFORE_TICK/-100,
                                                            // CORE/0, AFTER_TICK/100, AFTER_PIPELINE/200
```

- 载体必须是 `int`：`util/event/api` 属 domain，`ArchitectureTest` 禁止引用 `net.neoforged.*`；且 ASM `annotationData()` 对 int 属性就是 `Integer`，无需枚举解码。
- 排序在 domain 注册表完成：`(priority, declaringClass.getName(), methodName)` 三级键 → 跨 mod 装载顺序、跨运行稳定。今天的唯一订阅者（`EntityRenderOrchestrator#onRenderStage`）显式标注 `CORE`，实际顺序不变；「同优先级顺序从发现顺序变为类名/方法名」是唯一的有意变化。
- 不做 pre/post 成对注解：只能表达「相对固定点」，表达不了「相对其它订阅者」；数值锚点已覆盖需求。
- 时机契约不变：每帧一次、早于 Iris/Oculus shadow pass（ADR-0038）。

## 3. 后果

- 下游删除 `BatchedRenderSink`（130 行复制的分桶与不变量），改用 `try (var batch = sink.openBatch(BatchPolicy.MERGE)) { … }`；同一段代码在三节点成立（26.1 语义降级但编译与调用面一致）。
- 下游可以用 `skinngSupport(...)` 判断「这帧能不能合批」，而不是猜 `skinning=null`。
- 下游把「抢在 TickStage 之前」从 NeoForge 事件优先级改为 `@OnRenderStage(priority = BEFORE_TICK)`，同一 dispatch 内的顺序成为契约。
- 未覆盖：不做跨 sink 实例/实体 pass 级全局合批（ADR-0042 §5 的「全部 solid 先 flush、blend 后 flush」），不把库内实体路径切到 `MERGE`。

## 4. 验证

- 单测：`PassTierTest`（全部 `Transparency` 成员 + `passTier()` 的 null 归 OPAQUE）、`RenderStageSubscriberRegistryTest` 新增 `priorityOrdersDispatch` / `samePriorityIsStableByClassName`（既有 6 个用例不改）、`BatchPolicy` 值语义。
- 三节点 `mcmcp_build` / `test` 全绿（含 `PassTierTest`、`BatchPolicyTest`、`BatchScopeTest`、`RenderStageSubscriberRegistryTest` 新增两例）。
- **未验证（显式收窄）**：① `openBatch(MERGE)` 的实际 draw 数与像素等价（1.21.1 作物场景、`BatchStats.draws`）**未实机测量**；② 26.1 `stats().draws()==0` 与 `skinningSupport` 的 `OUTSIDE_STAGE_WINDOW` 返回值**未实机打印**；③ 1.20.1 分桶关闭路径按 sources jar 签名实现，**未在 1.20.1 实机跑过合批**；④ `MERGE` 改变 vanilla 通道与合并几何相对次序后的半透明叠序未实机确认。
