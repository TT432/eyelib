# C1 GPU 蒙皮（26.1.2）设计

> 依据：docs/research/2026-08-26-render-gpu-offload.md §5-C1、ADR-0032、
> 26.1.2 sources jar 一手 API 事实（2026-08-27 librarian 调研，行号见各节引用）。
> 基线：../c1-baseline-26.1.2.md（world-mixed-n96 render_work P50 12.478ms）。

## 1. 数据流

```
加载期（一次/BakedModel）：BakedModel → SkinnedGeometry（静态 GpuBuffer 顶点+索引，
    顶点格式 Position3f/UV0 2f/Normal 3b+pad/BoneIndex uint，28B  stride；
    骨骼按 id 排序连续分段，记录 per-bone index range 与 palette slot）
每帧 RenderFrameEvent.Pre：palette MappableRingBuffer rotate + 绘制记录清空
每实体×组件（DeferredRenderSink.submit → submitCustomGeometry 节点）：
    writer 回调（renderSolid/TranslucentFeatures 阶段，= 正确 phase/时机）
    → DFSModel.visit（applyBoneTranslate 不变，CPU 合成骨骼矩阵——locator/attachable 依赖）
    → HighSpeedRenderModelVisitor：session 非空 → appendBone(boneId, pose, normal)
      + 可见骨骼 markVisible；不写任何顶点
    → session.finish()：std140 打包（header 32B + mat4 pose + mat4 normal per bone）
      写 palette ring buffer 得 GpuBufferSlice；
      · override（RenderSystem.outputColorTextureOverride）非空（PIP/物品栏/FBO 场景）
        → 立即自建 RenderPass 画到 override 视图
      · override 为空（LevelRenderer 主 pass）→ 追加到当帧绘制记录
阶段末 flush：RenderLevelStageEvent.AfterOpaqueFeatures（在 copyDepthFrom 之前，
    LevelRenderer.java L709 vs L713-721）/ AfterTranslucentFeatures（L729，半透明地形前）
    → 每 (pipeline) 一个 RenderPass：setPipeline + bindDefaultUniforms
      + setUniform("DynamicTransforms", writeTransform(stageModelView, 1,1,1,1, 0, identity))
      + 按纹理组 bindTexture(Sampler0/1/2) + 逐实体 setUniform("BonePalette", slice)
      + 按合并后的可见 range drawIndexed
```

## 2. 关键决策与理由

| 决策 | 理由（证据） |
|---|---|
| 调色板走 UBO，不走 TBO | 26.1.2 TEXEL_BUFFER 支持存在但 TextureFormat 枚举无 float 格式（GlConst.toGlInternalId 仅 RGBA8/RED8/RED8I/depth；Uniform.java L25-31）。UBO 是官方 DynamicUniforms 范式 |
| 调色板 block 尺寸 = align256(32 + 128×模型骨骼数)，MAX_BONES=96 编译期 define | GL 3.3 保证 MAX_UNIFORM_BLOCK_SIZE ≥16384B；96 骨骼=12320B 安全。超出 → 该实体回退经典 CPU 路径 |
| 保持 submitCustomGeometry 作为 phase 路由 | writer 回调时机 = renderSolid/TranslucentFeatures 内（CustomFeatureRenderer L22-43），phase 正确性零成本；routing RenderType 管线不变（写零顶点，BufferBuilder build()=null 被跳过） |
| 世界路径批量 flush 在 stage event，不在 writer 内立即画 | AfterOpaqueFeatures 在 copyDepthFrom 之前 → 不透明实体进主深度，半透明遮挡关系不变（LevelRenderer L706-729 顺序一手验证）；且 ~150 次 pass 创建降为每 phase 一次 |
| PIP/物品栏（GuiEntityRenderer renderAllFeatures + output override）与未来修复的 FBO 场景 → 立即模式 | override 非空即非世界路径，直接画到 override 视图，语义与 vanilla PIP 内实体一致 |
| 半透明（sortOnUpload）材质 P1 不蒙皮，保持经典 CPU 路径 | 现状 sortQuads 是全 RenderType 跨实体全局排序；静态几何无法逐帧重排顶点。后续 P1.5：静态顶点 + 逐帧 CPU 排序仅重写索引缓冲 |
| VS 复刻 entity.vsh，FS 直接复用 vanilla core/entity | 光照/雾/overlay/lightmap 语义逐行对齐（entity.vsh 73 行全文已核对）；浮点差异点 = CPU double 中间值 vs GPU fp32（RenderDoc GetPostVSData 验证） |
| 法线阵原样上传 Pose.normal（非 inverse-transpose） | 与 CPU 路径 transformNormal 逐位同构（含 vanilla 非均匀缩放的已知 quirk） |
| 管线派生：`routingPipeline.toBuilder()` 换 VS+顶点格式+加 BonePalette uniform | RenderPipeline.toBuilder()（L162）保持 BrRenderTypeFactory 单一事实源；派生管线懒编译（GlDevice.getOrCompilePipeline），不注册 RegisterRenderPipelinesEvent（每状态一个管线，注册事件启动期无法穷举） |
| VertexFormatElement.register(findNextId(), 0, UINT, false, 1) 注册 BoneIndex | 非归一化非 FLOAT → glVertexAttribIPointer（VertexArrayCache L60-67），GLSL `in uint`；GlProgram 按 format 名字顺序 glBindAttribLocation（GlProgram L40-45） |

## 3. 功能保留映射（调研 §4 → 实现）

- partVisibility：markVisible → draw range 合并（相邻可见骨骼段合并为一次 drawIndexed）；palette 含全部有几何骨骼（单次 UBO slice 连续）。
- 材质分区/passOrder/透明排序：routing RenderType 不变；writer 顺序 = 上游 passOrder 排序后的提交顺序；flush 保持 writer 顺序。
- overlay/lightmap：移入 UBO header（ivec2 OverlayUV/LightUV），VS texelFetch/sample_lightmap 语义不变。
- tint：UBO header vec4（clamp [0,1] 逻辑与 HighSpeedRenderModelVisitor.visitVertex 相同）。
- locator/attachable/extraRender：CPU 矩阵唯一权威，路径不动。
- AR 兼容：AR 仅 <26.1，26.1.2 无交互。
- ModelPreviewScreen/NodeAssetPreview/AttachableItemRenderSetup：不经 RenderSink.submit 蒙皮路径（直接 consumer/独立 Tesselator），不受影响。

## 4. 回退与开关

- `-Deyelib.gpuSkinning=false` 全局关闭（默认开，仅 26.1.2 生效）。
- 骨骼数 >96 或 palette ring 当帧耗尽：该实体回退经典路径（routing RenderType 缓冲 = ENTITY 格式，经典写入合法）。
- ring 默认 8MB×3；耗尽判定在 session.begin（visitPreModel 前），不在中途。

## 5. 验证计划

1. 单测：std140 打包布局（偏移/对齐）、可见 range 合并、骨槽映射。
2. 客户端：mcmcp 启动 26.1.2，A&S 实体动画视觉回归（截图对比基线）。
3. benchmark：world-mixed-n96 同机 A-B-A 对比（paired ratio，fresh JVM）。
4. RenderDoc GetPostVSData 顶点数值对比（容差定标）——条件允许时。
