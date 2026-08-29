# 迭代 1：跨实体合批 compute 蒙皮（≤26.1，GL≥4.6）

## 目标

消除已实证的性能瓶颈：逐实体 draw + 状态机 + 12KB 调色板上传。
手段：同 RenderType 实体会话跨实体合并 → 每组 1 次状态机 setup + ≤#几何 次 dispatch + 1 次 draw。

## 关键事实（源码实证）

- `ImmediateRenderSink.flush()` 每实体每组件调用：endBatch() 后逐会话 `LegacySkinningManager.draw`。
- RenderSystem MV 在实体循环期间 = 相机矩阵，**实体变换在 palette 根姿态里**（pose.last() 传入 writer）→ 合批无需逐实体 MV 折叠，draw 时取 MV 与循环内同值。
- per-entity 差异仅剩：palette（slotCount×2 mat4）、tint、overlayUV、lightUV → 折叠为 meta SSBO + 顶点属性。
- 骨架 hook：RenderLevelStageEvent AFTER_SKY/AFTER_ENTITIES/AFTER_LEVEL（Forge 1.20.1 javap 实证 11 个 Stage；NeoForge 1.21.1 同名）。
- FboBenchmarkWorkload 在 AFTER_LEVEL 手动渲染 → 显式窗口 API 包住循环。
- 窗口外（GUI/预览/nether 无 sky 假设）→ flush 时立即 drain = 旧行为，天然回退。

## 数据流

```
submit(entity) → session(palette arrays + tint/light/overlay)
flush() → endBatch() → Dispatcher.submit(session)
  窗口开 → 入队；窗口关 → 立即 drain（=旧语义）
AFTER_ENTITIES → drain()：
  1. 按 RenderType 分组（LinkedHashMap 保提交序）
  2. 组内按几何(模型)分子批；布局：vertexBase/matBase/metaIdx 顺序分配
  3. 上传 palette SSBO（紧凑打包，仅 slotCount×2 mat4/实体）+ meta SSBO（64B/实体）
  4. 计算相：每子批一次 2D dispatch（x=顶点，y=子批内实体）
  5. 屏障一次
  6. 绘制相：每组 setupRenderState → vanilla uniform 序列 → glDrawArrays(组首顶点, 组顶点数)
AFTER_LEVEL → closeWindow（安全兜底）
openWindow 时发现残留 → 丢弃+告警（异常帧防护）
```

## 布局

- VertOut（80B，std430）：pos@0(16) nrm@16(16) uv@32(8) light@40(8) overlay@48(8) pad@56(8) tint@64(16)
- Meta（64B，std430 stride 64）：tint@0(16) lightOverlay@16(16)=(lu,lv,ou,ov) poseBase@32 normalBase@36 vertexBase@40 pad@44
- Palette：mat4[] 无尺寸数组，逐实体紧凑（pose n 个 + normal n 个）
- VAO 属性（stride 80）：0=Position@0 1=UV0@32 2=Normal@16 3=Tint@64 4=LightUV@40 5=OverlayUV@48
- 属性位置契约：vanilla ShaderInstance 按 VertexFormat 元素顺序 glBindAttribLocation（已实证）

## 资产（新 8 文件/版本族；旧 compute 资产 16 文件+skin.comp 删除——干净切换）

- skin_batch.comp：2D dispatch，meta 驱动
- entity_skinned_batch{,_emissive}.vsh（1.20.1）/ entity_skinned_batch_121{,_emissive}.vsh（1.21.1）：
  对应 compute vsh 改 3 uniform → 3 属性；texelFetch 处 ivec2(LightUV)/16、ivec2(OverlayUV)
- 4+4 JSON：对应 compute JSON 去 TintColor/OverlayUV/LightUV uniform，attributes 6 项

## Java

- `BatchSkinningProgram`（新）：probe（GL≥4.6+指针）、编译 skin_batch.comp、palette/meta/output SSBO
  （容量倍增增长；palette/meta 每 drain orphan+subData；output 仅容量保障+屏障保序）、全局 VAO（output 重建时重指）、dispatch/barrier
- `BatchSkinningDispatcher`（新）：窗口状态、会话队列、分组布局、drain 编排、stale 防护
- `SkinningBatchStageHook`（新，//? if <26.1）：FORGE/GAME bus 订阅 stage 事件
- `ComputeSkinnedGeometry`：删 outputBuffer/vaoId（输出全局化）；新增 BATCH_FORMAT（6 元素）
  - 1.20.1 元素：Tint=(0,FLOAT,COLOR,4)；LightUV=(3,FLOAT,UV,2)；OverlayUV=(4,FLOAT,UV,2)（避开 UV0/1/2 的 usage+index 冲突）
  - 1.21.1：id 8/9/10，Usage.GENERIC
- `LegacySkinningManager`：computeProgram→BatchSkinningProgram；compute 分支 draw→dispatcher.submit；
  computeDraw 删除；COMPUTE_SHADER_NAMES→batch 名；公开 openBatchWindow/closeBatchWindow/drainBatch（benchmark 用）
- `FboBenchmarkWorkload`：实体循环外包 openBatchWindow/drainBatch/closeBatchWindow（drain 在 endScene 前，FBO+MV 有效）
- VS 路径与 OFF 不变（回退链完整）

## 正确性不变量

1. 组内会话顺序 = 提交顺序；组间顺序 = 首见顺序（passOrder 保证所有实体 solid→translucent 一致排序）
2. additive/emissive 顺序无关；alpha translucent 仍在 AFTER_ENTITIES（water 之前）——与 vanilla 半透明实体同相位
3. partVisibility 退化矩阵随 palette 上传，不变
4. tint 保持 raw float（vsh 量化与现路径逐位一致）
5. 重载窗口期着色器 null → 丢该组一帧（现行为一致）

## 验证

- 两版本编译 + 单测
- 1.20.1 运行时截图：牛/蜘蛛(发光红眼)/僵尸/史莱姆×3 重叠（半透明序） vs CPU 路径
- benchmark 1.20.1：slime n96/n384 fbo+world × CPU/VS/batch；mixed n96
- 成功标准：batch ≥ CPU 路径（预期 fbo n384 显著超 65 FPS 基线）

## 风险

| 风险 | 缓解 |
|---|---|
| AFTER_SKY 不触发（nether） | 窗口关 → 立即 drain 回退 |
| 半透明跨实体顺序变化 | 本就无排序保证；截图验证 |
| drain 时 MV 变化 | stage 事件在 renderLevel 内，MV 常量（实体变换在 palette） |
| 输出 SSBO 重建后 VAO 悬空 | 重建时置 dirty，draw 前重指属性 |
