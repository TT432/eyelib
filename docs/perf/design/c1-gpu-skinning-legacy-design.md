# C1 GPU 蒙皮 P2 —— ≤26.1（1.20.1 Forge / 1.21.1 NeoForge）设计

> 依据：DESIGN.md（26.1.2 先行版）、ADR-0032、1.20.1 vanilla sources jar 一手 API 事实（2026-08-27，行号级引用见各节）。
> 目标：与 26.1.2 相同的「静态几何 + 骨骼调色板 + VS 蒙皮」架构，在 GL 3.2 core / vanilla 状态机 RenderType 约束下落地，完整保留功能（不支持的路径回退经典 CPU）。

## 1. 与 26.1.2 的架构差异（约束 → 决策）

| 约束（一手证据） | 决策 |
|---|---|
| 无自定义 RenderPipeline；RenderType = 状态机 + ShaderInstance | 不替换 RenderType：draw 时 `routingType.setupRenderState()`（纹理/混合/深度/cull/lightmap/overlay shard 全部生效，Sampler0/1/2 由 shard 绑定），只换着色器为蒙皮变体，`routingType.clearRenderState()` 收尾。材质状态单一事实源仍是 BrRenderTypeFactory |
| palette 无 UBO 抽象；vanilla `Uniform` 支持 MAT4 数组（JSON `"type":"matrix4x4","count":N`，N=浮点数个数；`uploadAsMatrix` → glUniformMatrix4 全量数组） | palette = 两个 `uniform mat4 BonePose[96]/BoneNormal[96]` + `TintColor/OverlayUV/LightUV` 散装 uniform。单次 draw 同步上传（池化 float[1536]×2/会话，flush 后归还），无 ring/staging 生命周期问题 |
| `ShaderInstance.apply()` 会应用 JSON 声明的 `blend`（BlendMode.apply 在 transparency shard 之后执行，可能覆盖自定义混合因子） | 与 CPU 路径保持**逐位一致**（vanilla shader 同样有 JSON blend 覆盖行为，CPU 路径本就如此）；4 个变体 JSON 的 blend 块照抄对应 vanilla json |
| uniform 组件预算：GL 3.2 spec 下限 1024 float，96 骨骼需 ~3132 | 注册 shader 时用 `GlStateManager._getInteger(0x8B4A)`（blaze3d 自带包装，非裸 GL）做一次性能力探测，不足则整体禁用蒙皮（大声日志，回退 CPU）。现实桌面 GPU ≥4096 |
| `VertexBuffer.drawWithShader(MV, Proj, shader)` 自动设置 ModelViewMat/ProjMat/Fog/ColorModulator/Light0/1/TextureMat + 从 RenderSystem.getShaderTexture(0..11) 绑定 Sampler | 绘制 API 直接用 `drawWithShader`；只需在 setupRenderState 之后补设蒙皮 uniform 再调用 |
| 显式索引缓冲无 public API（storeRenderedBuffer 非排序时 sequentialIndex=true → 用 RenderSystem 序列索引）；1.21.1 BufferBuilder 全新 API（MeshData）更无此路径 | 几何烘焙为**展开三角形**（每 quad 6 顶点，无索引缓冲，序列索引 0..N-1）。AutoStorageIndexBuffer.bind 按 count 自动 SHORT/INT 升级，无 65535 上限。静态内存 1.5×，一次性成本 |
| `VertexBuffer.draw()` 只支持整段绘制（无 range draw API） | partVisibility 用**零缩放矩阵退化**实现：不可见骨骼 slot 的 pose 写为 m00..m22=0/m33=1 的矩阵 → 该骨骼全部顶点塌缩到同一点 → 零面积三角形，光栅化丢弃（无片段、无深度写入），语义等价于不绘制。同时省掉 range 合并 |
| 骨骼索引顶点属性载体：自定义元素注册签名在 1.20.1/1.21.1 分裂，且 1.21.1 VertexConsumer 无 generic UV3 setter | **复用 vanilla `UV1` 元素**（SHORT×2，Usage.UV 非 FLOAT → glVertexAttribIPointer，shader `in ivec2` 取 .x）：顶点写入全程 vanilla VertexConsumer API（1.20.1 vertex/uv/normal/overlayCoords，1.21.1 addVertex/setUv/setNormal/setUv1），法线量化与经典路径天然逐位一致，零自定义元素注册。27B stride |
| overlay/lightmap 纹理绑定由 OverlayStateShard/LightmapStateShard 在 setupRenderState 完成（OverlayTexture.setupOverlayColor / lightTexture.turnOnLightLayer） | 不自带绑定逻辑；flush 时序 = 实体渲染调用栈内（ImmediateRenderSink.flush，等价于 vanilla endBatch 时机），RenderSystem MV/Proj 与 vanilla 一致 |

## 2. 数据流

```
加载期（一次/BakedModel）：BakedModel → SkinningGeometryPacker.planSlots（骨骼按 id 升序分段，与 NG 共用唯一事实源）
    → LegacySkinnedGeometry（vanilla VertexConsumer 写入，展开 6 顶点/quad，VertexBuffer STATIC，27B 顶点）
资源重载：RegisterShadersEvent（MOD bus）注册 4 个蒙皮 ShaderInstance 变体 + 能力探测
模型失效：ManagerEntryChanged/ManagerReplaced → 几何缓存清空（复用 NG 同款钩子）
RenderType 创建期（BrRenderTypeFactory.custom）：LegacySkinningManager.registerVariant(renderType, variant)
每实体×组件（ImmediateRenderSink.submit）：
    session = LegacySkinningManager.createSession(routingType)
    （关闭/未知变体/GLINT → null → 经典路径）
    writer.write(..., session) → visitor 采集 appendBone/markVisible（共享代码不变）
    session.finish() → 不可见 slot 退化矩阵；入 ImmediateRenderSink 待画列表
ImmediateRenderSink.flush()：endBatch() 后逐 session：
    routingType.setupRenderState()
    shader.BonePose/BoneNormal/TintColor/OverlayUV/LightUV 设置
    vertexBuffer.drawWithShader(RenderSystem MV, RenderSystem Proj, shader)
    routingType.clearRenderState()
```

## 3. 着色器变体（GLSL 150，FS 复用 vanilla 同名 fsh——ShaderInstance 支持命名空间引用）

| 变体 | VS | FS（vanilla 复用） | 覆盖 surfaceClass |
|---|---|---|---|
| `eyelib:entity_skinned` | entity_skinned.vsh（fog_distance+lightmap+overlay，对齐 rendertype_entity_cutout.vsh） | minecraft:rendertype_entity_cutout | CUTOUT |
| `eyelib:entity_skinned_solid` | 同上 | minecraft:rendertype_entity_solid（无 discard） | SOLID（默认分支） |
| `eyelib:entity_skinned_translucent` | 同上 | minecraft:rendertype_entity_translucent | TRANSLUCENT/ADDITIVE 非发光 |
| `eyelib:entity_skinned_emissive` | entity_skinned_emissive.vsh（无 fog.glsl/lightmap，对齐 translucent_emissive.vsh） | minecraft:rendertype_entity_translucent_emissive | EMISSIVE_CUTOUT/TRANSLUCENT_EMISSIVE/发光 BLEND |
| GLINT | —— 不支持，回退 CPU | | |

VS 与 CPU 路径同构点：tint 字节量化（floor(clamp×255)/255）、法线 mat3 变换 + lenSq>1e-8 守卫归一化、
overlay/lightmap = 逐实体 uniform ivec2（overlay & 0xFFFF, overlay>>>16；light 同，/16 在 shader 内与 vanilla 一致）。
蒙皮数学 `BonePose[i] * vec4(Position,1)`、`mat3(BoneNormal[i]) * Normal` 与 BakedBone.transformPos/transformNormal 逐行同构。

## 4. 功能保留映射

- partVisibility：零缩放退化矩阵（见 §1）。
- 材质分区/passOrder：routing RenderType 不变；flush 逐组件在 endBatch 后立即执行，透明组件经 passOrder 排序后最后 flush，顺序语义与现状一致。
- overlay/lightmap/tint：uniform 传入（§3）。
- locator/attachable/extraRender：CPU 矩阵唯一权威，不动。
- AR（仅 <26.1）：ARBakedVisitor 需 bind-pose 数组——AR 宿主实体不创建 session（回退点见实现），与 26.1.2 的 AR 处理一致。
- 半透明排序：1.20.1 vanilla 自定义透明类型 sortOnUpload=true 仅作用于 BufferBuilder 共享缓冲；蒙皮路径顶点不进共享缓冲，与 26.1.2 一致（同版本对齐，非新增差异）。
- ModelPreviewScreen/NodeAssetPreview/AttachableItemRenderSetup：不经 RenderSink 蒙皮路径，不受影响。

## 5. 回退与开关

- `-Deyelib.gpuSkinning=false` 全局关闭（与 26.1.2 同一开关）。
- 回退条件（全部回经典 CPU 路径，零功能损失）：能力探测失败；骨骼数 >96；未知/未注册变体（含 GLINT 与非本厂 RenderType）；AR 宿主；shader 尚未注册（重载窗口期）。

## 6. 验证计划（已全部执行，结果见 docs/perf/c1-gpu-skinning-legacy.md）

1. 单测：LEGACY 打包布局（24B stride、展开顶点、骨骼分段）——扩展 SkinningGeometryPackerTest。
2. 运行时：mcmcp 启动 1.20.1，A&S 牛/僵尸/骷髅/史莱姆动画视觉回归（截图），蒙皮活跃探针（几何缓存 >0、变体计数）。
3. benchmark：1.20.1 world-mixed ON/OFF A-B-A（沿用 docs/perf/render-fps-benchmark.md 纪律）。
4. 1.21.1 同构验证（差异仅事件总线/包名，代码 //? 分支共享）。
