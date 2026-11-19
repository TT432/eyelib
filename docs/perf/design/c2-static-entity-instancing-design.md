# C2 静态实体实例化 —— 设计

> 依据：ADR-0048、ADR-0032、C1 结果（`docs/perf/c1-gpu-skinning-legacy.md`）。
> 用户决策（2026-11-17）：三节点全量实现；静态判定「逐帧 + 定义级预热」双口径。

## 1. 触发条件与判定

一次 `RenderSink.submit` 的 writer 内，`HighSpeedRenderModelVisitor.visitPreModel` 同时持有
`model`（bind 骨骼树）、`infos`（本帧动画采样）与 `params.partVisibility`，在此判定：

```
staticPose = params.staticPose() || !infos.hasAnyEntry(model.allBones().keySet())
```

- `params.staticPose()`：定义级——`AnimationComponent.serializableInfo == null`
  或 `animations` 与 `animate` 均为空。命中时免除逐骨骼扫描（预热口径）。
- 逐帧项：模型所有骨骼（含无几何骨骼，子骨骼世界矩阵依赖父链）都没有 entry。

不满足任一附加条件即回退 C1：AR 渲染器（`ARBakedVisitor`）、Iris/Oculus 光影、
`skinning == null`（GPU 路径不可用）、未知 RenderType 变体。

## 2. 几何

`StaticGeometryPacker`（纯 CPU，可单测）把 `(Model bind 树, BakedModel)` 摊平为模型空间几何：

- 逐骨骼用**同一变换链**求 bind 世界矩阵：`rotateY(π)` → `ModelPoseTransforms.applyBone(..., ModelRuntimeData.EMPTY)`
  （位置/法线分别用 `transformAffine` / 法线阵 + `lenSq > 1e-8` 归一化守卫，与
  `RenderModelVisitor.visitVertex` 逐行同构）。
- 顶点按骨骼分段、每 quad 展开为 6 顶点（与 C1 展开同序），记录 `slot → [firstVertex, count]`。
- 顶点布局：Position(3f) / UV0(2f) / Normal(3f) = 32B（无 boneIndex）。
- 可见性：`partVisibility` 的骨骼 id 经 `slotOf` 映射到 slot，合并相邻 slot 为连续顶点区间
  （`SkinningGeometryPacker.coalesceRanges` 的顶点版）。

缓存：`IdentityHashMap<BakedModel, StaticGeometry>`，与蒙皮几何同批在 ModelManager 条目
变更/整体替换时清空。

## 3. 逐实例布局

`StaticInstance`（float 下标；≤26.1 SSBO std430，160B；26.1 UBO std140，160B）：

| ≤26.1 偏移 | 26.1 偏移 | 内容 |
|---:|---:|---|
| 0 | 32 | root pose mat4（列主序，`PoseStack.Pose.pose()`） |
| 16 | 96 | normal mat3（3×vec4，第 4 分量为 0） |
| 32 | 0 | tint vec4（begin 时 clamp 到 [0,1]） |
| 36 | 16 | light UV vec2（`light & 0xFFFF`, `light >>> 16`） |
| 38 | 24 | overlay UV vec2（`overlay & 0xFFFF`, `overlay >>> 16`） |

与 C1 语义对齐：几何为 root-pose 空间（与 palette 的根姿态同空间），绘制仍取
`ModelViewMat`；光照/overlay 的 `texelFetch` 与量化规则照搬 C1 batch 着色器。
tint 为 float（C1 meta 同为 float），避免与 CPU 字节量化产生 ULP 差异。

## 4. 节点实现

### ≤26.1（compute 模式，GL ≥ 4.6）

- `LegacyStaticGeometry`：裸 GL VBO（STATIC）存摊平顶点；无索引（`glDrawArrays`）。
- 实例数据走 **SSBO**（binding 4，`gl_InstanceID` 索引）：drain 内一个全局
  `GL_STREAM_DRAW` VBO，逐批 `glBindBufferRange` 绑定本批区间。不用逐实例顶点属性：
  1.20.1 的 `VertexFormatElement` 只允许非 UV usage 的 index 为 0，13 个属性放不进单个
  `VertexFormat`；且静态通道与 compute 同门控（GL ≥ 4.6），SSBO 无额外门槛。
- 批键：`(geometry, routingType, variant, 可见区间的 slot BitSet)`；组内按首见序。
- 绘制：`glDrawArraysInstanced(GL_TRIANGLES, firstVertex, count, instances)`，
  状态机仍走 `routingType.setupRenderState()/clearRenderState()`，uniform 序列照抄
  `BatchSkinningDispatcher` 的 `setVanillaUniforms`。
- 着色器：`entity_static_batch.vsh` / `entity_static_batch_121.vsh` 及 emissive 变体；
  4 个 JSON 变体各自复用对应 vanilla FS（与 batch 变体一一对应）。

### 26.1.2

- `NgStaticGeometry`：`GpuBuffer`（VERTEX）存摊平几何 + 每 slot 顶点区间（无索引缓冲，
  绘制用 `RenderPass.draw(firstVertex, vertexCount)`）。
- 逐 draw：`InstanceTransform` UBO 块（std140：vec4 tint / ivec2 overlay / ivec2 light /
  mat4 pose / mat4 normal = 160B）写入共享 ring（与 palette 同一 `MappableRingBuffer`，
  每 phase 一次 mapBuffer 批量上传）。
- 管线：`routing.pipeline().toBuilder()` 换 `eyelib:core/entity_static` VS +
  Position/UV0/Normal 顶点格式 + `InstanceTransform` UNIFORM_BUFFER，保留 routing 的
  FS/采样器/状态。
- 绘制：`pass.setUniform("InstanceTransform", slice)` + `pass.draw(...)` 逐实例区间；
  分组键 `(pipeline, texture)` 与 C1 记录共用同一 `execute`（静态与蒙皮 pipeline 不同，天然分组）。
- 静态 vs 半透明队列：沿用 `hasBlending()` 分组。

## 5. 顺序与回退

- 静态批与 C1 会话在同一窗口 drain；组内按「首见序 + 提交序」排列，避免改变半透明相位。
- 回退：`beginStatic` 返回 false、几何创建失败（全零顶点）、可见性为全不可见时按 C1 语义处理
  （全不可见 → 无 draw）。
- 开关：沿用 `-Deyelib.gpuSkinning=false`（整体关闭 GPU 路径，含静态通道）。

## 6. 验证

1. 单测 `StaticGeometryPackerTest`：摊平顶点与手算 bind 链结果一致；分段范围；可见性合并；
   空几何返回 null —— 4 项通过。
2. 单测 `ModelRuntimeDataEntryTest`：`hasAnyEntry` 边界（空集/已写入/负 id/越界）—— 3 项通过。
3. 编译三节点绿；NullAway 三节点无违规；1.20.1 全量 test 绿；1.21.1/26.1.2 `test` 绿。
4. clientsmoke：1.20.1 12/12、1.21.1 9/9、26.1.2 9/9（1.21.1/26.1.2 的 smoke run 目录需放 A&S 包）。
5. 实机 A/B：
   - 1.20.1 同会话：非动画 client entity（绑定去掉 animations/animation_controllers 的副本）→
     静态通道（`STATIC_GEOMETRIES=6`）；反射切 `Mode.VERTEX_SHADER`（VS 着色器未注册 ⇒
     `createSession` 返回 null）得到 CPU 参照，模型区域像素差 0（max 1）。恢复动画绑定后
     C1 compute 路径正常。
   - 1.21.1 同会话：静态通道生效（`STATIC_GEOMETRIES=6`）；与 CPU 参照逐像素对比，牛模型区域
     零差异（差异像素全部落在背景中移动的史莱姆上）。
   - 26.1.2：静态通道生效并正确渲染；`-Deyelib.gpuSkinning=false` 重启的 CPU 参照因跨会话
     相机/时间漂移无法逐像素对齐，几何/位姿目检一致。
6. 验证期修复的 26.1 缺陷（已入 ADR-0048）：顶点格式 23B 非 4 对齐；`RenderPass.draw` 前未
   `setVertexBuffer`。

## 7. 未验证/后续

- 性能基准（非动画场景 ON/OFF）未做：本次以功能正确性与上传量结构为交付目标，基准沿用
  `docs/perf/render-fps-benchmark.md` 纪律另行执行。
- 多可见性模式（逐实体 RC part_visibility 差异）只由打包器单测覆盖，未做逐像素 A/B。
