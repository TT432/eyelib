# C1 GPU 蒙皮 ≤26.1（1.20.1 / 1.21.1）结果

> 日期：2026-08-27。前置：26.1.2 P1（c1-gpu-skinning-26.1.2.md）；设计：work/c1-gpu-skinning/DESIGN-P2.md。
> 实现：LegacySkinningManager/LegacySkinningSession/LegacySkinnedGeometry（`bridge/client/render/skinning/adapter/`，`//? if <26.1`）。

## 实现要点（与 26.1.2 的差异）

- palette = `uniform mat4 BonePose[96]/BoneNormal[96]` + TintColor/OverlayUV/LightUV 散装 uniform（vanilla `Uniform` MAT4 数组 → glUniformMatrix4fv）；逐 draw 同步上传，池化 float[1536]×2/会话。
- 骨骼索引顶点属性复用 vanilla **UV1 元素**（SHORT×2，Usage.UV 非 FLOAT → glVertexAttribIPointer），shader `in ivec2 BoneIndex`，取 .x。**零自定义元素注册、零裸 GL**（ADR-0032 兼容）。
- 几何展开三角形（6 顶点/quad，27B stride），VertexBuffer STATIC，序列索引（AutoStorageIndexBuffer 自动 SHORT/INT）。
- partVisibility：不可见骨骼 pose 写退化矩阵（旋转缩放 0、w=1）→ 零面积三角形被光栅化丢弃。
- 绘制：ImmediateRenderSink.flush（endBatch 后）逐会话 `routingType.setupRenderState()` → 蒙皮 uniform → `VertexBuffer.drawWithShader(RenderSystem MV/Proj, shader)` → `clearRenderState()`。材质状态单一事实源仍是 BrRenderTypeFactory；shader 变体在 `custom()` 创建期登记（与 shaderState 的 vanilla shader 选择一一对应），GLINT/未知类型回退 CPU。
- FS 复用 vanilla `rendertype_entity_*` fsh（ShaderInstance 支持命名空间引用）；VS 逐行复刻 vanilla 母版（1.20.1 与 1.21.1 母版不同：1.21.1 `fog_distance(pos, shape)` 2 参、无 IViewRotMat/normal 输出 → 两版资产分开）。
- 能力探测：注册期 `GlStateManager._getInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS)` ≥ 3136（本机 4096），不足则整体禁用回退 CPU。
- 开关：`-Deyelib.gpuSkinning=false`（与 26.1.2 同一开关）。

## 正确性验证

- 单测：1.20.1 全绿（planSlots 重构由既有 SkinningGeometryPackerTest 覆盖）。
- 运行时（1.20.1 + 1.21.1，A&S 1.10 v2）：牛/蜘蛛/僵尸/史莱姆正确渲染，蜘蛛**发光红眼**（EMISSIVE 变体）昼夜正确；蒙皮路径活跃（GEOMETRIES>0、数组池复用无泄漏、OFF run 无 shader 注册日志）。截图：work/c1-gpu-skinning/shots/p2_mc_day.png（1.20.1）、p2_mc_121d.png（1.21.1）、p2_mc_120_ivec2.png（ivec2 修正后回归）。
- 修复的缺陷：①VertexBuffer.upload 前必须 bind VAO（属性指针/索引缓冲记录在 VAO 状态；缺失 → glDrawElements 驱动崩溃，vanilla 同模式 ChunkRenderDispatcher.uploadChunkLayer）②BoneIndex shader 声明须为 ivec2（UV1 SHORT×2 的实际类型；uint 在 NVIDIA 上巧合可用但属类型不匹配）。

## Benchmark（1.20.1，mixed 场景，RTX 4070 Laptop，15s warmup + 30s measure，fresh JVM，ON/OFF 交错）

n96（slime/zombie/skeleton/cow 各 24）：

| run | 场景 | FPS | fi_p50 | fi_p99 | rw_p50 | rw_p99 |
|---|---|---:|---:|---:|---:|---:|
| ON1 | fbo | 126.7 | 7.818 | 10.205 | 7.439 | 9.406 |
| OFF2 | fbo | 128.3 | 7.705 | 9.937 | 7.318 | 9.178 |
| ON3 | fbo | 116.9 | 8.263 | 12.692 | 7.782 | 11.312 |
| OFF4 | fbo | 125.8 | 7.818 | 10.449 | 7.426 | 9.864 |
| ON1 | world | 38.7 | 25.737 | 30.111 | 24.847 | 28.518 |
| OFF2 | world | 38.3 | 25.937 | 31.943 | 25.015 | 30.547 |
| ON3 | world | 37.7 | 26.387 | 31.333 | 25.452 | 29.827 |
| OFF4 | world | 38.3 | 26.091 | 29.273 | 25.170 | 28.217 |

n384（各 96；world 场景 actual 305/309 ≠ configured 384，按有效性规则 world 数据仅作趋势参考）：

| run | 场景 | FPS | fi_p50 | rw_p50 |
|---|---|---:|---:|---:|
| ON | fbo | 62.5 | 15.869 | 15.260 |
| OFF | fbo | 64.0 | 15.496 | 14.891 |
| ON | world | 13.7 | 71.472 | 68.291 |
| OFF | world | 13.7 | 72.911 | 69.993 |

（benchmark 运行于 `in uint BoneIndex` 版本；ivec2 修正只改属性读取声明路径，不影响性能结论。）

## 结论

**1.20.1 上 C1 性能中性**（FBO ON 慢 ~1–3%，world 持平，噪声 ±3–5%）。结构性解释：vanilla 1.20.1 把同 RenderType 几何合批进共享缓冲（全帧每类型一次 draw），蒙皮路径换成逐实体×组件 draw + 状态机 setup/clear + 24KB uniform 上传；省下的顶点 CPU（Opt1-9 后本已不大）被状态切换与上传开销抵消。

**价值定位与后续**：与 26.1.2 结论一致——C1 的直接收益不在当前 OpenGL 帧时间，而在：①GPU 带宽消除（未测）；②ADR-0032 基建（自有 shader/几何通道三版本贯通）；③26.2+ Vulkan compute 路径的落点。若要把 ≤26.1 做到正收益，下一步是 **P2.5 按 RenderType 跨实体状态去重**（阶段化收集 + 每类型一次 setupRenderState，26.1.2 的阶段 flush 同构），把逐实体状态切换从 O(实体数) 降到 O(RenderType 数)。
